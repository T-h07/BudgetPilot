package com.budgetpilot.service.analytics;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.util.ValidationUtils;

import java.time.YearMonth;
import java.util.Locale;

public class AnalyticsQuery {
    private final YearMonth endMonth;
    private final int monthsBack;
    private final PlannerBucket bucketFilter;
    private final ExpenseCategory categoryFilter;
    private final String tagOrSubcategorySearch;

    public AnalyticsQuery(
            YearMonth endMonth,
            int monthsBack,
            PlannerBucket bucketFilter,
            ExpenseCategory categoryFilter,
            String tagOrSubcategorySearch
    ) {
        this.endMonth = ValidationUtils.requireNonNull(endMonth, "endMonth");
        this.monthsBack = Math.max(1, monthsBack);
        this.bucketFilter = bucketFilter;
        this.categoryFilter = categoryFilter;
        this.tagOrSubcategorySearch = normalizeSearch(tagOrSubcategorySearch);
    }

    public YearMonth getEndMonth() {
        return endMonth;
    }

    public int getMonthsBack() {
        return monthsBack;
    }

    public PlannerBucket getBucketFilter() {
        return bucketFilter;
    }

    public ExpenseCategory getCategoryFilter() {
        return categoryFilter;
    }

    public String getTagOrSubcategorySearch() {
        return tagOrSubcategorySearch;
    }

    public boolean hasSearchFilter() {
        return !tagOrSubcategorySearch.isBlank();
    }

    private String normalizeSearch(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
