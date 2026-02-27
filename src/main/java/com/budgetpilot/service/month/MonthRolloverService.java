package com.budgetpilot.service.month;

import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.service.templates.TemplateGenerationResult;
import com.budgetpilot.service.templates.TemplateService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.FullDataStore;
import com.budgetpilot.util.ValidationUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public class MonthRolloverService {
    public static final String LAST_SEEN_MONTH_KEY = "last_seen_month";
    public static final String LAST_GENERATED_MONTH_KEY = "last_rollover_generated_month";
    public static final String LAST_GENERATED_EXPENSE_COUNT_KEY = "last_rollover_generated_expense_count";
    public static final String LAST_GENERATED_INCOME_COUNT_KEY = "last_rollover_generated_income_count";

    private final BudgetStore store;
    private final TemplateService templateService;
    private YearMonth inMemoryLastSeenMonth;
    private YearMonth inMemoryLastGeneratedMonth;
    private int inMemoryGeneratedExpenseCount;
    private int inMemoryGeneratedIncomeCount;

    public MonthRolloverService(BudgetStore store) {
        this.store = ValidationUtils.requireNonNull(store, "store");
        this.templateService = new TemplateService(this.store);
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

    public List<ExpenseTemplate> listActiveExpenseTemplates() {
        return templateService.listActiveExpenseTemplates();
    }

    public List<IncomeTemplate> listActiveIncomeTemplates() {
        return templateService.listActiveIncomeTemplates();
    }

    public MonthRolloverResult startNewMonth(YearMonth month, MonthRolloverOptions options) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        MonthRolloverOptions effectiveOptions = options == null ? new MonthRolloverOptions() : options;
        YearMonth previousMonth = targetMonth.minusMonths(1);

        boolean copiedPlannerPlan = false;
        if (effectiveOptions.isCopyPlannerPlan() && store.getMonthlyPlan(targetMonth) == null) {
            MonthlyPlan previousPlan = store.getMonthlyPlan(previousMonth);
            if (previousPlan != null) {
                store.saveMonthlyPlan(cloneMonthlyPlan(previousPlan, targetMonth));
                copiedPlannerPlan = true;
            }
        }

        TemplateGenerationResult generated = templateService.generateAndSaveForMonth(
                targetMonth,
                effectiveOptions.getSelectedExpenseTemplateIds(),
                effectiveOptions.getSelectedIncomeTemplateIds()
        );

        acknowledgeMonth(targetMonth);
        writeGenerationStats(
                targetMonth,
                generated.getGeneratedExpenseCount(),
                generated.getGeneratedIncomeCount()
        );

        return new MonthRolloverResult(
                copiedPlannerPlan,
                generated.getGeneratedExpenseCount(),
                generated.getGeneratedIncomeCount()
        );
    }

    public YearMonth readLastGeneratedMonth() {
        if (store instanceof FullDataStore fullDataStore) {
            String raw = fullDataStore.getAppSetting(LAST_GENERATED_MONTH_KEY);
            if (raw != null && !raw.isBlank()) {
                try {
                    return YearMonth.parse(raw.trim());
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            return null;
        }
        return inMemoryLastGeneratedMonth;
    }

    public int readLastGeneratedExpenseCount() {
        if (store instanceof FullDataStore fullDataStore) {
            String raw = fullDataStore.getAppSetting(LAST_GENERATED_EXPENSE_COUNT_KEY);
            return parseInt(raw, 0);
        }
        return inMemoryGeneratedExpenseCount;
    }

    public int readLastGeneratedIncomeCount() {
        if (store instanceof FullDataStore fullDataStore) {
            String raw = fullDataStore.getAppSetting(LAST_GENERATED_INCOME_COUNT_KEY);
            return parseInt(raw, 0);
        }
        return inMemoryGeneratedIncomeCount;
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

    private void writeGenerationStats(YearMonth month, int expenseCount, int incomeCount) {
        if (store instanceof FullDataStore fullDataStore) {
            fullDataStore.saveAppSetting(LAST_GENERATED_MONTH_KEY, month.toString());
            fullDataStore.saveAppSetting(LAST_GENERATED_EXPENSE_COUNT_KEY, String.valueOf(Math.max(0, expenseCount)));
            fullDataStore.saveAppSetting(LAST_GENERATED_INCOME_COUNT_KEY, String.valueOf(Math.max(0, incomeCount)));
            return;
        }
        inMemoryLastGeneratedMonth = month;
        inMemoryGeneratedExpenseCount = Math.max(0, expenseCount);
        inMemoryGeneratedIncomeCount = Math.max(0, incomeCount);
    }

    private int parseInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static class MonthRolloverResult {
        private final boolean plannerCopied;
        private final int recurringExpensesCopied;
        private final int recurringIncomeCopied;

        public MonthRolloverResult(boolean plannerCopied, int recurringExpensesCopied, int recurringIncomeCopied) {
            this.plannerCopied = plannerCopied;
            this.recurringExpensesCopied = recurringExpensesCopied;
            this.recurringIncomeCopied = recurringIncomeCopied;
        }

        public boolean isPlannerCopied() {
            return plannerCopied;
        }

        public int getRecurringExpensesCopied() {
            return recurringExpensesCopied;
        }

        public int getRecurringIncomeCopied() {
            return recurringIncomeCopied;
        }
    }
}
