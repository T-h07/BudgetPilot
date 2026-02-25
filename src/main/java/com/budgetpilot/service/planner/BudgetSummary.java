package com.budgetpilot.service.planner;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public class BudgetSummary {
    private final YearMonth month;
    private final BigDecimal plannedIncome;
    private final BigDecimal receivedIncome;
    private final BigDecimal fixedCostsBudget;
    private final BigDecimal essentialsBudget;
    private final BigDecimal discretionaryBudget;
    private final BigDecimal savingsPercent;
    private final BigDecimal goalsPercent;
    private final BigDecimal savingsAmountPlanned;
    private final BigDecimal goalsAmountPlanned;
    private final BigDecimal safetyBufferAmount;
    private final BigDecimal totalPlannedUsage;
    private final BigDecimal remainingPlanned;
    private final boolean overallocated;
    private final String statusMessage;

    public BudgetSummary(
            YearMonth month,
            BigDecimal plannedIncome,
            BigDecimal receivedIncome,
            BigDecimal fixedCostsBudget,
            BigDecimal essentialsBudget,
            BigDecimal discretionaryBudget,
            BigDecimal savingsPercent,
            BigDecimal goalsPercent,
            BigDecimal savingsAmountPlanned,
            BigDecimal goalsAmountPlanned,
            BigDecimal safetyBufferAmount,
            BigDecimal totalPlannedUsage,
            BigDecimal remainingPlanned,
            boolean overallocated,
            String statusMessage
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.plannedIncome = Objects.requireNonNull(plannedIncome, "plannedIncome");
        this.receivedIncome = Objects.requireNonNull(receivedIncome, "receivedIncome");
        this.fixedCostsBudget = Objects.requireNonNull(fixedCostsBudget, "fixedCostsBudget");
        this.essentialsBudget = Objects.requireNonNull(essentialsBudget, "essentialsBudget");
        this.discretionaryBudget = Objects.requireNonNull(discretionaryBudget, "discretionaryBudget");
        this.savingsPercent = Objects.requireNonNull(savingsPercent, "savingsPercent");
        this.goalsPercent = Objects.requireNonNull(goalsPercent, "goalsPercent");
        this.savingsAmountPlanned = Objects.requireNonNull(savingsAmountPlanned, "savingsAmountPlanned");
        this.goalsAmountPlanned = Objects.requireNonNull(goalsAmountPlanned, "goalsAmountPlanned");
        this.safetyBufferAmount = Objects.requireNonNull(safetyBufferAmount, "safetyBufferAmount");
        this.totalPlannedUsage = Objects.requireNonNull(totalPlannedUsage, "totalPlannedUsage");
        this.remainingPlanned = Objects.requireNonNull(remainingPlanned, "remainingPlanned");
        this.overallocated = overallocated;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getPlannedIncome() {
        return plannedIncome;
    }

    public BigDecimal getReceivedIncome() {
        return receivedIncome;
    }

    public BigDecimal getFixedCostsBudget() {
        return fixedCostsBudget;
    }

    public BigDecimal getEssentialsBudget() {
        return essentialsBudget;
    }

    public BigDecimal getDiscretionaryBudget() {
        return discretionaryBudget;
    }

    public BigDecimal getSavingsPercent() {
        return savingsPercent;
    }

    public BigDecimal getGoalsPercent() {
        return goalsPercent;
    }

    public BigDecimal getSavingsAmountPlanned() {
        return savingsAmountPlanned;
    }

    public BigDecimal getGoalsAmountPlanned() {
        return goalsAmountPlanned;
    }

    public BigDecimal getSafetyBufferAmount() {
        return safetyBufferAmount;
    }

    public BigDecimal getTotalPlannedUsage() {
        return totalPlannedUsage;
    }

    public BigDecimal getRemainingPlanned() {
        return remainingPlanned;
    }

    public boolean isOverallocated() {
        return overallocated;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
