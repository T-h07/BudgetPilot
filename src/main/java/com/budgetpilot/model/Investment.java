package com.budgetpilot.model;

import com.budgetpilot.model.enums.InvestmentKind;
import com.budgetpilot.model.enums.InvestmentStatus;
import com.budgetpilot.model.enums.InvestmentType;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Investment {
    private String id;
    private String name;
    private InvestmentType type;
    private InvestmentKind kind;
    private InvestmentStatus status;
    private BigDecimal targetAmount;
    private BigDecimal currentInvestedAmount;
    private BigDecimal currentEstimatedValue;
    private BigDecimal expectedProfitAmount;
    private BigDecimal expectedProfitPercent;
    private LocalDate startDate;
    private LocalDate expectedReturnDate;
    private int priority;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Investment() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.name = "Investment";
        this.type = InvestmentType.OTHER;
        this.kind = InvestmentKind.MONEY;
        this.status = InvestmentStatus.PLANNED;
        this.targetAmount = null;
        this.currentInvestedAmount = BigDecimal.ZERO.setScale(2);
        this.currentEstimatedValue = BigDecimal.ZERO.setScale(2);
        this.expectedProfitAmount = null;
        this.expectedProfitPercent = null;
        this.startDate = LocalDate.now();
        this.expectedReturnDate = null;
        this.priority = 3;
        this.active = true;
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Investment(
            String name,
            InvestmentType type,
            InvestmentKind kind,
            InvestmentStatus status,
            LocalDate startDate,
            int priority
    ) {
        this();
        setName(name);
        setType(type);
        setKind(kind);
        setStatus(status);
        setStartDate(startDate);
        setPriority(priority);
    }

    public Investment(Investment other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.name = other.name;
        this.type = other.type;
        this.kind = other.kind;
        this.status = other.status;
        this.targetAmount = other.targetAmount;
        this.currentInvestedAmount = other.currentInvestedAmount;
        this.currentEstimatedValue = other.currentEstimatedValue;
        this.expectedProfitAmount = other.expectedProfitAmount;
        this.expectedProfitPercent = other.expectedProfitPercent;
        this.startDate = other.startDate;
        this.expectedReturnDate = other.expectedReturnDate;
        this.priority = other.priority;
        this.active = other.active;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public Investment copy() {
        return new Investment(this);
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

    public InvestmentType getType() {
        return type;
    }

    public void setType(InvestmentType type) {
        this.type = ValidationUtils.requireNonNull(type, "type");
        touch();
    }

    public InvestmentKind getKind() {
        return kind;
    }

    public void setKind(InvestmentKind kind) {
        this.kind = ValidationUtils.requireNonNull(kind, "kind");
        touch();
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(InvestmentStatus status) {
        this.status = ValidationUtils.requireNonNull(status, "status");
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

    public BigDecimal getCurrentInvestedAmount() {
        return currentInvestedAmount;
    }

    public void setCurrentInvestedAmount(BigDecimal currentInvestedAmount) {
        this.currentInvestedAmount = ValidationUtils.requireNonNegative(currentInvestedAmount, "currentInvestedAmount");
        touch();
    }

    public BigDecimal getCurrentEstimatedValue() {
        return currentEstimatedValue;
    }

    public void setCurrentEstimatedValue(BigDecimal currentEstimatedValue) {
        this.currentEstimatedValue = ValidationUtils.requireNonNegative(currentEstimatedValue, "currentEstimatedValue");
        touch();
    }

    public BigDecimal getExpectedProfitAmount() {
        return expectedProfitAmount;
    }

    public void setExpectedProfitAmount(BigDecimal expectedProfitAmount) {
        if (expectedProfitAmount == null) {
            this.expectedProfitAmount = null;
        } else {
            this.expectedProfitAmount = ValidationUtils.requireNonNegative(expectedProfitAmount, "expectedProfitAmount");
        }
        touch();
    }

    public BigDecimal getExpectedProfitPercent() {
        return expectedProfitPercent;
    }

    public void setExpectedProfitPercent(BigDecimal expectedProfitPercent) {
        if (expectedProfitPercent == null) {
            this.expectedProfitPercent = null;
        } else {
            this.expectedProfitPercent = ValidationUtils.requireNonNegative(expectedProfitPercent, "expectedProfitPercent");
        }
        touch();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = ValidationUtils.requireNonNull(startDate, "startDate");
        touch();
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
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
        return "Investment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", kind=" + kind +
                ", status=" + status +
                ", currentInvestedAmount=" + currentInvestedAmount +
                ", currentEstimatedValue=" + currentEstimatedValue +
                ", priority=" + priority +
                ", active=" + active +
                '}';
    }
}
