package com.budgetpilot.model;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public class ExpenseEntry {
    private String id;
    private YearMonth month;
    private LocalDate expenseDate;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String subcategory;
    private String note;
    private PaymentMethod paymentMethod;
    private PlannerBucket plannerBucket;
    private boolean recurring;
    private String sourceTemplateId;
    private String tag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ExpenseEntry() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.month = MonthUtils.currentMonth();
        this.expenseDate = LocalDate.now();
        this.amount = new BigDecimal("1.00");
        this.category = ExpenseCategory.OTHER;
        this.subcategory = "";
        this.note = "";
        this.paymentMethod = PaymentMethod.CARD;
        this.plannerBucket = PlannerBucket.DISCRETIONARY;
        this.recurring = false;
        this.sourceTemplateId = "";
        this.tag = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public ExpenseEntry(
            YearMonth month,
            LocalDate expenseDate,
            BigDecimal amount,
            ExpenseCategory category,
            PaymentMethod paymentMethod
    ) {
        this();
        setMonth(month);
        setExpenseDate(expenseDate);
        setAmount(amount);
        setCategory(category);
        setPaymentMethod(paymentMethod);
        setPlannerBucket(PlannerBucket.inferFromCategory(category));
    }

    public ExpenseEntry(ExpenseEntry other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.month = other.month;
        this.expenseDate = other.expenseDate;
        this.amount = other.amount;
        this.category = other.category;
        this.subcategory = other.subcategory;
        this.note = other.note;
        this.paymentMethod = other.paymentMethod;
        this.plannerBucket = other.plannerBucket;
        this.recurring = other.recurring;
        this.sourceTemplateId = other.sourceTemplateId;
        this.tag = other.tag;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public ExpenseEntry copy() {
        return new ExpenseEntry(this);
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

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = ValidationUtils.requireNonNull(category, "category");
        touch();
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory == null ? "" : subcategory.trim();
        touch();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
        touch();
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = ValidationUtils.requireNonNull(paymentMethod, "paymentMethod");
        touch();
    }

    public PlannerBucket getPlannerBucket() {
        return plannerBucket;
    }

    public void setPlannerBucket(PlannerBucket plannerBucket) {
        this.plannerBucket = ValidationUtils.requireNonNull(plannerBucket, "plannerBucket");
        touch();
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
        touch();
    }

    public String getSourceTemplateId() {
        return sourceTemplateId;
    }

    public void setSourceTemplateId(String sourceTemplateId) {
        this.sourceTemplateId = sourceTemplateId == null ? "" : sourceTemplateId.trim();
        touch();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag == null ? "" : tag.trim();
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
        return "ExpenseEntry{" +
                "id='" + id + '\'' +
                ", month=" + month +
                ", expenseDate=" + expenseDate +
                ", amount=" + amount +
                ", category=" + category +
                ", plannerBucket=" + plannerBucket +
                ", sourceTemplateId='" + sourceTemplateId + '\'' +
                ", paymentMethod=" + paymentMethod +
                '}';
    }
}
