package com.budgetpilot.service.expenses;

import com.budgetpilot.model.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public class ExpenseSummary {
    private final YearMonth month;
    private final BigDecimal totalExpenses;
    private final BigDecimal averageDailySpend;
    private final int expenseCount;
    private final int distinctCategoriesCount;
    private final ExpenseCategory topCategory;
    private final BigDecimal topCategoryAmount;

    public ExpenseSummary(
            YearMonth month,
            BigDecimal totalExpenses,
            BigDecimal averageDailySpend,
            int expenseCount,
            int distinctCategoriesCount,
            ExpenseCategory topCategory,
            BigDecimal topCategoryAmount
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.totalExpenses = Objects.requireNonNull(totalExpenses, "totalExpenses");
        this.averageDailySpend = Objects.requireNonNull(averageDailySpend, "averageDailySpend");
        this.expenseCount = expenseCount;
        this.distinctCategoriesCount = distinctCategoriesCount;
        this.topCategory = topCategory;
        this.topCategoryAmount = Objects.requireNonNull(topCategoryAmount, "topCategoryAmount");
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getAverageDailySpend() {
        return averageDailySpend;
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public int getDistinctCategoriesCount() {
        return distinctCategoriesCount;
    }

    public ExpenseCategory getTopCategory() {
        return topCategory;
    }

    public BigDecimal getTopCategoryAmount() {
        return topCategoryAmount;
    }
}
