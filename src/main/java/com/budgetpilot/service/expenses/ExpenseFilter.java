package com.budgetpilot.service.expenses;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.PlannerBucket;

public class ExpenseFilter {
    private String searchText;
    private ExpenseCategory category;
    private PaymentMethod paymentMethod;
    private PlannerBucket plannerBucket;
    private String tagText;
    private boolean onlyTagged;

    public ExpenseFilter() {
        this.searchText = "";
        this.tagText = "";
        this.onlyTagged = false;
    }

    public static ExpenseFilter all() {
        return new ExpenseFilter();
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText == null ? "" : searchText.trim();
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PlannerBucket getPlannerBucket() {
        return plannerBucket;
    }

    public void setPlannerBucket(PlannerBucket plannerBucket) {
        this.plannerBucket = plannerBucket;
    }

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText == null ? "" : tagText.trim();
    }

    public boolean isOnlyTagged() {
        return onlyTagged;
    }

    public void setOnlyTagged(boolean onlyTagged) {
        this.onlyTagged = onlyTagged;
    }

    public boolean hasSearchText() {
        return !searchText.isBlank();
    }

    public boolean hasTagText() {
        return !tagText.isBlank();
    }
}
