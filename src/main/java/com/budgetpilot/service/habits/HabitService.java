package com.budgetpilot.service.habits;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.enums.HabitAllowanceMode;
import com.budgetpilot.model.enums.HabitSeverity;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.balance.MonthlyBalanceService;
import com.budgetpilot.service.balance.MonthlyBalanceSnapshot;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.FullDataStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HabitService {
    private static final BigDecimal WARNING_USAGE_PERCENT = new BigDecimal("80.00");
    private static final BigDecimal PACE_OK_MULTIPLIER = new BigDecimal("1.10");
    private static final BigDecimal DEFAULT_HABIT_PERCENT = new BigDecimal("10.00");
    private static final Set<PlannerBucket> DISCRETIONARY_TRACKED_BUCKETS =
            EnumSet.of(PlannerBucket.DISCRETIONARY, PlannerBucket.UNPLANNED);
    private static final String LOCKED_POOL_KEY_PREFIX = "habit_pool_locked_";

    private final BudgetStore budgetStore;
    private final ExpenseService expenseService;
    private final MonthlyBalanceService monthlyBalanceService;
    private final Map<YearMonth, BigDecimal> lastDynamicPoolByMonth = new HashMap<>();

    public HabitService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.expenseService = new ExpenseService(this.budgetStore);
        this.monthlyBalanceService = new MonthlyBalanceService(this.budgetStore);
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

    public HabitAllowanceSnapshot buildAllowanceSnapshot(YearMonth month) {
        return buildAllowanceSnapshot(month, true);
    }

    public HabitAllowanceSnapshot buildAllowanceSnapshot(YearMonth month, boolean onlyDiscretionaryAndUnplanned) {
        return buildAllowanceSnapshot(month, onlyDiscretionaryAndUnplanned, LocalDate.now());
    }

    public HabitAllowanceSnapshot buildAllowanceSnapshot(
            YearMonth month,
            boolean onlyDiscretionaryAndUnplanned,
            LocalDate referenceDate
    ) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        MonthlyPlan plan = resolvePlan(targetMonth);
        BigDecimal habitPercent = MoneyUtils.zeroIfNull(plan.getHabitPercent());
        HabitAllowanceMode mode = plan.getHabitMode() == null ? HabitAllowanceMode.DYNAMIC : plan.getHabitMode();
        PaceContext paceContext = resolvePaceContext(targetMonth, referenceDate);

        MonthlyBalanceSnapshot balance = monthlyBalanceService.buildSnapshot(targetMonth);
        BigDecimal availableAfterReserve = MoneyUtils.zeroIfNull(balance.getAvailableAfterReserve());
        BigDecimal dynamicPool = MoneyUtils.percentOf(availableAfterReserve, habitPercent);
        if (dynamicPool.compareTo(BigDecimal.ZERO) < 0) {
            dynamicPool = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal poolAmount = resolvePoolAmount(targetMonth, mode, dynamicPool);
        boolean poolAdjustedDown = detectPoolAdjustedDown(targetMonth, mode, poolAmount);

        List<ExpenseEntry> monthExpenses = expenseService.listForMonth(targetMonth);
        List<ExpenseEntry> trackedExpenses = onlyDiscretionaryAndUnplanned
                ? monthExpenses.stream()
                .filter(entry -> DISCRETIONARY_TRACKED_BUCKETS.contains(resolvePlannerBucket(entry)))
                .toList()
                : monthExpenses;

        List<HabitRule> rules = listRules();
        int totalWeight = rules.stream()
                .filter(this::isEffectiveEnabled)
                .mapToInt(HabitRule::getWeight)
                .sum();

        List<HabitAllowanceRow> rows = new ArrayList<>();
        for (HabitRule rule : rules) {
            BigDecimal spent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            int matchedCount = 0;
            for (ExpenseEntry expense : trackedExpenses) {
                if (matchesRule(expense, rule)) {
                    spent = MoneyUtils.normalize(spent.add(expense.getAmount()));
                    matchedCount++;
                }
            }

            boolean enabled = isEffectiveEnabled(rule);
            BigDecimal allocatedCap = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (enabled && totalWeight > 0) {
                allocatedCap = MoneyUtils.normalize(
                        poolAmount
                                .multiply(BigDecimal.valueOf(rule.getWeight()))
                                .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP)
                );
            }

            BigDecimal hardLimit = MoneyUtils.zeroIfNull(rule.getMonthlyLimit());
            BigDecimal baseline = MoneyUtils.zeroIfNull(rule.getBaselineAmount());
            BigDecimal finalCap = hardLimit.compareTo(BigDecimal.ZERO) > 0
                    ? allocatedCap.min(hardLimit)
                    : allocatedCap;
            finalCap = MoneyUtils.normalize(finalCap);

            BigDecimal excessSpent = MoneyUtils.safeSubtract(spent, baseline);
            if (excessSpent.compareTo(BigDecimal.ZERO) < 0) {
                excessSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal capToDate = MoneyUtils.normalize(
                    finalCap.multiply(paceContext.paceRatio())
            );
            BigDecimal remaining = MoneyUtils.safeSubtract(finalCap, excessSpent);
            BigDecimal usagePercent = calculateUsagePercent(finalCap, excessSpent);
            HabitStatus status = resolveStatus(enabled, finalCap, capToDate, excessSpent);

            rows.add(new HabitAllowanceRow(
                    rule.getId(),
                    rule.getDisplayName(),
                    normalizeTag(rule.getTag()),
                    rule.getLinkedCategory(),
                    rule.isActive(),
                    rule.isEnabledThisMonth(),
                    rule.getWeight(),
                    hardLimit,
                    baseline,
                    allocatedCap,
                    finalCap,
                    spent,
                    excessSpent,
                    capToDate,
                    remaining,
                    usagePercent,
                    status,
                    matchedCount
            ));
        }

        rows.sort(Comparator
                .comparingInt((HabitAllowanceRow row) -> statusRank(row.getStatus()))
                .thenComparing(HabitAllowanceRow::getUsagePercent, Comparator.reverseOrder())
                .thenComparing(HabitAllowanceRow::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        List<HabitAllowanceRow> enabledRows = rows.stream()
                .filter(HabitAllowanceRow::isEffectiveEnabled)
                .toList();

        BigDecimal spentAcrossHabits = MoneyUtils.normalize(enabledRows.stream()
                .map(HabitAllowanceRow::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal excessAcrossHabits = MoneyUtils.normalize(enabledRows.stream()
                .map(HabitAllowanceRow::getExcessSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal remainingPool = MoneyUtils.safeSubtract(poolAmount, excessAcrossHabits);

        int warningCount = (int) enabledRows.stream()
                .filter(row -> row.getStatus() == HabitStatus.WARNING)
                .count();
        int exceededCount = (int) enabledRows.stream()
                .filter(row -> row.getStatus() == HabitStatus.EXCEEDED)
                .count();
        int onTrackCount = (int) enabledRows.stream()
                .filter(row -> row.getStatus() == HabitStatus.ON_TRACK)
                .count();

        return new HabitAllowanceSnapshot(
                targetMonth,
                habitPercent,
                mode,
                availableAfterReserve,
                poolAmount,
                spentAcrossHabits,
                excessAcrossHabits,
                remainingPool,
                paceContext.daysElapsed(),
                paceContext.daysInMonth(),
                paceContext.paceRatio(),
                totalWeight,
                enabledRows.size(),
                warningCount,
                exceededCount,
                onTrackCount,
                onlyDiscretionaryAndUnplanned,
                poolAdjustedDown,
                rows
        );
    }

    public List<HabitSpendSummary> evaluateHabits(YearMonth month) {
        return evaluateHabits(month, true);
    }

    public List<HabitSpendSummary> evaluateHabits(YearMonth month, boolean onlyDiscretionaryAndUnplanned) {
        HabitAllowanceSnapshot snapshot = buildAllowanceSnapshot(month, onlyDiscretionaryAndUnplanned);
        return toSpendSummaries(snapshot);
    }

    public HabitPageSummary getHabitPageSummary(YearMonth month) {
        return getHabitPageSummary(month, true);
    }

    public HabitPageSummary getHabitPageSummary(YearMonth month, boolean onlyDiscretionaryAndUnplanned) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        HabitAllowanceSnapshot allowanceSnapshot = buildAllowanceSnapshot(targetMonth, onlyDiscretionaryAndUnplanned);
        List<HabitSpendSummary> evaluations = toSpendSummaries(allowanceSnapshot);

        return new HabitPageSummary(
                targetMonth,
                allowanceSnapshot.getSpentAcrossHabits(),
                allowanceSnapshot.getExcessAcrossHabits(),
                allowanceSnapshot.getEnabledHabitsCount(),
                allowanceSnapshot.getWarningCount(),
                allowanceSnapshot.getExceededCount(),
                allowanceSnapshot.getOnTrackCount(),
                allowanceSnapshot,
                evaluations,
                buildInsights(allowanceSnapshot, evaluations)
        );
    }

    public List<HabitInsight> getInsights(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        HabitAllowanceSnapshot allowanceSnapshot = buildAllowanceSnapshot(targetMonth, true);
        return buildInsights(allowanceSnapshot, toSpendSummaries(allowanceSnapshot));
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
        if (rule.getMonthlyLimit() == null || rule.getMonthlyLimit().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Hard limit must be non-negative.");
        }
        if (rule.getBaselineAmount() == null || rule.getBaselineAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Baseline must be non-negative.");
        }
        if (rule.getWarningThresholdPercent() == null
                || rule.getWarningThresholdPercent().compareTo(BigDecimal.ZERO) < 0
                || rule.getWarningThresholdPercent().compareTo(MoneyUtils.HUNDRED) > 0) {
            result.addError("Warning threshold must be between 0 and 100.");
        }
        if (rule.getWeight() < 1 || rule.getWeight() > 10) {
            result.addError("Weight must be between 1 and 10.");
        }
        return result;
    }

    private List<HabitSpendSummary> toSpendSummaries(HabitAllowanceSnapshot snapshot) {
        List<HabitSpendSummary> summaries = new ArrayList<>();
        for (HabitAllowanceRow row : snapshot.getRows()) {
            summaries.add(new HabitSpendSummary(
                    row.getRuleId(),
                    row.getTag(),
                    row.getDisplayName(),
                    row.getLinkedCategory(),
                    row.getFinalCapAmount(),
                    row.getBaselineAmount(),
                    WARNING_USAGE_PERCENT,
                    row.getSpentAmount(),
                    row.getExcessSpentAmount(),
                    row.getCapToDateAmount(),
                    row.getRemainingAmount(),
                    row.getUsagePercent(),
                    row.getStatus(),
                    row.getMatchedExpenseCount(),
                    row.isEffectiveEnabled()
            ));
        }
        return List.copyOf(summaries);
    }

    private List<HabitInsight> buildInsights(HabitAllowanceSnapshot snapshot, List<HabitSpendSummary> evaluations) {
        List<HabitInsight> insights = new ArrayList<>();

        if (snapshot.getRows().isEmpty()) {
            insights.add(new HabitInsight(
                    "no-rules",
                    HabitSeverity.INFO,
                    "No habit rules configured",
                    "Add a habit rule to distribute your behavior allowance.",
                    "Create your first rule in Habits."
            ));
            return List.copyOf(insights);
        }

        if (snapshot.getEnabledHabitsCount() == 0) {
            insights.add(new HabitInsight(
                    "no-enabled-rules",
                    HabitSeverity.INFO,
                    "No habits enabled this month",
                    "Enable at least one habit rule to allocate the monthly habit pool.",
                    "Toggle Enabled on a habit rule card."
            ));
            return List.copyOf(insights);
        }

        if (snapshot.getExceededCount() > 0) {
            insights.add(new HabitInsight(
                    "habits-exceeded",
                    HabitSeverity.DANGER,
                    "Habits exceeded allowance",
                    snapshot.getExceededCount() + " enabled habit(s) exceeded pace-adjusted allowance.",
                    "Reduce discretionary habit spending pace."
            ));
        }

        if (snapshot.getWarningCount() > 0) {
            insights.add(new HabitInsight(
                    "habits-warning",
                    HabitSeverity.WARNING,
                    "Habits near allowance limit",
                    snapshot.getWarningCount() + " enabled habit(s) are ahead of pace versus allocated caps.",
                    "Scale back before allowance is exceeded."
            ));
        }

        boolean hasRisk = snapshot.getExceededCount() > 0 || snapshot.getWarningCount() > 0;
        if (!hasRisk) {
            insights.add(new HabitInsight(
                    "all-on-track",
                    HabitSeverity.INFO,
                    "Habits within allowance",
                    "All enabled habit rules are within pace-adjusted allowance.",
                    "Keep spending consistent."
            ));
        }

        if (snapshot.getExcessAcrossHabits().compareTo(BigDecimal.ZERO) <= 0) {
            insights.add(new HabitInsight(
                    "no-tracked-spend",
                    HabitSeverity.INFO,
                    "No excess habit spend tracked",
                    "Current habit spending is within configured baselines.",
                    "Baselines are absorbing routine habit spend."
            ));
        }

        insights.sort(Comparator.comparingInt((HabitInsight insight) -> severityRank(insight.getSeverity())));
        return List.copyOf(insights.stream().limit(5).toList());
    }

    private BigDecimal calculateUsagePercent(BigDecimal cap, BigDecimal excessSpent) {
        if (cap.compareTo(BigDecimal.ZERO) <= 0) {
            return excessSpent.compareTo(BigDecimal.ZERO) > 0
                    ? MoneyUtils.HUNDRED
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return MoneyUtils.normalize(excessSpent.multiply(MoneyUtils.HUNDRED).divide(cap, 2, RoundingMode.HALF_UP));
    }

    private HabitStatus resolveStatus(
            boolean enabled,
            BigDecimal fullMonthCap,
            BigDecimal capToDate,
            BigDecimal excessSpent
    ) {
        if (!enabled) {
            return HabitStatus.ON_TRACK;
        }
        if (fullMonthCap.compareTo(BigDecimal.ZERO) <= 0) {
            return excessSpent.compareTo(BigDecimal.ZERO) > 0 ? HabitStatus.EXCEEDED : HabitStatus.ON_TRACK;
        }
        if (excessSpent.compareTo(fullMonthCap) > 0) {
            return HabitStatus.EXCEEDED;
        }

        BigDecimal okThreshold = MoneyUtils.normalize(capToDate.multiply(PACE_OK_MULTIPLIER));
        if (excessSpent.compareTo(okThreshold) > 0) {
            return HabitStatus.WARNING;
        }
        return HabitStatus.ON_TRACK;
    }

    private MonthlyPlan resolvePlan(YearMonth month) {
        MonthlyPlan existing = budgetStore.getMonthlyPlan(month);
        if (existing != null) {
            if (existing.getHabitPercent() == null) {
                existing.setHabitPercent(DEFAULT_HABIT_PERCENT);
            }
            if (existing.getHabitMode() == null) {
                existing.setHabitMode(HabitAllowanceMode.DYNAMIC);
            }
            return existing;
        }

        MonthlyPlan fallback = new MonthlyPlan(month);
        fallback.setHabitPercent(DEFAULT_HABIT_PERCENT);
        fallback.setHabitMode(HabitAllowanceMode.DYNAMIC);
        return fallback;
    }

    private PaceContext resolvePaceContext(YearMonth month, LocalDate referenceDate) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        LocalDate refDate = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth refMonth = YearMonth.from(refDate);

        int daysInMonth = Math.max(1, MonthUtils.daysInMonth(targetMonth));
        int daysElapsed;
        if (targetMonth.isBefore(refMonth)) {
            daysElapsed = daysInMonth;
        } else if (targetMonth.equals(refMonth)) {
            daysElapsed = Math.min(Math.max(refDate.getDayOfMonth(), 1), daysInMonth);
        } else {
            daysElapsed = 1;
        }
        if (daysElapsed <= 0) {
            daysElapsed = 1;
        }

        BigDecimal paceRatio = BigDecimal.valueOf(daysElapsed)
                .divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
        return new PaceContext(daysElapsed, daysInMonth, paceRatio);
    }

    private BigDecimal resolvePoolAmount(YearMonth month, HabitAllowanceMode mode, BigDecimal dynamicPool) {
        if (mode == HabitAllowanceMode.LOCKED && budgetStore instanceof FullDataStore fullDataStore) {
            String key = lockedPoolKey(month);
            BigDecimal parsed = parseNonNegativeSetting(fullDataStore.getAppSetting(key));
            if (parsed != null) {
                return parsed;
            }
            fullDataStore.saveAppSetting(key, dynamicPool.toPlainString());
            return dynamicPool;
        }
        return dynamicPool;
    }

    private boolean detectPoolAdjustedDown(YearMonth month, HabitAllowanceMode mode, BigDecimal poolAmount) {
        if (mode != HabitAllowanceMode.DYNAMIC) {
            lastDynamicPoolByMonth.put(month, poolAmount);
            return false;
        }

        BigDecimal previous = lastDynamicPoolByMonth.put(month, poolAmount);
        return previous != null && poolAmount.compareTo(previous) < 0;
    }

    private String lockedPoolKey(YearMonth month) {
        return LOCKED_POOL_KEY_PREFIX + month;
    }

    private BigDecimal parseNonNegativeSetting(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = MoneyUtils.normalize(new BigDecimal(raw.trim()));
            return parsed.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2) : parsed;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isEffectiveEnabled(HabitRule rule) {
        return rule != null && rule.isActive() && rule.isEnabledThisMonth();
    }

    private boolean matchesRule(ExpenseEntry expense, HabitRule rule) {
        boolean tagMatch = false;
        if (expense.getTag() != null && !expense.getTag().isBlank() && rule.getTag() != null && !rule.getTag().isBlank()) {
            tagMatch = normalizeTag(expense.getTag()).equalsIgnoreCase(normalizeTag(rule.getTag()));
        }
        boolean categoryMatch = rule.getLinkedCategory() != null && expense.getCategory() == rule.getLinkedCategory();
        return tagMatch || categoryMatch;
    }

    private PlannerBucket resolvePlannerBucket(ExpenseEntry expense) {
        if (expense == null) {
            return PlannerBucket.DISCRETIONARY;
        }
        if (expense.getPlannerBucket() != null) {
            return expense.getPlannerBucket();
        }
        return PlannerBucket.inferFromCategory(expense.getCategory());
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

    private record PaceContext(int daysElapsed, int daysInMonth, BigDecimal paceRatio) {
    }
}
