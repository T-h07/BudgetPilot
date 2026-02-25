package com.budgetpilot.service;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.enums.HabitSeverity;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HabitService {
    private final BudgetStore budgetStore;
    private final ExpenseService expenseService;

    public HabitService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.expenseService = new ExpenseService(budgetStore);
    }

    public List<HabitRule> listRules() {
        return budgetStore.listHabitRules().stream()
                .sorted(Comparator.comparing(HabitRule::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void saveRule(HabitRule rule) {
        HabitValidationResult validation = validateRule(rule);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }
        HabitRule copy = rule.copy();
        copy.setTag(normalizeTag(copy.getTag()));
        budgetStore.saveHabitRule(copy);
    }

    public void deleteRule(String ruleId) {
        budgetStore.deleteHabitRule(ValidationUtils.requireNonBlank(ruleId, "ruleId"));
    }

    public List<HabitSpendSummary> evaluateHabits(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<ExpenseEntry> monthExpenses = expenseService.listForMonth(targetMonth);
        List<HabitSpendSummary> evaluations = new ArrayList<>();

        for (HabitRule rule : listRules()) {
            BigDecimal actualSpend = BigDecimal.ZERO;
            int matchedExpenseCount = 0;
            for (ExpenseEntry expense : monthExpenses) {
                if (matchesRule(expense, rule)) {
                    actualSpend = actualSpend.add(expense.getAmount());
                    matchedExpenseCount++;
                }
            }
            actualSpend = MoneyUtils.normalize(actualSpend);

            BigDecimal limit = MoneyUtils.zeroIfNull(rule.getMonthlyLimit());
            BigDecimal warningThreshold = MoneyUtils.zeroIfNull(rule.getWarningThresholdPercent());
            BigDecimal warningAmount = MoneyUtils.percentOf(limit, warningThreshold);

            BigDecimal remainingBeforeLimit = MoneyUtils.safeSubtract(limit, actualSpend);
            BigDecimal usagePercent = limit.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : MoneyUtils.normalize(actualSpend.multiply(MoneyUtils.HUNDRED).divide(limit, 2, RoundingMode.HALF_UP));

            HabitStatus status = resolveStatus(rule.isActive(), actualSpend, limit, warningAmount);
            evaluations.add(new HabitSpendSummary(
                    rule.getId(),
                    normalizeTag(rule.getTag()),
                    rule.getDisplayName(),
                    rule.getLinkedCategory(),
                    limit,
                    warningThreshold,
                    actualSpend,
                    remainingBeforeLimit,
                    usagePercent,
                    status,
                    matchedExpenseCount,
                    rule.isActive()
            ));
        }

        evaluations.sort(Comparator
                .comparingInt((HabitSpendSummary summary) -> statusRank(summary.getStatus()))
                .thenComparing(HabitSpendSummary::getUsagePercent, Comparator.reverseOrder())
                .thenComparing(HabitSpendSummary::getDisplayName));
        return List.copyOf(evaluations);
    }

    public HabitPageSummary getHabitPageSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<HabitSpendSummary> evaluations = evaluateHabits(targetMonth);
        List<HabitSpendSummary> activeEvaluations = evaluations.stream()
                .filter(HabitSpendSummary::isActive)
                .toList();

        BigDecimal trackedSpend = MoneyUtils.normalize(activeEvaluations.stream()
                .map(HabitSpendSummary::getActualSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int warningCount = (int) activeEvaluations.stream()
                .filter(summary -> summary.getStatus() == HabitStatus.WARNING)
                .count();
        int exceededCount = (int) activeEvaluations.stream()
                .filter(summary -> summary.getStatus() == HabitStatus.EXCEEDED)
                .count();
        int onTrackCount = (int) activeEvaluations.stream()
                .filter(summary -> summary.getStatus() == HabitStatus.ON_TRACK)
                .count();

        return new HabitPageSummary(
                targetMonth,
                trackedSpend,
                activeEvaluations.size(),
                warningCount,
                exceededCount,
                onTrackCount,
                evaluations,
                getInsights(targetMonth)
        );
    }

    public List<HabitInsight> getInsights(YearMonth month) {
        List<HabitSpendSummary> activeEvaluations = evaluateHabits(ValidationUtils.requireNonNull(month, "month")).stream()
                .filter(HabitSpendSummary::isActive)
                .toList();

        List<HabitInsight> insights = new ArrayList<>();
        if (activeEvaluations.isEmpty()) {
            insights.add(new HabitInsight(
                    "no-rules",
                    HabitSeverity.INFO,
                    "No habit rules configured",
                    "Add a habit rule to track spending behavior by tag or category.",
                    "Create your first rule in the Habit Rule form."
            ));
            return List.copyOf(insights);
        }

        BigDecimal trackedSpend = MoneyUtils.normalize(activeEvaluations.stream()
                .map(HabitSpendSummary::getActualSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        if (trackedSpend.compareTo(BigDecimal.ZERO) <= 0) {
            insights.add(new HabitInsight(
                    "no-tracked-spend",
                    HabitSeverity.INFO,
                    "No habit-tracked spending",
                    "No expenses matched your active habit rules this month.",
                    "Use consistent tags on expenses to enable habit analysis."
            ));
        }

        activeEvaluations.stream()
                .filter(summary -> summary.getStatus() == HabitStatus.EXCEEDED)
                .limit(3)
                .forEach(summary -> insights.add(new HabitInsight(
                        "exceeded-" + summary.getRuleId(),
                        HabitSeverity.DANGER,
                        summary.getDisplayName() + " exceeded limit",
                        summary.getDisplayName() + " exceeded monthly limit by "
                                + MoneyUtils.normalize(summary.getActualSpend().subtract(summary.getMonthlyLimit())).toPlainString() + ".",
                        "Reduce spending pace for this habit category/tag."
                )));

        activeEvaluations.stream()
                .filter(summary -> summary.getStatus() == HabitStatus.WARNING)
                .limit(3)
                .forEach(summary -> insights.add(new HabitInsight(
                        "warning-" + summary.getRuleId(),
                        HabitSeverity.WARNING,
                        summary.getDisplayName() + " near limit",
                        summary.getDisplayName() + " reached " + summary.getUsagePercent().stripTrailingZeros().toPlainString()
                                + "% of its monthly limit.",
                        "Slow down spending before the limit is exceeded."
                )));

        boolean hasRisk = activeEvaluations.stream()
                .anyMatch(summary -> summary.getStatus() == HabitStatus.WARNING || summary.getStatus() == HabitStatus.EXCEEDED);
        if (!hasRisk) {
            insights.add(new HabitInsight(
                    "all-on-track",
                    HabitSeverity.INFO,
                    "All habits on track",
                    "All active habit rules are currently within safe spending limits.",
                    "Keep tracking to maintain consistency."
            ));
        }

        insights.sort(Comparator.comparingInt((HabitInsight insight) -> severityRank(insight.getSeverity())));
        return List.copyOf(insights.stream().limit(5).toList());
    }

    public BigDecimal getTotalHabitTrackedSpend(YearMonth month) {
        return getHabitPageSummary(month).getHabitTrackedSpend();
    }

    public int getExceededHabitsCount(YearMonth month) {
        return getHabitPageSummary(month).getExceededCount();
    }

    public int getWarningHabitsCount(YearMonth month) {
        return getHabitPageSummary(month).getWarningCount();
    }

    public HabitValidationResult validateRule(HabitRule rule) {
        HabitValidationResult result = new HabitValidationResult();
        if (rule == null) {
            result.addError("Habit rule is required.");
            return result;
        }
        if (rule.getDisplayName() == null || rule.getDisplayName().isBlank()) {
            result.addError("Habit name is required.");
        }
        if (rule.getTag() == null || rule.getTag().isBlank()) {
            result.addError("Tag is required.");
        }
        if (rule.getMonthlyLimit() == null || rule.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Monthly limit must be greater than 0.");
        }
        if (rule.getWarningThresholdPercent() == null
                || rule.getWarningThresholdPercent().compareTo(BigDecimal.ZERO) < 0
                || rule.getWarningThresholdPercent().compareTo(MoneyUtils.HUNDRED) > 0) {
            result.addError("Warning threshold must be between 0 and 100.");
        }
        return result;
    }

    private HabitStatus resolveStatus(boolean active, BigDecimal actualSpend, BigDecimal limit, BigDecimal warningAmount) {
        if (!active) {
            return HabitStatus.ON_TRACK;
        }
        if (actualSpend.compareTo(limit) > 0) {
            return HabitStatus.EXCEEDED;
        }
        if (actualSpend.compareTo(warningAmount) >= 0) {
            return HabitStatus.WARNING;
        }
        return HabitStatus.ON_TRACK;
    }

    private boolean matchesRule(ExpenseEntry expense, HabitRule rule) {
        boolean tagMatch = false;
        if (expense.getTag() != null && !expense.getTag().isBlank() && rule.getTag() != null && !rule.getTag().isBlank()) {
            tagMatch = normalizeTag(expense.getTag()).equalsIgnoreCase(normalizeTag(rule.getTag()));
        }
        boolean categoryMatch = rule.getLinkedCategory() != null && expense.getCategory() == rule.getLinkedCategory();
        return tagMatch || categoryMatch;
    }

    private String normalizeTag(String rawTag) {
        String normalized = ValidationUtils.requireNonBlank(rawTag, "tag").toLowerCase(Locale.ROOT);
        return normalized.startsWith("#") ? normalized : "#" + normalized;
    }

    private int statusRank(HabitStatus status) {
        return switch (status) {
            case EXCEEDED -> 0;
            case WARNING -> 1;
            case ON_TRACK -> 2;
        };
    }

    private int severityRank(HabitSeverity severity) {
        return switch (severity) {
            case DANGER -> 0;
            case WARNING -> 1;
            case INFO -> 2;
        };
    }
}
