package com.budgetpilot.service;

import com.budgetpilot.model.enums.FamilyExpenseType;

import java.math.BigDecimal;
import java.util.Objects;

public class FamilyExpenseSummary {
    private final FamilyExpenseType expenseType;
    private final BigDecimal total;
    private final int entryCount;

    public FamilyExpenseSummary(FamilyExpenseType expenseType, BigDecimal total, int entryCount) {
        this.expenseType = Objects.requireNonNull(expenseType, "expenseType");
        this.total = Objects.requireNonNull(total, "total");
        this.entryCount = entryCount;
    }

    public FamilyExpenseType getExpenseType() {
        return expenseType;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getEntryCount() {
        return entryCount;
    }
}
