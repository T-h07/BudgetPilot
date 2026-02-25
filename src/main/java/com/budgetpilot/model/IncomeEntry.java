package com.budgetpilot.model;

import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class IncomeEntry {
    private String id;
    private YearMonth month;
    private LocalDate receivedDate;
    private String sourceName;
    private IncomeType incomeType;
    private BigDecimal amount;
    private boolean recurring;
    private boolean received;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IncomeEntry() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.month = MonthUtils.currentMonth();
        this.receivedDate = LocalDate.now();
        this.sourceName = "Income";
        this.incomeType = IncomeType.OTHER;
        this.amount = BigDecimal.ZERO.setScale(2);
        this.recurring = false;
        this.received = false;
        this.notes = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public IncomeEntry(YearMonth month, LocalDate receivedDate, String sourceName, IncomeType incomeType, BigDecimal amount) {
        this();
        setMonth(month);
        setReceivedDate(receivedDate);
        setSourceName(sourceName);
        setIncomeType(incomeType);
        setAmount(amount);
    }

    public IncomeEntry(IncomeEntry other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.month = other.month;
        this.receivedDate = other.receivedDate;
        this.sourceName = other.sourceName;
        this.incomeType = other.incomeType;
        this.amount = other.amount;
        this.recurring = other.recurring;
        this.received = other.received;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public IncomeEntry copy() {
        return new IncomeEntry(this);
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

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = ValidationUtils.requireNonNull(receivedDate, "receivedDate");
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = ValidationUtils.requireNonNegative(amount, "amount");
        touch();
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
        touch();
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
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
        return "IncomeEntry{" +
                "id='" + id + '\'' +
                ", month=" + month +
                ", receivedDate=" + receivedDate +
                ", sourceName='" + sourceName + '\'' +
                ", incomeType=" + incomeType +
                ", amount=" + amount +
                ", received=" + received +
                '}';
    }
}
