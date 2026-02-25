package com.budgetpilot.service;

import com.budgetpilot.model.enums.InvestmentKind;
import com.budgetpilot.model.enums.InvestmentStatus;
import com.budgetpilot.model.enums.InvestmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class InvestmentPositionSummary {
    private final String investmentId;
    private final String name;
    private final InvestmentType type;
    private final InvestmentKind kind;
    private final InvestmentStatus status;
    private final BigDecimal targetAmount;
    private final BigDecimal investedAmountTotal;
    private final BigDecimal returnsTotal;
    private final BigDecimal feesTotal;
    private final BigDecimal withdrawalsTotal;
    private final BigDecimal currentEstimatedValue;
    private final BigDecimal netProfitAmount;
    private final BigDecimal roiPercent;
    private final BigDecimal progressPercent;
    private final BigDecimal expectedProfitAmount;
    private final LocalDate expectedReturnDate;
    private final String daysRemainingText;
    private final BigDecimal monthlyContributionTotal;
    private final BigDecimal monthlyReturnTotal;
    private final int transactionCount;
    private final boolean active;

    public InvestmentPositionSummary(
            String investmentId,
            String name,
            InvestmentType type,
            InvestmentKind kind,
            InvestmentStatus status,
            BigDecimal targetAmount,
            BigDecimal investedAmountTotal,
            BigDecimal returnsTotal,
            BigDecimal feesTotal,
            BigDecimal withdrawalsTotal,
            BigDecimal currentEstimatedValue,
            BigDecimal netProfitAmount,
            BigDecimal roiPercent,
            BigDecimal progressPercent,
            BigDecimal expectedProfitAmount,
            LocalDate expectedReturnDate,
            String daysRemainingText,
            BigDecimal monthlyContributionTotal,
            BigDecimal monthlyReturnTotal,
            int transactionCount,
            boolean active
    ) {
        this.investmentId = Objects.requireNonNull(investmentId, "investmentId");
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.status = Objects.requireNonNull(status, "status");
        this.targetAmount = targetAmount;
        this.investedAmountTotal = Objects.requireNonNull(investedAmountTotal, "investedAmountTotal");
        this.returnsTotal = Objects.requireNonNull(returnsTotal, "returnsTotal");
        this.feesTotal = Objects.requireNonNull(feesTotal, "feesTotal");
        this.withdrawalsTotal = Objects.requireNonNull(withdrawalsTotal, "withdrawalsTotal");
        this.currentEstimatedValue = Objects.requireNonNull(currentEstimatedValue, "currentEstimatedValue");
        this.netProfitAmount = Objects.requireNonNull(netProfitAmount, "netProfitAmount");
        this.roiPercent = Objects.requireNonNull(roiPercent, "roiPercent");
        this.progressPercent = Objects.requireNonNull(progressPercent, "progressPercent");
        this.expectedProfitAmount = expectedProfitAmount;
        this.expectedReturnDate = expectedReturnDate;
        this.daysRemainingText = Objects.requireNonNull(daysRemainingText, "daysRemainingText");
        this.monthlyContributionTotal = Objects.requireNonNull(monthlyContributionTotal, "monthlyContributionTotal");
        this.monthlyReturnTotal = Objects.requireNonNull(monthlyReturnTotal, "monthlyReturnTotal");
        this.transactionCount = transactionCount;
        this.active = active;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public String getName() {
        return name;
    }

    public InvestmentType getType() {
        return type;
    }

    public InvestmentKind getKind() {
        return kind;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getInvestedAmountTotal() {
        return investedAmountTotal;
    }

    public BigDecimal getReturnsTotal() {
        return returnsTotal;
    }

    public BigDecimal getFeesTotal() {
        return feesTotal;
    }

    public BigDecimal getWithdrawalsTotal() {
        return withdrawalsTotal;
    }

    public BigDecimal getCurrentEstimatedValue() {
        return currentEstimatedValue;
    }

    public BigDecimal getNetProfitAmount() {
        return netProfitAmount;
    }

    public BigDecimal getRoiPercent() {
        return roiPercent;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public BigDecimal getExpectedProfitAmount() {
        return expectedProfitAmount;
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public String getDaysRemainingText() {
        return daysRemainingText;
    }

    public BigDecimal getMonthlyContributionTotal() {
        return monthlyContributionTotal;
    }

    public BigDecimal getMonthlyReturnTotal() {
        return monthlyReturnTotal;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public boolean isActive() {
        return active;
    }
}
