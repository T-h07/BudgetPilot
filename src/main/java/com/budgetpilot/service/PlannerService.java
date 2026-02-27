package com.budgetpilot.service;

import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlannerService {
    private static final DateTimeFormatter WEEK_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());

    private final BudgetStore budgetStore;
    private final IncomeService incomeService;

    public PlannerService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.incomeService = new IncomeService(budgetStore);
    }

    public MonthlyPlan getOrCreateMonthlyPlan(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        MonthlyPlan existing = budgetStore.getMonthlyPlan(targetMonth);
        return existing == null ? new MonthlyPlan(targetMonth) : existing;
    }

    public void saveMonthlyPlan(MonthlyPlan plan, boolean familyModuleEnabled) {
        PlannerValidationResult validation = validatePlan(plan);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        MonthlyPlan copy = plan.copy();
        if (!familyModuleEnabled) {
            copy.setFamilyBudget(BigDecimal.ZERO);
        }
        budgetStore.saveMonthlyPlan(copy);
    }

    public PlannerValidationResult validatePlan(MonthlyPlan plan) {
        PlannerValidationResult result = new PlannerValidationResult();
        if (plan == null) {
            result.addError("Monthly plan is required.");
            return result;
        }

        if (plan.getMonth() == null) {
            result.addError("Plan month is required.");
        }

        validateNonNegative(result, plan.getFixedCostsBudget(), "Fixed costs budget");
        validateNonNegative(result, plan.getFoodBudget(), "Food budget");
        validateNonNegative(result, plan.getTransportBudget(), "Transport budget");
        validateNonNegative(result, plan.getFamilyBudget(), "Family budget");
        validateNonNegative(result, plan.getDiscretionaryBudget(), "Discretionary budget");
        validateNonNegative(result, plan.getSafetyBufferAmount(), "Safety buffer amount");
        validatePercent(result, plan.getSavingsPercent(), "Savings percent");
        validatePercent(result, plan.getGoalsPercent(), "Goals percent");
        return result;
    }

    public BudgetSummary buildBudgetSummary(YearMonth month, boolean familyModuleEnabled) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        MonthlyPlan plan = getOrCreateMonthlyPlan(targetMonth);

        BigDecimal plannedIncome = incomeService.getPlannedIncomeTotal(targetMonth);
        BigDecimal receivedIncome = incomeService.getReceivedIncomeTotal(targetMonth);

        BigDecimal fixed = MoneyUtils.zeroIfNull(plan.getFixedCostsBudget());
        BigDecimal food = MoneyUtils.zeroIfNull(plan.getFoodBudget());
        BigDecimal transport = MoneyUtils.zeroIfNull(plan.getTransportBudget());
        BigDecimal family = familyModuleEnabled ? MoneyUtils.zeroIfNull(plan.getFamilyBudget()) : BigDecimal.ZERO.setScale(2);
        BigDecimal discretionary = MoneyUtils.zeroIfNull(plan.getDiscretionaryBudget());
        BigDecimal savingsPct = MoneyUtils.zeroIfNull(plan.getSavingsPercent());
        BigDecimal goalsPct = MoneyUtils.zeroIfNull(plan.getGoalsPercent());
        BigDecimal safety = MoneyUtils.zeroIfNull(plan.getSafetyBufferAmount());

        BigDecimal essentials = MoneyUtils.safeAdd(MoneyUtils.safeAdd(food, transport), family);
        BigDecimal savingsAmount = MoneyUtils.percentOf(plannedIncome, savingsPct);
        BigDecimal goalsAmount = MoneyUtils.percentOf(plannedIncome, goalsPct);
        BigDecimal totalUsage = fixed
                .add(essentials)
                .add(discretionary)
                .add(savingsAmount)
                .add(goalsAmount)
                .add(safety);
        totalUsage = MoneyUtils.normalize(totalUsage);
        BigDecimal remaining = MoneyUtils.safeSubtract(plannedIncome, totalUsage);
        boolean overallocated = remaining.compareTo(BigDecimal.ZERO) < 0;

        String statusMessage;
        if (plannedIncome.compareTo(BigDecimal.ZERO) <= 0) {
            statusMessage = "No income entered for this month.";
        } else if (overallocated) {
            statusMessage = "Overallocated by " + MoneyUtils.normalize(remaining.abs()) + ".";
        } else if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            statusMessage = "Plan is fully allocated.";
        } else {
            statusMessage = "Healthy plan.";
        }

        return new BudgetSummary(
                targetMonth,
                plannedIncome,
                receivedIncome,
                fixed,
                essentials,
                discretionary,
                savingsPct,
                goalsPct,
                savingsAmount,
                goalsAmount,
                safety,
                totalUsage,
                remaining,
                overallocated,
                statusMessage
        );
    }

    public WeeklyBudgetBreakdown buildWeeklyBreakdown(YearMonth month, boolean familyModuleEnabled) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        MonthlyPlan plan = getOrCreateMonthlyPlan(targetMonth);
        List<MonthUtils.WeekRange> weekRanges = MonthUtils.calendarWeekRanges(targetMonth);
        int weekCount = Math.max(weekRanges.size(), 1);

        BigDecimal foodPerWeek = splitEqual(plan.getFoodBudget(), weekCount);
        BigDecimal transportPerWeek = splitEqual(plan.getTransportBudget(), weekCount);
        BigDecimal discretionaryPerWeek = splitEqual(plan.getDiscretionaryBudget(), weekCount);
        BigDecimal familyPerWeek = familyModuleEnabled
                ? splitEqual(plan.getFamilyBudget(), weekCount)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal totalPerWeek = MoneyUtils.normalize(
                MoneyUtils.zeroIfNull(plan.getFixedCostsBudget()).divide(BigDecimal.valueOf(weekCount), 2, RoundingMode.HALF_UP)
                        .add(foodPerWeek)
                        .add(transportPerWeek)
                        .add(discretionaryPerWeek)
                        .add(familyPerWeek)
        );

        List<WeekAllocation> allocations = new ArrayList<>();
        for (MonthUtils.WeekRange range : weekRanges) {
            String rangeText = WEEK_DATE_FORMAT.format(range.getStartDate()) + " - " + WEEK_DATE_FORMAT.format(range.getEndDate());
            allocations.add(new WeekAllocation(
                    range.getWeekLabel(),
                    rangeText,
                    totalPerWeek,
                    foodPerWeek,
                    transportPerWeek,
                    discretionaryPerWeek,
                    familyPerWeek
            ));
        }

        return new WeeklyBudgetBreakdown(targetMonth, allocations);
    }

    private BigDecimal splitEqual(BigDecimal amount, int divisor) {
        return MoneyUtils.normalize(MoneyUtils.zeroIfNull(amount).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
    }

    private void validateNonNegative(PlannerValidationResult result, BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            result.addError(fieldName + " must be non-negative.");
        }
    }

    private void validatePercent(PlannerValidationResult result, BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MoneyUtils.HUNDRED) > 0) {
            result.addError(fieldName + " must be between 0 and 100.");
        }
    }
}
