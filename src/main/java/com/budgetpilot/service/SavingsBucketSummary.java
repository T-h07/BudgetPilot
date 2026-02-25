package com.budgetpilot.service;

import java.math.BigDecimal;
import java.util.Objects;

public class SavingsBucketSummary {
    private final String bucketId;
    private final String bucketName;
    private final BigDecimal currentAmount;
    private final BigDecimal targetAmount;
    private final BigDecimal progressPercent;
    private final BigDecimal monthlyContributions;
    private final BigDecimal monthlyWithdrawals;
    private final BigDecimal monthlyNetChange;
    private final int entryCount;
    private final boolean active;

    public SavingsBucketSummary(
            String bucketId,
            String bucketName,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            BigDecimal progressPercent,
            BigDecimal monthlyContributions,
            BigDecimal monthlyWithdrawals,
            BigDecimal monthlyNetChange,
            int entryCount,
            boolean active
    ) {
        this.bucketId = Objects.requireNonNull(bucketId, "bucketId");
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName");
        this.currentAmount = Objects.requireNonNull(currentAmount, "currentAmount");
        this.targetAmount = targetAmount;
        this.progressPercent = Objects.requireNonNull(progressPercent, "progressPercent");
        this.monthlyContributions = Objects.requireNonNull(monthlyContributions, "monthlyContributions");
        this.monthlyWithdrawals = Objects.requireNonNull(monthlyWithdrawals, "monthlyWithdrawals");
        this.monthlyNetChange = Objects.requireNonNull(monthlyNetChange, "monthlyNetChange");
        this.entryCount = entryCount;
        this.active = active;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
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

    public int getEntryCount() {
        return entryCount;
    }

    public boolean isActive() {
        return active;
    }
}
