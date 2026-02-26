package com.budgetpilot.service.month;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;

public class ExpenseTemplateCandidate {
    private final String key;
    private final PlannerBucket bucket;
    private final ExpenseCategory category;
    private final String subcategory;
    private final String displayName;
    private final BigDecimal lastAmount;
    private final BigDecimal avgAmount;
    private final PaymentMethod paymentMethod;
    private final boolean wasRecurring;
    private final int countOccurrences;
    private final String tag;
    private final String note;

    public ExpenseTemplateCandidate(
            String key,
            PlannerBucket bucket,
            ExpenseCategory category,
            String subcategory,
            String displayName,
            BigDecimal lastAmount,
            BigDecimal avgAmount,
            PaymentMethod paymentMethod,
            boolean wasRecurring,
            int countOccurrences,
            String tag,
            String note
    ) {
        this.key = ValidationUtils.requireNonBlank(key, "key");
        this.bucket = ValidationUtils.requireNonNull(bucket, "bucket");
        this.category = ValidationUtils.requireNonNull(category, "category");
        this.subcategory = subcategory == null ? "" : subcategory.trim();
        this.displayName = ValidationUtils.requireNonBlank(displayName, "displayName");
        this.lastAmount = MoneyUtils.normalize(ValidationUtils.requireNonNegative(lastAmount, "lastAmount"));
        this.avgAmount = MoneyUtils.normalize(ValidationUtils.requireNonNegative(avgAmount, "avgAmount"));
        this.paymentMethod = paymentMethod;
        this.wasRecurring = wasRecurring;
        this.countOccurrences = Math.max(1, countOccurrences);
        this.tag = tag == null ? "" : tag.trim();
        this.note = note == null ? "" : note.trim();
    }

    public String getKey() {
        return key;
    }

    public PlannerBucket getBucket() {
        return bucket;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getLastAmount() {
        return lastAmount;
    }

    public BigDecimal getAvgAmount() {
        return avgAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public boolean wasRecurring() {
        return wasRecurring;
    }

    public int getCountOccurrences() {
        return countOccurrences;
    }

    public String getTag() {
        return tag;
    }

    public String getNote() {
        return note;
    }
}
