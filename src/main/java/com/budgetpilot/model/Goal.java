package com.budgetpilot.model;

import com.budgetpilot.model.enums.GoalType;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Goal {
    private String id;
    private String name;
    private GoalType goalType;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private int priority;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Goal() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.name = "Goal";
        this.goalType = GoalType.CUSTOM;
        this.targetAmount = BigDecimal.ZERO.setScale(2);
        this.currentAmount = BigDecimal.ZERO.setScale(2);
        this.targetDate = null;
        this.priority = 3;
        this.active = true;
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Goal(String name, GoalType goalType, BigDecimal targetAmount, BigDecimal currentAmount, int priority) {
        this();
        setName(name);
        setGoalType(goalType);
        setTargetAmount(targetAmount);
        setCurrentAmount(currentAmount);
        setPriority(priority);
    }

    public Goal(Goal other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.name = other.name;
        this.goalType = other.goalType;
        this.targetAmount = other.targetAmount;
        this.currentAmount = other.currentAmount;
        this.targetDate = other.targetDate;
        this.priority = other.priority;
        this.active = other.active;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public Goal copy() {
        return new Goal(this);
    }

    public BigDecimal getProgressPercent() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progress = currentAmount
                .divide(targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        if (progress.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (progress.compareTo(MoneyUtils.HUNDRED) > 0) {
            return MoneyUtils.HUNDRED;
        }
        return progress;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtils.requireNonBlank(name, "name");
        touch();
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = ValidationUtils.requireNonNull(goalType, "goalType");
        touch();
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = ValidationUtils.requireNonNegative(targetAmount, "targetAmount");
        touch();
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = ValidationUtils.requireNonNegative(currentAmount, "currentAmount");
        touch();
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
        touch();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("priority must be between 1 and 5");
        }
        this.priority = priority;
        touch();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        touch();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = ValidationUtils.requireNonNull(createdAt, "createdAt");
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = ValidationUtils.requireNonNull(updatedAt, "updatedAt");
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Goal{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", goalType=" + goalType +
                ", targetAmount=" + targetAmount +
                ", currentAmount=" + currentAmount +
                ", priority=" + priority +
                ", active=" + active +
                '}';
    }
}
