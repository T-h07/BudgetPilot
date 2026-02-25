package com.budgetpilot.model;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SavingsBucket {
    private String id;
    private String name;
    private BigDecimal currentAmount;
    private BigDecimal targetAmount;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SavingsBucket() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.name = "Savings Bucket";
        this.currentAmount = BigDecimal.ZERO.setScale(2);
        this.targetAmount = null;
        this.active = true;
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public SavingsBucket(String name, BigDecimal currentAmount, BigDecimal targetAmount) {
        this();
        setName(name);
        setCurrentAmount(currentAmount);
        setTargetAmount(targetAmount);
    }

    public SavingsBucket(SavingsBucket other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.name = other.name;
        this.currentAmount = other.currentAmount;
        this.targetAmount = other.targetAmount;
        this.active = other.active;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public SavingsBucket copy() {
        return new SavingsBucket(this);
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

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = ValidationUtils.requireNonNegative(currentAmount, "currentAmount");
        touch();
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        if (targetAmount == null) {
            this.targetAmount = null;
        } else {
            this.targetAmount = ValidationUtils.requireNonNegative(targetAmount, "targetAmount");
        }
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
        return "SavingsBucket{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", currentAmount=" + currentAmount +
                ", targetAmount=" + targetAmount +
                ", active=" + active +
                '}';
    }
}
