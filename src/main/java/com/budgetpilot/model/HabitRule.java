package com.budgetpilot.model;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class HabitRule {
    private String id;
    private String tag;
    private String displayName;
    private ExpenseCategory linkedCategory;
    private BigDecimal monthlyLimit;
    private BigDecimal warningThresholdPercent;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HabitRule() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.tag = "#rule";
        this.displayName = "Spending Rule";
        this.linkedCategory = null;
        this.monthlyLimit = BigDecimal.ZERO.setScale(2);
        this.warningThresholdPercent = new BigDecimal("70.00");
        this.active = true;
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public HabitRule(String tag, String displayName, BigDecimal monthlyLimit, BigDecimal warningThresholdPercent) {
        this();
        setTag(tag);
        setDisplayName(displayName);
        setMonthlyLimit(monthlyLimit);
        setWarningThresholdPercent(warningThresholdPercent);
    }

    public HabitRule(HabitRule other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.tag = other.tag;
        this.displayName = other.displayName;
        this.linkedCategory = other.linkedCategory;
        this.monthlyLimit = other.monthlyLimit;
        this.warningThresholdPercent = other.warningThresholdPercent;
        this.active = other.active;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public HabitRule copy() {
        return new HabitRule(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        String normalizedTag = ValidationUtils.requireNonBlank(tag, "tag");
        if (!normalizedTag.startsWith("#")) {
            throw new IllegalArgumentException("tag must start with #");
        }
        this.tag = normalizedTag;
        touch();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = ValidationUtils.requireNonBlank(displayName, "displayName");
        touch();
    }

    public ExpenseCategory getLinkedCategory() {
        return linkedCategory;
    }

    public void setLinkedCategory(ExpenseCategory linkedCategory) {
        this.linkedCategory = linkedCategory;
        touch();
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = ValidationUtils.requireNonNegative(monthlyLimit, "monthlyLimit");
        touch();
    }

    public BigDecimal getWarningThresholdPercent() {
        return warningThresholdPercent;
    }

    public void setWarningThresholdPercent(BigDecimal warningThresholdPercent) {
        this.warningThresholdPercent = ValidationUtils.requirePercent(
                warningThresholdPercent,
                "warningThresholdPercent"
        );
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
        return "HabitRule{" +
                "id='" + id + '\'' +
                ", tag='" + tag + '\'' +
                ", displayName='" + displayName + '\'' +
                ", monthlyLimit=" + monthlyLimit +
                ", warningThresholdPercent=" + warningThresholdPercent +
                ", active=" + active +
                '}';
    }
}
