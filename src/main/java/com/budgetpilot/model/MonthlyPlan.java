package com.budgetpilot.model;

import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class MonthlyPlan {
    private String id;
    private YearMonth month;
    private BigDecimal fixedCostsBudget;
    private BigDecimal foodBudget;
    private BigDecimal transportBudget;
    private BigDecimal familyBudget;
    private BigDecimal discretionaryBudget;
    private BigDecimal savingsPercent;
    private BigDecimal goalsPercent;
    private BigDecimal safetyBufferAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MonthlyPlan() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.month = MonthUtils.currentMonth();
        this.fixedCostsBudget = MoneyUtils.zeroIfNull(null);
        this.foodBudget = MoneyUtils.zeroIfNull(null);
        this.transportBudget = MoneyUtils.zeroIfNull(null);
        this.familyBudget = MoneyUtils.zeroIfNull(null);
        this.discretionaryBudget = MoneyUtils.zeroIfNull(null);
        this.savingsPercent = MoneyUtils.zeroIfNull(null);
        this.goalsPercent = MoneyUtils.zeroIfNull(null);
        this.safetyBufferAmount = MoneyUtils.zeroIfNull(null);
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public MonthlyPlan(YearMonth month) {
        this();
        setMonth(month);
    }

    public MonthlyPlan(MonthlyPlan other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.month = other.month;
        this.fixedCostsBudget = other.fixedCostsBudget;
        this.foodBudget = other.foodBudget;
        this.transportBudget = other.transportBudget;
        this.familyBudget = other.familyBudget;
        this.discretionaryBudget = other.discretionaryBudget;
        this.savingsPercent = other.savingsPercent;
        this.goalsPercent = other.goalsPercent;
        this.safetyBufferAmount = other.safetyBufferAmount;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public MonthlyPlan copy() {
        return new MonthlyPlan(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        touch();
    }

    public BigDecimal getFixedCostsBudget() {
        return fixedCostsBudget;
    }

    public void setFixedCostsBudget(BigDecimal fixedCostsBudget) {
        this.fixedCostsBudget = ValidationUtils.requireNonNegative(fixedCostsBudget, "fixedCostsBudget");
        touch();
    }

    public BigDecimal getFoodBudget() {
        return foodBudget;
    }

    public void setFoodBudget(BigDecimal foodBudget) {
        this.foodBudget = ValidationUtils.requireNonNegative(foodBudget, "foodBudget");
        touch();
    }

    public BigDecimal getTransportBudget() {
        return transportBudget;
    }

    public void setTransportBudget(BigDecimal transportBudget) {
        this.transportBudget = ValidationUtils.requireNonNegative(transportBudget, "transportBudget");
        touch();
    }

    public BigDecimal getFamilyBudget() {
        return familyBudget;
    }

    public void setFamilyBudget(BigDecimal familyBudget) {
        this.familyBudget = ValidationUtils.requireNonNegative(familyBudget, "familyBudget");
        touch();
    }

    public BigDecimal getDiscretionaryBudget() {
        return discretionaryBudget;
    }

    public void setDiscretionaryBudget(BigDecimal discretionaryBudget) {
        this.discretionaryBudget = ValidationUtils.requireNonNegative(discretionaryBudget, "discretionaryBudget");
        touch();
    }

    public BigDecimal getSavingsPercent() {
        return savingsPercent;
    }

    public void setSavingsPercent(BigDecimal savingsPercent) {
        this.savingsPercent = ValidationUtils.requirePercent(savingsPercent, "savingsPercent");
        touch();
    }

    public BigDecimal getGoalsPercent() {
        return goalsPercent;
    }

    public void setGoalsPercent(BigDecimal goalsPercent) {
        this.goalsPercent = ValidationUtils.requirePercent(goalsPercent, "goalsPercent");
        touch();
    }

    public BigDecimal getSafetyBufferAmount() {
        return safetyBufferAmount;
    }

    public void setSafetyBufferAmount(BigDecimal safetyBufferAmount) {
        this.safetyBufferAmount = ValidationUtils.requireNonNegative(safetyBufferAmount, "safetyBufferAmount");
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
        return "MonthlyPlan{" +
                "id='" + id + '\'' +
                ", month=" + month +
                ", fixedCostsBudget=" + fixedCostsBudget +
                ", discretionaryBudget=" + discretionaryBudget +
                ", savingsPercent=" + savingsPercent +
                ", goalsPercent=" + goalsPercent +
                '}';
    }
}
