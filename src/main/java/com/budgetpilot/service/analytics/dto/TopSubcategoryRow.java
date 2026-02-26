package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;

public class TopSubcategoryRow {
    private final String label;
    private final BigDecimal amount;
    private final int entryCount;

    public TopSubcategoryRow(String label, BigDecimal amount, int entryCount) {
        this.label = ValidationUtils.requireNonBlank(label, "label");
        this.amount = ValidationUtils.requireNonNull(amount, "amount");
        this.entryCount = Math.max(0, entryCount);
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getEntryCount() {
        return entryCount;
    }
}
