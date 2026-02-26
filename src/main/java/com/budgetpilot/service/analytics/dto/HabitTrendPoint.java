package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;

public class HabitTrendPoint {
    private final YearMonth month;
    private final String monthLabel;
    private final int activeRulesCount;
    private final int warningCount;
    private final int exceededCount;
    private final int onTrackCount;
    private final BigDecimal compliancePercent;

    public HabitTrendPoint(
            YearMonth month,
            String monthLabel,
            int activeRulesCount,
            int warningCount,
            int exceededCount,
            int onTrackCount,
            BigDecimal compliancePercent
    ) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.monthLabel = ValidationUtils.requireNonBlank(monthLabel, "monthLabel");
        this.activeRulesCount = Math.max(0, activeRulesCount);
        this.warningCount = Math.max(0, warningCount);
        this.exceededCount = Math.max(0, exceededCount);
        this.onTrackCount = Math.max(0, onTrackCount);
        this.compliancePercent = ValidationUtils.requireNonNull(compliancePercent, "compliancePercent");
    }

    public YearMonth getMonth() {
        return month;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public int getActiveRulesCount() {
        return activeRulesCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getExceededCount() {
        return exceededCount;
    }

    public int getOnTrackCount() {
        return onTrackCount;
    }

    public BigDecimal getCompliancePercent() {
        return compliancePercent;
    }
}
