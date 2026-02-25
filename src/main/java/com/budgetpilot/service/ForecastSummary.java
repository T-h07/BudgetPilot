package com.budgetpilot.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public class ForecastSummary {
    private final YearMonth month;
    private final int daysInMonth;
    private final int daysElapsed;
    private final BigDecimal actualSpentSoFar;
    private final BigDecimal averageDailySpend;
    private final BigDecimal projectedExpensesByMonthEnd;
    private final BigDecimal plannedIncome;
    private final BigDecimal plannedExpenseBudget;
    private final BigDecimal plannedSavingsAmount;
    private final BigDecimal plannedGoalsAmount;
    private final BigDecimal safetyBufferAmount;
    private final BigDecimal projectedRemainingAfterExpenses;
    private final BigDecimal projectedRemainingAfterPlan;
    private final boolean hasNoData;
    private final boolean overspendingRisk;
    private final boolean overPlannerBudgetRisk;
    private final boolean planMissing;
    private final String statusMessage;

    public ForecastSummary(
            YearMonth month,
            int daysInMonth,
            int daysElapsed,
            BigDecimal actualSpentSoFar,
            BigDecimal averageDailySpend,
            BigDecimal projectedExpensesByMonthEnd,
            BigDecimal plannedIncome,
            BigDecimal plannedExpenseBudget,
            BigDecimal plannedSavingsAmount,
            BigDecimal plannedGoalsAmount,
            BigDecimal safetyBufferAmount,
            BigDecimal projectedRemainingAfterExpenses,
            BigDecimal projectedRemainingAfterPlan,
            boolean hasNoData,
            boolean overspendingRisk,
            boolean overPlannerBudgetRisk,
            boolean planMissing,
            String statusMessage
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.daysInMonth = daysInMonth;
        this.daysElapsed = daysElapsed;
        this.actualSpentSoFar = Objects.requireNonNull(actualSpentSoFar, "actualSpentSoFar");
        this.averageDailySpend = Objects.requireNonNull(averageDailySpend, "averageDailySpend");
        this.projectedExpensesByMonthEnd = Objects.requireNonNull(projectedExpensesByMonthEnd, "projectedExpensesByMonthEnd");
        this.plannedIncome = Objects.requireNonNull(plannedIncome, "plannedIncome");
        this.plannedExpenseBudget = Objects.requireNonNull(plannedExpenseBudget, "plannedExpenseBudget");
        this.plannedSavingsAmount = Objects.requireNonNull(plannedSavingsAmount, "plannedSavingsAmount");
        this.plannedGoalsAmount = Objects.requireNonNull(plannedGoalsAmount, "plannedGoalsAmount");
        this.safetyBufferAmount = Objects.requireNonNull(safetyBufferAmount, "safetyBufferAmount");
        this.projectedRemainingAfterExpenses = Objects.requireNonNull(projectedRemainingAfterExpenses, "projectedRemainingAfterExpenses");
        this.projectedRemainingAfterPlan = Objects.requireNonNull(projectedRemainingAfterPlan, "projectedRemainingAfterPlan");
        this.hasNoData = hasNoData;
        this.overspendingRisk = overspendingRisk;
        this.overPlannerBudgetRisk = overPlannerBudgetRisk;
        this.planMissing = planMissing;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
    }

    public YearMonth getMonth() {
        return month;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public int getDaysElapsed() {
        return daysElapsed;
    }

    public BigDecimal getActualSpentSoFar() {
        return actualSpentSoFar;
    }

    public BigDecimal getAverageDailySpend() {
        return averageDailySpend;
    }

    public BigDecimal getProjectedExpensesByMonthEnd() {
        return projectedExpensesByMonthEnd;
    }

    public BigDecimal getPlannedIncome() {
        return plannedIncome;
    }

    public BigDecimal getPlannedExpenseBudget() {
        return plannedExpenseBudget;
    }

    public BigDecimal getPlannedSavingsAmount() {
        return plannedSavingsAmount;
    }

    public BigDecimal getPlannedGoalsAmount() {
        return plannedGoalsAmount;
    }

    public BigDecimal getSafetyBufferAmount() {
        return safetyBufferAmount;
    }

    public BigDecimal getProjectedRemainingAfterExpenses() {
        return projectedRemainingAfterExpenses;
    }

    public BigDecimal getProjectedRemainingAfterPlan() {
        return projectedRemainingAfterPlan;
    }

    public boolean isHasNoData() {
        return hasNoData;
    }

    public boolean isOverspendingRisk() {
        return overspendingRisk;
    }

    public boolean isOverPlannerBudgetRisk() {
        return overPlannerBudgetRisk;
    }

    public boolean isPlanMissing() {
        return planMissing;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
