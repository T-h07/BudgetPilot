package com.budgetpilot.service.expenses;

import com.budgetpilot.model.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.util.Objects;

public class ExpenseCategorySummary {
    private final ExpenseCategory category;
    private final BigDecimal total;
    private final BigDecimal percentOfTotal;
    private final int entryCount;

    public ExpenseCategorySummary(
            ExpenseCategory category,
            BigDecimal total,
            BigDecimal percentOfTotal,
            int entryCount
    ) {
        this.category = Objects.requireNonNull(category, "category");
        this.total = Objects.requireNonNull(total, "total");
        this.percentOfTotal = Objects.requireNonNull(percentOfTotal, "percentOfTotal");
        this.entryCount = entryCount;
    }

    public ExpenseCategory getCategory() {
        return category;
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
