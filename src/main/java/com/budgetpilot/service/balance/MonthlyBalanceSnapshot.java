package com.budgetpilot.service.balance;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MonthlyBalanceSnapshot {
    private final YearMonth month;
    private final BigDecimal plannedIncome;
    private final BigDecimal receivedIncome;
    private final BigDecimal totalExpenses;
    private final BigDecimal netSavingsAllocationsThisMonth;
    private final BigDecimal netGoalAllocationsThisMonth;
    private final BigDecimal netInvestmentAllocationsThisMonth;
    private final BigDecimal availableBeforeAllocations;
    private final BigDecimal availableAfterAllocations;
    private final MonthlyBalanceWarningLevel warningLevel;
    private final String warningMessage;

    public MonthlyBalanceSnapshot(
            YearMonth month,
            BigDecimal plannedIncome,
            BigDecimal receivedIncome,
            BigDecimal totalExpenses,
            BigDecimal netSavingsAllocationsThisMonth,
            BigDecimal netGoalAllocationsThisMonth,
            BigDecimal netInvestmentAllocationsThisMonth,
            BigDecimal availableBeforeAllocations,
            BigDecimal availableAfterAllocations,
            MonthlyBalanceWarningLevel warningLevel,
            String warningMessage
    ) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.plannedIncome = ValidationUtils.requireNonNull(plannedIncome, "plannedIncome");
        this.receivedIncome = ValidationUtils.requireNonNull(receivedIncome, "receivedIncome");
        this.totalExpenses = ValidationUtils.requireNonNull(totalExpenses, "totalExpenses");
        this.netSavingsAllocationsThisMonth = ValidationUtils.requireNonNull(netSavingsAllocationsThisMonth, "netSavingsAllocationsThisMonth");
        this.netGoalAllocationsThisMonth = ValidationUtils.requireNonNull(netGoalAllocationsThisMonth, "netGoalAllocationsThisMonth");
        this.netInvestmentAllocationsThisMonth = ValidationUtils.requireNonNull(netInvestmentAllocationsThisMonth, "netInvestmentAllocationsThisMonth");
        this.availableBeforeAllocations = ValidationUtils.requireNonNull(availableBeforeAllocations, "availableBeforeAllocations");
        this.availableAfterAllocations = ValidationUtils.requireNonNull(availableAfterAllocations, "availableAfterAllocations");
        this.warningLevel = ValidationUtils.requireNonNull(warningLevel, "warningLevel");
        this.warningMessage = warningMessage == null ? "" : warningMessage.trim();
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

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getNetSavingsAllocationsThisMonth() {
        return netSavingsAllocationsThisMonth;
    }

    public BigDecimal getNetSavingsReservedThisMonth() {
        return netSavingsAllocationsThisMonth;
    }

    public BigDecimal getNetGoalAllocationsThisMonth() {
        return netGoalAllocationsThisMonth;
    }

    public BigDecimal getNetGoalsReservedThisMonth() {
        return netGoalAllocationsThisMonth;
    }

    public BigDecimal getNetInvestmentAllocationsThisMonth() {
        return netInvestmentAllocationsThisMonth;
    }

    public BigDecimal getNetInvestmentsReservedThisMonth() {
        return netInvestmentAllocationsThisMonth;
    }

    public BigDecimal getAvailableBeforeAllocations() {
        return availableBeforeAllocations;
    }

    public BigDecimal getAvailableBeforeReserve() {
        return availableBeforeAllocations;
    }

    public BigDecimal getAvailableAfterAllocations() {
        return availableAfterAllocations;
    }

    public BigDecimal getAvailableAfterReserve() {
        return availableAfterAllocations;
    }

    public MonthlyBalanceWarningLevel getWarningLevel() {
        return warningLevel;
    }

    public CashflowStatus getStatus() {
        return CashflowStatus.valueOf(warningLevel.name());
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public String getStatusMessage() {
        return warningMessage;
    }
}
