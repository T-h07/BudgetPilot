package com.budgetpilot.service.savings;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public class SavingsSummary {
    private final YearMonth month;
    private final BigDecimal totalCurrentSavings;
    private final BigDecimal monthlyContributions;
    private final BigDecimal monthlyWithdrawals;
    private final BigDecimal monthlyNetChange;
    private final int activeBucketCount;
    private final int bucketCount;
    private final BigDecimal totalTargetAmount;
    private final BigDecimal targetCoveragePercent;

    public SavingsSummary(
            YearMonth month,
            BigDecimal totalCurrentSavings,
            BigDecimal monthlyContributions,
            BigDecimal monthlyWithdrawals,
            BigDecimal monthlyNetChange,
            int activeBucketCount,
            int bucketCount,
            BigDecimal totalTargetAmount,
            BigDecimal targetCoveragePercent
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.totalCurrentSavings = Objects.requireNonNull(totalCurrentSavings, "totalCurrentSavings");
        this.monthlyContributions = Objects.requireNonNull(monthlyContributions, "monthlyContributions");
        this.monthlyWithdrawals = Objects.requireNonNull(monthlyWithdrawals, "monthlyWithdrawals");
        this.monthlyNetChange = Objects.requireNonNull(monthlyNetChange, "monthlyNetChange");
        this.activeBucketCount = activeBucketCount;
        this.bucketCount = bucketCount;
        this.totalTargetAmount = Objects.requireNonNull(totalTargetAmount, "totalTargetAmount");
        this.targetCoveragePercent = Objects.requireNonNull(targetCoveragePercent, "targetCoveragePercent");
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalCurrentSavings() {
        return totalCurrentSavings;
    }

    public BigDecimal getMonthlyContributions() {
        return monthlyContributions;
    }

    public BigDecimal getMonthlyWithdrawals() {
        return monthlyWithdrawals;
    }

    public BigDecimal getMonthlyNetChange() {
        return monthlyNetChange;
    }

    public int getActiveBucketCount() {
        return activeBucketCount;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public BigDecimal getTotalTargetAmount() {
        return totalTargetAmount;
    }

    public BigDecimal getTargetCoveragePercent() {
        return targetCoveragePercent;
    }
}
