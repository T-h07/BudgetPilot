package com.budgetpilot.service.goals;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public class GoalSummary {
    private final YearMonth month;
    private final BigDecimal totalCurrentAmount;
    private final BigDecimal totalTargetAmount;
    private final BigDecimal totalRemainingAmount;
    private final BigDecimal monthlyContributions;
    private final BigDecimal monthlyWithdrawals;
    private final BigDecimal monthlyNetChange;
    private final int activeGoalCount;
    private final int completedGoalCount;
    private final BigDecimal overallProgressPercent;

    public GoalSummary(
            YearMonth month,
            BigDecimal totalCurrentAmount,
            BigDecimal totalTargetAmount,
            BigDecimal totalRemainingAmount,
            BigDecimal monthlyContributions,
            BigDecimal monthlyWithdrawals,
            BigDecimal monthlyNetChange,
            int activeGoalCount,
            int completedGoalCount,
            BigDecimal overallProgressPercent
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.totalCurrentAmount = Objects.requireNonNull(totalCurrentAmount, "totalCurrentAmount");
        this.totalTargetAmount = Objects.requireNonNull(totalTargetAmount, "totalTargetAmount");
        this.totalRemainingAmount = Objects.requireNonNull(totalRemainingAmount, "totalRemainingAmount");
        this.monthlyContributions = Objects.requireNonNull(monthlyContributions, "monthlyContributions");
        this.monthlyWithdrawals = Objects.requireNonNull(monthlyWithdrawals, "monthlyWithdrawals");
        this.monthlyNetChange = Objects.requireNonNull(monthlyNetChange, "monthlyNetChange");
        this.activeGoalCount = activeGoalCount;
        this.completedGoalCount = completedGoalCount;
        this.overallProgressPercent = Objects.requireNonNull(overallProgressPercent, "overallProgressPercent");
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalCurrentAmount() {
        return totalCurrentAmount;
    }

    public BigDecimal getTotalTargetAmount() {
        return totalTargetAmount;
    }

    public BigDecimal getTotalRemainingAmount() {
        return totalRemainingAmount;
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

    public int getActiveGoalCount() {
        return activeGoalCount;
    }

    public int getCompletedGoalCount() {
        return completedGoalCount;
    }

    public BigDecimal getOverallProgressPercent() {
        return overallProgressPercent;
    }
}
