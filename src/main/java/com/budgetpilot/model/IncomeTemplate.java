package com.budgetpilot.model;

import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.RecurrenceCadence;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class IncomeTemplate {
    private String id;
    private String sourceName;
    private IncomeType incomeType;
    private BigDecimal defaultAmount;
    private RecurrenceCadence cadence;
    private int dayOfMonth;
    private boolean active;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IncomeTemplate() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.sourceName = "Income";
        this.incomeType = IncomeType.OTHER;
        this.defaultAmount = BigDecimal.ZERO.setScale(2);
        this.cadence = RecurrenceCadence.MONTHLY;
        this.dayOfMonth = 1;
        this.active = true;
        this.note = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public IncomeTemplate(IncomeTemplate other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.sourceName = other.sourceName;
        this.incomeType = other.incomeType;
        this.defaultAmount = other.defaultAmount;
        this.cadence = other.cadence;
        this.dayOfMonth = other.dayOfMonth;
        this.active = other.active;
        this.note = other.note;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public IncomeTemplate copy() {
        return new IncomeTemplate(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = ValidationUtils.requireNonBlank(sourceName, "sourceName");
        touch();
    }

    public IncomeType getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(IncomeType incomeType) {
        this.incomeType = ValidationUtils.requireNonNull(incomeType, "incomeType");
        touch();
    }

    public BigDecimal getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(BigDecimal defaultAmount) {
        this.defaultAmount = ValidationUtils.requireNonNegative(defaultAmount, "defaultAmount");
        touch();
    }

    public RecurrenceCadence getCadence() {
        return cadence;
    }

    public void setCadence(RecurrenceCadence cadence) {
        this.cadence = ValidationUtils.requireNonNull(cadence, "cadence");
        touch();
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("dayOfMonth must be between 1 and 31");
        }
        this.dayOfMonth = dayOfMonth;
        touch();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        touch();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
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
        return "IncomeTemplate{" +
                "id='" + id + '\'' +
                ", sourceName='" + sourceName + '\'' +
                ", incomeType=" + incomeType +
                ", defaultAmount=" + MoneyUtils.normalize(defaultAmount) +
                ", cadence=" + cadence +
                ", dayOfMonth=" + dayOfMonth +
                ", active=" + active +
                '}';
    }
}
