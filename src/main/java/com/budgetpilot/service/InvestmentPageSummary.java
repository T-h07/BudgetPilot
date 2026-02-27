package com.budgetpilot.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class InvestmentPageSummary {
    private final YearMonth month;
    private final BigDecimal totalInvestedAllTime;
    private final BigDecimal totalEstimatedValue;
    private final BigDecimal totalNetProfit;
    private final BigDecimal totalMonthlyContributions;
    private final BigDecimal totalMonthlyReturns;
    private final int activeInvestmentCount;
    private final int completedInvestmentCount;
    private final BigDecimal avgRoiPercent;
    private final List<InvestmentPositionSummary> positions;

    public InvestmentPageSummary(
            YearMonth month,
            BigDecimal totalInvestedAllTime,
            BigDecimal totalEstimatedValue,
            BigDecimal totalNetProfit,
            BigDecimal totalMonthlyContributions,
            BigDecimal totalMonthlyReturns,
            int activeInvestmentCount,
            int completedInvestmentCount,
            BigDecimal avgRoiPercent,
            List<InvestmentPositionSummary> positions
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.totalInvestedAllTime = Objects.requireNonNull(totalInvestedAllTime, "totalInvestedAllTime");
        this.totalEstimatedValue = Objects.requireNonNull(totalEstimatedValue, "totalEstimatedValue");
        this.totalNetProfit = Objects.requireNonNull(totalNetProfit, "totalNetProfit");
        this.totalMonthlyContributions = Objects.requireNonNull(totalMonthlyContributions, "totalMonthlyContributions");
        this.totalMonthlyReturns = Objects.requireNonNull(totalMonthlyReturns, "totalMonthlyReturns");
        this.activeInvestmentCount = activeInvestmentCount;
        this.completedInvestmentCount = completedInvestmentCount;
        this.avgRoiPercent = Objects.requireNonNull(avgRoiPercent, "avgRoiPercent");
        this.positions = List.copyOf(positions == null ? List.of() : positions);
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalInvestedAllTime() {
        return totalInvestedAllTime;
    }

    public BigDecimal getTotalEstimatedValue() {
        return totalEstimatedValue;
    }

    public BigDecimal getTotalNetProfit() {
        return totalNetProfit;
    }

    public BigDecimal getTotalMonthlyContributions() {
        return totalMonthlyContributions;
    }

    public BigDecimal getTotalMonthlyReturns() {
        return totalMonthlyReturns;
    }

    public int getActiveInvestmentCount() {
        return activeInvestmentCount;
    }

    public int getCompletedInvestmentCount() {
        return completedInvestmentCount;
    }

    public BigDecimal getAvgRoiPercent() {
        return avgRoiPercent;
    }

    public List<InvestmentPositionSummary> getPositions() {
        return positions;
    }
}
