package com.budgetpilot.service.month;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.FullDataStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MonthRolloverService {
    public static final String LAST_SEEN_MONTH_KEY = "last_seen_month";

    private final BudgetStore store;
    private YearMonth inMemoryLastSeenMonth;

    public MonthRolloverService(BudgetStore store) {
        this.store = ValidationUtils.requireNonNull(store, "store");
    }

    public boolean shouldPromptForMonth(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        UserProfile profile = store.getUserProfile();
        if (profile == null) {
            return false;
        }
        YearMonth lastSeenMonth = readLastSeenMonth();
        return !targetMonth.equals(lastSeenMonth);
    }

    public void acknowledgeMonth(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        writeLastSeenMonth(targetMonth);
    }

    public List<ExpenseTemplateCandidate> buildExpenseTemplateCandidates(YearMonth sourceMonth) {
        YearMonth month = ValidationUtils.requireNonNull(sourceMonth, "sourceMonth");
        List<ExpenseEntry> expenses = store.listExpenseEntries(month);
        if (expenses.isEmpty()) {
            return List.of();
        }

        Map<String, TemplateAggregate> aggregateMap = new LinkedHashMap<>();
        for (ExpenseEntry expense : expenses) {
            PlannerBucket bucket = expense.getPlannerBucket() == null
                    ? PlannerBucket.inferFromCategory(expense.getCategory())
                    : expense.getPlannerBucket();
            String key = buildTemplateKey(expense, bucket);
            TemplateAggregate aggregate = aggregateMap.computeIfAbsent(key, unused -> new TemplateAggregate(expense, bucket, key));
            aggregate.include(expense);
        }

        return aggregateMap.values().stream()
                .map(TemplateAggregate::toCandidate)
                .sorted(Comparator
                        .comparing((ExpenseTemplateCandidate candidate) -> candidate.getBucket().ordinal())
                        .thenComparing(ExpenseTemplateCandidate::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public MonthRolloverResult startNewMonth(YearMonth month, MonthRolloverOptions options) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        MonthRolloverOptions effectiveOptions = options == null ? new MonthRolloverOptions() : options;
        YearMonth previousMonth = targetMonth.minusMonths(1);

        int copiedIncomeEntries = 0;
        int copiedExpenseEntries = 0;
        boolean copiedPlannerPlan = false;

        if (effectiveOptions.isCopyPlannerPlan() && store.getMonthlyPlan(targetMonth) == null) {
            MonthlyPlan previousPlan = store.getMonthlyPlan(previousMonth);
            if (previousPlan != null) {
                store.saveMonthlyPlan(cloneMonthlyPlan(previousPlan, targetMonth));
                copiedPlannerPlan = true;
            }
        }

        if (effectiveOptions.isCarryForwardRecurringIncome()) {
            List<IncomeEntry> recurringIncome = store.listIncomeEntries(previousMonth).stream()
                    .filter(IncomeEntry::isRecurring)
                    .toList();
            for (IncomeEntry source : recurringIncome) {
                store.saveIncomeEntry(cloneRecurringIncome(source, targetMonth));
                copiedIncomeEntries++;
            }
        }

        if (effectiveOptions.isCarryForwardRecurringExpenses()) {
            for (ExpenseTemplateSelection selection : effectiveOptions.getSelectedExpenseTemplates()) {
                ExpenseTemplateCandidate candidate = selection.getCandidate();
                if (candidate == null) {
                    continue;
                }
                store.saveExpenseEntry(cloneTemplateExpense(candidate, targetMonth, selection.isMarkRecurring()));
                copiedExpenseEntries++;
            }
        }

        acknowledgeMonth(targetMonth);
        return new MonthRolloverResult(copiedPlannerPlan, copiedIncomeEntries, copiedExpenseEntries);
    }

    private MonthlyPlan cloneMonthlyPlan(MonthlyPlan source, YearMonth targetMonth) {
        MonthlyPlan clone = source.copy();
        LocalDateTime now = LocalDateTime.now();
        clone.setId(UUID.randomUUID().toString());
        clone.setMonth(targetMonth);
        clone.setCreatedAt(now);
        clone.setUpdatedAt(now);
        return clone;
    }

    private IncomeEntry cloneRecurringIncome(IncomeEntry source, YearMonth targetMonth) {
        IncomeEntry clone = source.copy();
        LocalDateTime now = LocalDateTime.now();
        int day = Math.min(source.getReceivedDate().getDayOfMonth(), targetMonth.lengthOfMonth());
        clone.setId(UUID.randomUUID().toString());
        clone.setMonth(targetMonth);
        clone.setReceivedDate(targetMonth.atDay(day));
        clone.setReceived(false);
        clone.setCreatedAt(now);
        clone.setUpdatedAt(now);
        return clone;
    }

    private ExpenseEntry cloneTemplateExpense(ExpenseTemplateCandidate candidate, YearMonth targetMonth, boolean recurring) {
        ExpenseEntry clone = new ExpenseEntry();
        LocalDateTime now = LocalDateTime.now();
        clone.setId(UUID.randomUUID().toString());
        clone.setMonth(targetMonth);
        clone.setExpenseDate(LocalDate.of(targetMonth.getYear(), targetMonth.getMonth(), 1));
        clone.setAmount(candidate.getLastAmount().compareTo(BigDecimal.ZERO) > 0
                ? candidate.getLastAmount()
                : candidate.getAvgAmount());
        clone.setCategory(candidate.getCategory());
        clone.setPlannerBucket(candidate.getBucket());
        clone.setSubcategory(candidate.getSubcategory());
        clone.setPaymentMethod(candidate.getPaymentMethod() == null ? clone.getPaymentMethod() : candidate.getPaymentMethod());
        clone.setTag(candidate.getTag());
        clone.setNote(candidate.getNote());
        clone.setRecurring(recurring);
        clone.setCreatedAt(now);
        clone.setUpdatedAt(now);
        return clone;
    }

    private String buildTemplateKey(ExpenseEntry expense, PlannerBucket bucket) {
        String normalizedSubcategory = normalize(expense.getSubcategory());
        String normalizedTag = normalize(expense.getTag());
        String normalizedFallback = normalizedSubcategory.isBlank()
                ? normalize(firstTwenty(expense.getNote()))
                : "";
        return bucket.name()
                + "|"
                + expense.getCategory().name()
                + "|"
                + normalizedSubcategory
                + "|"
                + normalizedTag
                + "|"
                + normalizedFallback;
    }

    private String buildDisplayName(ExpenseEntry expense) {
        String subcategory = safeTrim(expense.getSubcategory());
        if (!subcategory.isBlank()) {
            return subcategory;
        }
        String note = safeTrim(expense.getNote());
        if (!note.isBlank()) {
            return firstTwenty(note);
        }
        return expense.getCategory().getLabel();
    }

    private String firstTwenty(String text) {
        String normalized = safeTrim(text);
        if (normalized.length() <= 20) {
            return normalized;
        }
        return normalized.substring(0, 20).trim();
    }

    private String normalize(String value) {
        String trimmed = safeTrim(value).toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private YearMonth readLastSeenMonth() {
        if (store instanceof FullDataStore fullDataStore) {
            String raw = fullDataStore.getAppSetting(LAST_SEEN_MONTH_KEY);
            if (raw != null && !raw.isBlank()) {
                try {
                    return YearMonth.parse(raw.trim());
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            return null;
        }
        return inMemoryLastSeenMonth;
    }

    private void writeLastSeenMonth(YearMonth month) {
        if (store instanceof FullDataStore fullDataStore) {
            fullDataStore.saveAppSetting(LAST_SEEN_MONTH_KEY, month.toString());
            return;
        }
        inMemoryLastSeenMonth = month;
    }

    private final class TemplateAggregate {
        private final String key;
        private final PlannerBucket bucket;
        private final String displayName;

        private ExpenseEntry latestExpense;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int countOccurrences;
        private boolean wasRecurring;

        private TemplateAggregate(ExpenseEntry seed, PlannerBucket bucket, String key) {
            this.key = key;
            this.bucket = bucket;
            this.displayName = buildDisplayName(seed);
            include(seed);
        }

        private void include(ExpenseEntry expense) {
            totalAmount = totalAmount.add(MoneyUtils.zeroIfNull(expense.getAmount()));
            countOccurrences++;
            wasRecurring = wasRecurring || expense.isRecurring();
            if (latestExpense == null || isNewer(expense, latestExpense)) {
                latestExpense = expense;
            }
        }

        private boolean isNewer(ExpenseEntry left, ExpenseEntry right) {
            if (left.getExpenseDate() == null) {
                return false;
            }
            if (right.getExpenseDate() == null) {
                return true;
            }
            int dateCompare = left.getExpenseDate().compareTo(right.getExpenseDate());
            if (dateCompare != 0) {
                return dateCompare > 0;
            }
            if (left.getCreatedAt() == null || right.getCreatedAt() == null) {
                return false;
            }
            return left.getCreatedAt().isAfter(right.getCreatedAt());
        }

        private ExpenseTemplateCandidate toCandidate() {
            BigDecimal average = countOccurrences == 0
                    ? BigDecimal.ZERO
                    : totalAmount.divide(BigDecimal.valueOf(countOccurrences), 2, RoundingMode.HALF_UP);
            BigDecimal lastAmount = latestExpense == null
                    ? BigDecimal.ZERO
                    : MoneyUtils.zeroIfNull(latestExpense.getAmount());
            return new ExpenseTemplateCandidate(
                    key,
                    bucket,
                    latestExpense.getCategory(),
                    safeTrim(latestExpense.getSubcategory()),
                    displayName,
                    lastAmount,
                    average,
                    latestExpense.getPaymentMethod(),
                    wasRecurring,
                    countOccurrences,
                    safeTrim(latestExpense.getTag()),
                    safeTrim(latestExpense.getNote())
            );
        }
    }

    public static class MonthRolloverResult {
        private final boolean plannerCopied;
        private final int recurringIncomeCopied;
        private final int recurringExpensesCopied;

        public MonthRolloverResult(boolean plannerCopied, int recurringIncomeCopied, int recurringExpensesCopied) {
            this.plannerCopied = plannerCopied;
            this.recurringIncomeCopied = recurringIncomeCopied;
            this.recurringExpensesCopied = recurringExpensesCopied;
        }

        public boolean isPlannerCopied() {
            return plannerCopied;
        }

        public int getRecurringIncomeCopied() {
            return recurringIncomeCopied;
        }

        public int getRecurringExpensesCopied() {
            return recurringExpensesCopied;
        }
    }
}
