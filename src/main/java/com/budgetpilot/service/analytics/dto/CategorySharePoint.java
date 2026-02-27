package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;

public class CategorySharePoint {
    private final ExpenseCategory category;
    private final String categoryLabel;
    private final BigDecimal total;
    private final BigDecimal percentOfTotal;
    private final int entryCount;

    public CategorySharePoint(
            ExpenseCategory category,
            String categoryLabel,
            BigDecimal total,
            BigDecimal percentOfTotal,
            int entryCount
    ) {
        this.category = ValidationUtils.requireNonNull(category, "category");
        this.categoryLabel = ValidationUtils.requireNonBlank(categoryLabel, "categoryLabel");
        this.total = ValidationUtils.requireNonNull(total, "total");
        this.percentOfTotal = ValidationUtils.requireNonNull(percentOfTotal, "percentOfTotal");
        this.entryCount = Math.max(0, entryCount);
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getPercentOfTotal() {
        return percentOfTotal;
    }

    public int getEntryCount() {
        return entryCount;
    }
}
