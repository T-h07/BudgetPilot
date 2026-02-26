package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class AnalyticsSnapshot {
    private final YearMonth endMonth;
    private final int monthsBack;
    private final List<MonthPoint> monthlySpendTrend;
    private final List<MonthPoint> monthlyIncomeTrend;
    private final List<MonthPoint> monthlyUnplannedTrend;
    private final PlanVsActualBreakdown selectedMonthBuckets;
    private final List<CategorySharePoint> categoryShare;
    private final ForecastAccuracySeries forecastAccuracy;
    private final List<TopSubcategoryRow> topSubcategories;
    private final HabitTrendSeries habitTrend;
    private final BigDecimal totalSpendInRange;
    private final BigDecimal avgMonthlySpend;
    private final BigDecimal bestMonthSpend;
    private final BigDecimal worstMonthSpend;

    public AnalyticsSnapshot(
            YearMonth endMonth,
            int monthsBack,
            List<MonthPoint> monthlySpendTrend,
            List<MonthPoint> monthlyIncomeTrend,
            List<MonthPoint> monthlyUnplannedTrend,
            PlanVsActualBreakdown selectedMonthBuckets,
            List<CategorySharePoint> categoryShare,
            ForecastAccuracySeries forecastAccuracy,
            List<TopSubcategoryRow> topSubcategories,
            HabitTrendSeries habitTrend,
            BigDecimal totalSpendInRange,
            BigDecimal avgMonthlySpend,
            BigDecimal bestMonthSpend,
            BigDecimal worstMonthSpend
    ) {
        this.endMonth = ValidationUtils.requireNonNull(endMonth, "endMonth");
        this.monthsBack = Math.max(1, monthsBack);
        this.monthlySpendTrend = List.copyOf(monthlySpendTrend == null ? List.of() : monthlySpendTrend);
        this.monthlyIncomeTrend = List.copyOf(monthlyIncomeTrend == null ? List.of() : monthlyIncomeTrend);
        this.monthlyUnplannedTrend = List.copyOf(monthlyUnplannedTrend == null ? List.of() : monthlyUnplannedTrend);
        this.selectedMonthBuckets = ValidationUtils.requireNonNull(selectedMonthBuckets, "selectedMonthBuckets");
        this.categoryShare = List.copyOf(categoryShare == null ? List.of() : categoryShare);
        this.forecastAccuracy = ValidationUtils.requireNonNull(forecastAccuracy, "forecastAccuracy");
        this.topSubcategories = List.copyOf(topSubcategories == null ? List.of() : topSubcategories);
        this.habitTrend = ValidationUtils.requireNonNull(habitTrend, "habitTrend");
        this.totalSpendInRange = ValidationUtils.requireNonNull(totalSpendInRange, "totalSpendInRange");
        this.avgMonthlySpend = ValidationUtils.requireNonNull(avgMonthlySpend, "avgMonthlySpend");
        this.bestMonthSpend = ValidationUtils.requireNonNull(bestMonthSpend, "bestMonthSpend");
        this.worstMonthSpend = ValidationUtils.requireNonNull(worstMonthSpend, "worstMonthSpend");
    }

    public YearMonth getEndMonth() {
        return endMonth;
    }

    public int getMonthsBack() {
        return monthsBack;
    }

    public List<MonthPoint> getMonthlySpendTrend() {
        return monthlySpendTrend;
    }

    public List<MonthPoint> getMonthlyIncomeTrend() {
        return monthlyIncomeTrend;
    }

    public List<MonthPoint> getMonthlyUnplannedTrend() {
        return monthlyUnplannedTrend;
    }

    public PlanVsActualBreakdown getSelectedMonthBuckets() {
        return selectedMonthBuckets;
    }

    public List<CategorySharePoint> getCategoryShare() {
        return categoryShare;
    }

    public ForecastAccuracySeries getForecastAccuracy() {
        return forecastAccuracy;
    }

    public List<TopSubcategoryRow> getTopSubcategories() {
        return topSubcategories;
    }

    public HabitTrendSeries getHabitTrend() {
        return habitTrend;
    }

    public BigDecimal getTotalSpendInRange() {
        return totalSpendInRange;
    }

    public BigDecimal getAvgMonthlySpend() {
        return avgMonthlySpend;
    }

    public BigDecimal getBestMonthSpend() {
        return bestMonthSpend;
    }

    public BigDecimal getWorstMonthSpend() {
        return worstMonthSpend;
    }
}
