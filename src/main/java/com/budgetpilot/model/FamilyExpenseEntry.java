package com.budgetpilot.model;

import com.budgetpilot.model.enums.FamilyExpenseType;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class FamilyExpenseEntry {
    private String id;
    private String familyMemberId;
    private YearMonth month;
    private LocalDate expenseDate;
    private BigDecimal amount;
    private FamilyExpenseType expenseType;
    private String note;
    private String relatedExpenseEntryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FamilyExpenseEntry() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.familyMemberId = "";
        this.month = MonthUtils.currentMonth();
        this.expenseDate = LocalDate.now();
        this.amount = new BigDecimal("1.00");
        this.expenseType = FamilyExpenseType.SUPPORT;
        this.note = "";
        this.relatedExpenseEntryId = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public FamilyExpenseEntry(
            String familyMemberId,
            YearMonth month,
            LocalDate expenseDate,
            BigDecimal amount,
            FamilyExpenseType expenseType
    ) {
        this();
        setFamilyMemberId(familyMemberId);
        setMonth(month);
        setExpenseDate(expenseDate);
        setAmount(amount);
        setExpenseType(expenseType);
    }

    public FamilyExpenseEntry(FamilyExpenseEntry other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.familyMemberId = other.familyMemberId;
        this.month = other.month;
        this.expenseDate = other.expenseDate;
        this.amount = other.amount;
        this.expenseType = other.expenseType;
        this.note = other.note;
        this.relatedExpenseEntryId = other.relatedExpenseEntryId;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public FamilyExpenseEntry copy() {
        return new FamilyExpenseEntry(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getFamilyMemberId() {
        return familyMemberId;
    }

    public void setFamilyMemberId(String familyMemberId) {
        this.familyMemberId = ValidationUtils.requireNonBlank(familyMemberId, "familyMemberId");
        touch();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        touch();
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = ValidationUtils.requireNonNull(expenseDate, "expenseDate");
        touch();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.normalize(ValidationUtils.requireNonNull(amount, "amount"));
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        this.amount = normalized;
        touch();
    }

    public FamilyExpenseType getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(FamilyExpenseType expenseType) {
        this.expenseType = ValidationUtils.requireNonNull(expenseType, "expenseType");
        touch();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
        touch();
    }

    public String getRelatedExpenseEntryId() {
        return relatedExpenseEntryId;
    }

    public void setRelatedExpenseEntryId(String relatedExpenseEntryId) {
        this.relatedExpenseEntryId = relatedExpenseEntryId == null ? "" : relatedExpenseEntryId.trim();
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
        return "FamilyExpenseEntry{" +
                "id='" + id + '\'' +
                ", familyMemberId='" + familyMemberId + '\'' +
                ", month=" + month +
                ", expenseDate=" + expenseDate +
                ", amount=" + amount +
                ", expenseType=" + expenseType +
                '}';
    }
}
