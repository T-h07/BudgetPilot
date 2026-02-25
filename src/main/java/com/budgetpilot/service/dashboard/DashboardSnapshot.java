package com.budgetpilot.service.dashboard;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class DashboardSnapshot {
    private final YearMonth month;
    private final String monthDisplayText;

    private final BigDecimal plannedIncome;
    private final BigDecimal receivedIncome;
    private final BigDecimal totalSpent;
    private final BigDecimal averageDailySpend;
    private final BigDecimal projectedMonthEndSpend;
    private final BigDecimal plannedSavingsAmount;
    private final BigDecimal plannedGoalsAmount;
    private final BigDecimal plannerRemainingPlanned;
    private final BigDecimal projectedRemainingAfterPlan;
    private final int expenseCount;
    private final int incomeCount;
    private final int activeGoalsCount;
    private final int savingsBucketCount;

    private final int budgetHealthScore;
    private final String budgetHealthLabel;
    private final String primaryStatusMessage;

    private final boolean hasMonthlyPlan;
    private final boolean hasIncomeData;
    private final boolean hasExpenseData;
    private final boolean forecastOverspendingRisk;
    private final boolean plannerOverallocated;

    private final List<CategorySpendPoint> categorySpending;
    private final List<WeeklySpendPoint> weeklySpending;
    private final List<DashboardAlert> alerts;
    private final PlannerVsActualSummary plannerVsActual;
    private final List<DashboardKpi> kpis;

    private final BigDecimal goalsCurrentTotal;
    private final BigDecimal goalsTargetTotal;
    private final BigDecimal savingsCurrentTotal;

    public DashboardSnapshot(
            YearMonth month,
            String monthDisplayText,
            BigDecimal plannedIncome,
            BigDecimal receivedIncome,
            BigDecimal totalSpent,
            BigDecimal averageDailySpend,
            BigDecimal projectedMonthEndSpend,
            BigDecimal plannedSavingsAmount,
            BigDecimal plannedGoalsAmount,
            BigDecimal plannerRemainingPlanned,
            BigDecimal projectedRemainingAfterPlan,
            int expenseCount,
            int incomeCount,
            int activeGoalsCount,
            int savingsBucketCount,
            int budgetHealthScore,
            String budgetHealthLabel,
            String primaryStatusMessage,
            boolean hasMonthlyPlan,
            boolean hasIncomeData,
            boolean hasExpenseData,
            boolean forecastOverspendingRisk,
            boolean plannerOverallocated,
            List<CategorySpendPoint> categorySpending,
            List<WeeklySpendPoint> weeklySpending,
            List<DashboardAlert> alerts,
            PlannerVsActualSummary plannerVsActual,
            List<DashboardKpi> kpis,
            BigDecimal goalsCurrentTotal,
            BigDecimal goalsTargetTotal,
            BigDecimal savingsCurrentTotal
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.monthDisplayText = Objects.requireNonNull(monthDisplayText, "monthDisplayText");
        this.plannedIncome = Objects.requireNonNull(plannedIncome, "plannedIncome");
        this.receivedIncome = Objects.requireNonNull(receivedIncome, "receivedIncome");
        this.totalSpent = Objects.requireNonNull(totalSpent, "totalSpent");
        this.averageDailySpend = Objects.requireNonNull(averageDailySpend, "averageDailySpend");
        this.projectedMonthEndSpend = Objects.requireNonNull(projectedMonthEndSpend, "projectedMonthEndSpend");
        this.plannedSavingsAmount = Objects.requireNonNull(plannedSavingsAmount, "plannedSavingsAmount");
        this.plannedGoalsAmount = Objects.requireNonNull(plannedGoalsAmount, "plannedGoalsAmount");
        this.plannerRemainingPlanned = Objects.requireNonNull(plannerRemainingPlanned, "plannerRemainingPlanned");
        this.projectedRemainingAfterPlan = Objects.requireNonNull(projectedRemainingAfterPlan, "projectedRemainingAfterPlan");
        this.expenseCount = expenseCount;
        this.incomeCount = incomeCount;
        this.activeGoalsCount = activeGoalsCount;
        this.savingsBucketCount = savingsBucketCount;
        this.budgetHealthScore = budgetHealthScore;
        this.budgetHealthLabel = Objects.requireNonNull(budgetHealthLabel, "budgetHealthLabel");
        this.primaryStatusMessage = Objects.requireNonNull(primaryStatusMessage, "primaryStatusMessage");
        this.hasMonthlyPlan = hasMonthlyPlan;
        this.hasIncomeData = hasIncomeData;
        this.hasExpenseData = hasExpenseData;
        this.forecastOverspendingRisk = forecastOverspendingRisk;
        this.plannerOverallocated = plannerOverallocated;
        this.categorySpending = List.copyOf(categorySpending == null ? List.of() : categorySpending);
        this.weeklySpending = List.copyOf(weeklySpending == null ? List.of() : weeklySpending);
        this.alerts = List.copyOf(alerts == null ? List.of() : alerts);
        this.plannerVsActual = Objects.requireNonNull(plannerVsActual, "plannerVsActual");
        this.kpis = List.copyOf(kpis == null ? List.of() : kpis);
        this.goalsCurrentTotal = Objects.requireNonNull(goalsCurrentTotal, "goalsCurrentTotal");
        this.goalsTargetTotal = Objects.requireNonNull(goalsTargetTotal, "goalsTargetTotal");
        this.savingsCurrentTotal = Objects.requireNonNull(savingsCurrentTotal, "savingsCurrentTotal");
    }

    public YearMonth getMonth() {
        return month;
    }

    public String getMonthDisplayText() {
        return monthDisplayText;
    }

    public BigDecimal getPlannedIncome() {
        return plannedIncome;
    }

    public BigDecimal getReceivedIncome() {
        return receivedIncome;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public BigDecimal getAverageDailySpend() {
        return averageDailySpend;
    }

    public BigDecimal getProjectedMonthEndSpend() {
        return projectedMonthEndSpend;
    }

    public BigDecimal getPlannedSavingsAmount() {
        return plannedSavingsAmount;
    }

    public BigDecimal getPlannedGoalsAmount() {
        return plannedGoalsAmount;
    }

    public BigDecimal getPlannerRemainingPlanned() {
        return plannerRemainingPlanned;
    }

    public BigDecimal getProjectedRemainingAfterPlan() {
        return projectedRemainingAfterPlan;
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public int getIncomeCount() {
        return incomeCount;
    }

    public int getActiveGoalsCount() {
        return activeGoalsCount;
    }

    public int getSavingsBucketCount() {
        return savingsBucketCount;
    }

    public int getBudgetHealthScore() {
        return budgetHealthScore;
    }

    public String getBudgetHealthLabel() {
        return budgetHealthLabel;
    }

    public String getPrimaryStatusMessage() {
        return primaryStatusMessage;
    }

    public boolean isHasMonthlyPlan() {
        return hasMonthlyPlan;
    }

    public boolean isHasIncomeData() {
        return hasIncomeData;
    }

    public boolean isHasExpenseData() {
        return hasExpenseData;
    }

    public boolean isForecastOverspendingRisk() {
        return forecastOverspendingRisk;
    }

    public boolean isPlannerOverallocated() {
        return plannerOverallocated;
    }

    public List<CategorySpendPoint> getCategorySpending() {
        return categorySpending;
    }

    public List<WeeklySpendPoint> getWeeklySpending() {
        return weeklySpending;
    }

    public List<DashboardAlert> getAlerts() {
        return alerts;
    }

    public PlannerVsActualSummary getPlannerVsActual() {
        return plannerVsActual;
    }

    public List<DashboardKpi> getKpis() {
        return kpis;
    }

    public BigDecimal getGoalsCurrentTotal() {
        return goalsCurrentTotal;
    }

    public BigDecimal getGoalsTargetTotal() {
        return goalsTargetTotal;
    }

    public BigDecimal getSavingsCurrentTotal() {
        return savingsCurrentTotal;
    }
}
