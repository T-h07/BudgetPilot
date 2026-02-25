package com.budgetpilot.model;

import com.budgetpilot.model.enums.SavingsEntryType;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class SavingsEntry {
    private String id;
    private String bucketId;
    private YearMonth month;
    private LocalDate entryDate;
    private BigDecimal amount;
    private SavingsEntryType entryType;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SavingsEntry() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.bucketId = "";
        this.month = MonthUtils.currentMonth();
        this.entryDate = LocalDate.now();
        this.amount = new BigDecimal("1.00");
        this.entryType = SavingsEntryType.CONTRIBUTION;
        this.note = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public SavingsEntry(
            String bucketId,
            YearMonth month,
            LocalDate entryDate,
            BigDecimal amount,
            SavingsEntryType entryType
    ) {
        this();
        setBucketId(bucketId);
        setMonth(month);
        setEntryDate(entryDate);
        setAmount(amount);
        setEntryType(entryType);
    }

    public SavingsEntry(SavingsEntry other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.bucketId = other.bucketId;
        this.month = other.month;
        this.entryDate = other.entryDate;
        this.amount = other.amount;
        this.entryType = other.entryType;
        this.note = other.note;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public SavingsEntry copy() {
        return new SavingsEntry(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        touch();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        touch();
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = ValidationUtils.requireNonNull(entryDate, "entryDate");
        touch();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.normalize(ValidationUtils.requireNonNull(amount, "amount"));
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("amount must not be zero");
        }
        this.amount = normalized;
        touch();
    }

    public SavingsEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(SavingsEntryType entryType) {
        this.entryType = ValidationUtils.requireNonNull(entryType, "entryType");
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
        return "SavingsEntry{" +
                "id='" + id + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", month=" + month +
                ", entryDate=" + entryDate +
                ", amount=" + amount +
                ", entryType=" + entryType +
                '}';
    }
}
