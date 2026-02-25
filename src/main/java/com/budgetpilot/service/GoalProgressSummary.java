package com.budgetpilot.service;

import com.budgetpilot.model.enums.GoalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class GoalProgressSummary {
    private final String goalId;
    private final String goalName;
    private final GoalType goalType;
    private final BigDecimal currentAmount;
    private final BigDecimal targetAmount;
    private final BigDecimal remainingAmount;
    private final BigDecimal progressPercent;
    private final BigDecimal monthlyContributions;
    private final BigDecimal monthlyWithdrawals;
    private final BigDecimal monthlyNetChange;
    private final String estimatedCompletionText;
    private final int priority;
    private final boolean active;
    private final LocalDate targetDate;

    public GoalProgressSummary(
            String goalId,
            String goalName,
            GoalType goalType,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            BigDecimal remainingAmount,
            BigDecimal progressPercent,
            BigDecimal monthlyContributions,
            BigDecimal monthlyWithdrawals,
            BigDecimal monthlyNetChange,
            String estimatedCompletionText,
            int priority,
            boolean active,
            LocalDate targetDate
    ) {
        this.goalId = Objects.requireNonNull(goalId, "goalId");
        this.goalName = Objects.requireNonNull(goalName, "goalName");
        this.goalType = Objects.requireNonNull(goalType, "goalType");
        this.currentAmount = Objects.requireNonNull(currentAmount, "currentAmount");
        this.targetAmount = Objects.requireNonNull(targetAmount, "targetAmount");
        this.remainingAmount = Objects.requireNonNull(remainingAmount, "remainingAmount");
        this.progressPercent = Objects.requireNonNull(progressPercent, "progressPercent");
        this.monthlyContributions = Objects.requireNonNull(monthlyContributions, "monthlyContributions");
        this.monthlyWithdrawals = Objects.requireNonNull(monthlyWithdrawals, "monthlyWithdrawals");
        this.monthlyNetChange = Objects.requireNonNull(monthlyNetChange, "monthlyNetChange");
        this.estimatedCompletionText = Objects.requireNonNull(estimatedCompletionText, "estimatedCompletionText");
        this.priority = priority;
        this.active = active;
        this.targetDate = targetDate;
    }

    public String getGoalId() {
        return goalId;
    }

    public String getGoalName() {
        return goalName;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
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

    public String getEstimatedCompletionText() {
        return estimatedCompletionText;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }
}
