package com.budgetpilot.model;

import com.budgetpilot.model.enums.InvestmentTransactionType;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class InvestmentTransaction {
    private String id;
    private String investmentId;
    private YearMonth month;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private InvestmentTransactionType type;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InvestmentTransaction() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.investmentId = "";
        this.month = MonthUtils.currentMonth();
        this.transactionDate = LocalDate.now();
        this.amount = new BigDecimal("1.00");
        this.type = InvestmentTransactionType.CONTRIBUTION;
        this.note = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public InvestmentTransaction(
            String investmentId,
            YearMonth month,
            LocalDate transactionDate,
            BigDecimal amount,
            InvestmentTransactionType type
    ) {
        this();
        setInvestmentId(investmentId);
        setMonth(month);
        setTransactionDate(transactionDate);
        setAmount(amount);
        setType(type);
    }

    public InvestmentTransaction(InvestmentTransaction other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.investmentId = other.investmentId;
        this.month = other.month;
        this.transactionDate = other.transactionDate;
        this.amount = other.amount;
        this.type = other.type;
        this.note = other.note;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public InvestmentTransaction copy() {
        return new InvestmentTransaction(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        touch();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        touch();
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = ValidationUtils.requireNonNull(transactionDate, "transactionDate");
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

    public InvestmentTransactionType getType() {
        return type;
    }

    public void setType(InvestmentTransactionType type) {
        this.type = ValidationUtils.requireNonNull(type, "type");
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
        return "InvestmentTransaction{" +
                "id='" + id + '\'' +
                ", investmentId='" + investmentId + '\'' +
                ", month=" + month +
                ", transactionDate=" + transactionDate +
                ", amount=" + amount +
                ", type=" + type +
                '}';
    }
}
