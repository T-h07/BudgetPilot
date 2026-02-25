package com.budgetpilot.model;

import com.budgetpilot.model.enums.RelationshipType;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FamilyMember {
    private String id;
    private String name;
    private RelationshipType relationshipType;
    private BigDecimal weeklyAllowance;
    private BigDecimal monthlyMedicalBudget;
    private BigDecimal monthlySupportBudget;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FamilyMember() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.name = "Family Member";
        this.relationshipType = RelationshipType.OTHER;
        this.weeklyAllowance = BigDecimal.ZERO.setScale(2);
        this.monthlyMedicalBudget = BigDecimal.ZERO.setScale(2);
        this.monthlySupportBudget = BigDecimal.ZERO.setScale(2);
        this.active = true;
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public FamilyMember(String name, RelationshipType relationshipType) {
        this();
        setName(name);
        setRelationshipType(relationshipType);
    }

    public FamilyMember(FamilyMember other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.name = other.name;
        this.relationshipType = other.relationshipType;
        this.weeklyAllowance = other.weeklyAllowance;
        this.monthlyMedicalBudget = other.monthlyMedicalBudget;
        this.monthlySupportBudget = other.monthlySupportBudget;
        this.active = other.active;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public FamilyMember copy() {
        return new FamilyMember(this);
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

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = ValidationUtils.requireNonNull(relationshipType, "relationshipType");
        touch();
    }

    public BigDecimal getWeeklyAllowance() {
        return weeklyAllowance;
    }

    public void setWeeklyAllowance(BigDecimal weeklyAllowance) {
        this.weeklyAllowance = ValidationUtils.requireNonNegative(weeklyAllowance, "weeklyAllowance");
        touch();
    }

    public BigDecimal getMonthlyMedicalBudget() {
        return monthlyMedicalBudget;
    }

    public void setMonthlyMedicalBudget(BigDecimal monthlyMedicalBudget) {
        this.monthlyMedicalBudget = ValidationUtils.requireNonNegative(monthlyMedicalBudget, "monthlyMedicalBudget");
        touch();
    }

    public BigDecimal getMonthlySupportBudget() {
        return monthlySupportBudget;
    }

    public void setMonthlySupportBudget(BigDecimal monthlySupportBudget) {
        this.monthlySupportBudget = ValidationUtils.requireNonNegative(monthlySupportBudget, "monthlySupportBudget");
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
        return "FamilyMember{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", relationshipType=" + relationshipType +
                ", monthlySupportBudget=" + monthlySupportBudget +
                ", active=" + active +
                '}';
    }
}
