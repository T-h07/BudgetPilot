package com.budgetpilot.service.export;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.util.ValidationUtils;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Locale;

public class ExportRequest {
    private final YearMonth startMonth;
    private final YearMonth endMonth;
    private final ExportFormat format;
    private final boolean includeExpenses;
    private final boolean includeIncome;
    private final boolean includeSavingsEntries;
    private final boolean includeGoalContributions;
    private final boolean includePlannerPlans;
    private final boolean includeHabitsSummary;
    private final boolean includeFamilyExpenses;
    private final boolean includeInvestmentTransactions;
    private final PlannerBucket expenseBucketFilter;
    private final ExpenseCategory expenseCategoryFilter;
    private final String expenseTagContains;
    private final String expenseSubcategoryContains;
    private final Path outputDir;

    public ExportRequest(
            YearMonth startMonth,
            YearMonth endMonth,
            ExportFormat format,
            boolean includeExpenses,
            boolean includeIncome,
            boolean includeSavingsEntries,
            boolean includeGoalContributions,
            boolean includePlannerPlans,
            boolean includeHabitsSummary,
            boolean includeFamilyExpenses,
            boolean includeInvestmentTransactions,
            PlannerBucket expenseBucketFilter,
            ExpenseCategory expenseCategoryFilter,
            String expenseTagContains,
            String expenseSubcategoryContains,
            Path outputDir
    ) {
        this.startMonth = ValidationUtils.requireNonNull(startMonth, "startMonth");
        this.endMonth = ValidationUtils.requireNonNull(endMonth, "endMonth");
        this.format = ValidationUtils.requireNonNull(format, "format");
        this.includeExpenses = includeExpenses;
        this.includeIncome = includeIncome;
        this.includeSavingsEntries = includeSavingsEntries;
        this.includeGoalContributions = includeGoalContributions;
        this.includePlannerPlans = includePlannerPlans;
        this.includeHabitsSummary = includeHabitsSummary;
        this.includeFamilyExpenses = includeFamilyExpenses;
        this.includeInvestmentTransactions = includeInvestmentTransactions;
        this.expenseBucketFilter = expenseBucketFilter;
        this.expenseCategoryFilter = expenseCategoryFilter;
        this.expenseTagContains = normalizeSearch(expenseTagContains);
        this.expenseSubcategoryContains = normalizeSearch(expenseSubcategoryContains);
        this.outputDir = ValidationUtils.requireNonNull(outputDir, "outputDir");
    }

    public YearMonth getStartMonth() {
        return startMonth;
    }

    public YearMonth getEndMonth() {
        return endMonth;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public boolean isIncludeExpenses() {
        return includeExpenses;
    }

    public boolean isIncludeIncome() {
        return includeIncome;
    }

    public boolean isIncludeSavingsEntries() {
        return includeSavingsEntries;
    }

    public boolean isIncludeGoalContributions() {
        return includeGoalContributions;
    }

    public boolean isIncludePlannerPlans() {
        return includePlannerPlans;
    }

    public boolean isIncludeHabitsSummary() {
        return includeHabitsSummary;
    }

    public boolean isIncludeFamilyExpenses() {
        return includeFamilyExpenses;
    }

    public boolean isIncludeInvestmentTransactions() {
        return includeInvestmentTransactions;
    }

    public PlannerBucket getExpenseBucketFilter() {
        return expenseBucketFilter;
    }

    public ExpenseCategory getExpenseCategoryFilter() {
        return expenseCategoryFilter;
    }

    public String getExpenseTagContains() {
        return expenseTagContains;
    }

    public String getExpenseSubcategoryContains() {
        return expenseSubcategoryContains;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public boolean hasAnyDatasetSelected() {
        return includeExpenses
                || includeIncome
                || includeSavingsEntries
                || includeGoalContributions
                || includePlannerPlans
                || includeHabitsSummary
                || includeFamilyExpenses
                || includeInvestmentTransactions;
    }

    public boolean hasExpenseTagFilter() {
        return !expenseTagContains.isBlank();
    }

    public boolean hasExpenseSubcategoryFilter() {
        return !expenseSubcategoryContains.isBlank();
    }

    private String normalizeSearch(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
