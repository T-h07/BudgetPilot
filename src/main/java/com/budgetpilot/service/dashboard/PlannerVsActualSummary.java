package com.budgetpilot.service.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

public class PlannerVsActualSummary {
    private final BigDecimal plannedIncome;
    private final BigDecimal totalSpentActual;
    private final BigDecimal projectedMonthEndSpend;
    private final BigDecimal plannedExpenseBudget;
    private final BigDecimal plannedSavingsAmount;
    private final BigDecimal plannedGoalsAmount;
    private final BigDecimal plannerRemainingPlanned;
    private final BigDecimal projectedRemainingAfterPlan;

    public PlannerVsActualSummary(
            BigDecimal plannedIncome,
            BigDecimal totalSpentActual,
            BigDecimal projectedMonthEndSpend,
            BigDecimal plannedExpenseBudget,
            BigDecimal plannedSavingsAmount,
            BigDecimal plannedGoalsAmount,
            BigDecimal plannerRemainingPlanned,
            BigDecimal projectedRemainingAfterPlan
    ) {
        this.plannedIncome = Objects.requireNonNull(plannedIncome, "plannedIncome");
        this.totalSpentActual = Objects.requireNonNull(totalSpentActual, "totalSpentActual");
        this.projectedMonthEndSpend = Objects.requireNonNull(projectedMonthEndSpend, "projectedMonthEndSpend");
        this.plannedExpenseBudget = Objects.requireNonNull(plannedExpenseBudget, "plannedExpenseBudget");
        this.plannedSavingsAmount = Objects.requireNonNull(plannedSavingsAmount, "plannedSavingsAmount");
        this.plannedGoalsAmount = Objects.requireNonNull(plannedGoalsAmount, "plannedGoalsAmount");
        this.plannerRemainingPlanned = Objects.requireNonNull(plannerRemainingPlanned, "plannerRemainingPlanned");
        this.projectedRemainingAfterPlan = Objects.requireNonNull(projectedRemainingAfterPlan, "projectedRemainingAfterPlan");
    }

    public BigDecimal getPlannedIncome() {
        return plannedIncome;
    }

    public BigDecimal getTotalSpentActual() {
        return totalSpentActual;
    }

    public BigDecimal getProjectedMonthEndSpend() {
        return projectedMonthEndSpend;
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

    public BigDecimal getPlannerRemainingPlanned() {
        return plannerRemainingPlanned;
    }

    public BigDecimal getProjectedRemainingAfterPlan() {
        return projectedRemainingAfterPlan;
    }
}
