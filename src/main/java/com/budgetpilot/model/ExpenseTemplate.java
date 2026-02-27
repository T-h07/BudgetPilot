package com.budgetpilot.model;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.model.enums.RecurrenceCadence;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ExpenseTemplate {
    private String id;
    private String name;
    private PlannerBucket plannerBucket;
    private ExpenseCategory category;
    private String subcategory;
    private PaymentMethod paymentMethod;
    private BigDecimal defaultAmount;
    private RecurrenceCadence cadence;
    private int dayOfMonth;
    private boolean active;
    private String tag;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ExpenseTemplate() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.name = "Template";
        this.plannerBucket = PlannerBucket.DISCRETIONARY;
        this.category = ExpenseCategory.OTHER;
        this.subcategory = "";
        this.paymentMethod = PaymentMethod.CARD;
        this.defaultAmount = BigDecimal.ZERO.setScale(2);
        this.cadence = RecurrenceCadence.MONTHLY;
        this.dayOfMonth = 1;
        this.active = true;
        this.tag = "";
        this.note = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public ExpenseTemplate(ExpenseTemplate other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.name = other.name;
        this.plannerBucket = other.plannerBucket;
        this.category = other.category;
        this.subcategory = other.subcategory;
        this.paymentMethod = other.paymentMethod;
        this.defaultAmount = other.defaultAmount;
        this.cadence = other.cadence;
        this.dayOfMonth = other.dayOfMonth;
        this.active = other.active;
        this.tag = other.tag;
        this.note = other.note;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public ExpenseTemplate copy() {
        return new ExpenseTemplate(this);
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

    public PlannerBucket getPlannerBucket() {
        return plannerBucket;
    }

    public void setPlannerBucket(PlannerBucket plannerBucket) {
        this.plannerBucket = ValidationUtils.requireNonNull(plannerBucket, "plannerBucket");
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag == null ? "" : tag.trim();
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
        return "ExpenseTemplate{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", plannerBucket=" + plannerBucket +
                ", category=" + category +
                ", defaultAmount=" + MoneyUtils.normalize(defaultAmount) +
                ", cadence=" + cadence +
                ", dayOfMonth=" + dayOfMonth +
                ", active=" + active +
                '}';
    }
}
