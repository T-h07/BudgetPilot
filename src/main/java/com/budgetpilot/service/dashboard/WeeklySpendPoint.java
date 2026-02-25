package com.budgetpilot.service.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

public class WeeklySpendPoint {
    private final String weekLabel;
    private final String dateRangeLabel;
    private final BigDecimal totalSpent;

    public WeeklySpendPoint(String weekLabel, String dateRangeLabel, BigDecimal totalSpent) {
        this.weekLabel = Objects.requireNonNull(weekLabel, "weekLabel");
        this.dateRangeLabel = Objects.requireNonNull(dateRangeLabel, "dateRangeLabel");
        this.totalSpent = Objects.requireNonNull(totalSpent, "totalSpent");
    }

    public String getWeekLabel() {
        return weekLabel;
    }

    public String getDateRangeLabel() {
        return dateRangeLabel;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }
}
