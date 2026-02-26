package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;

public class ForecastAccuracyPoint {
    private final YearMonth month;
    private final String monthLabel;
    private final BigDecimal projectedFromWeekOne;
    private final BigDecimal actual;

    public ForecastAccuracyPoint(
            YearMonth month,
            String monthLabel,
            BigDecimal projectedFromWeekOne,
            BigDecimal actual
    ) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.monthLabel = ValidationUtils.requireNonBlank(monthLabel, "monthLabel");
        this.projectedFromWeekOne = ValidationUtils.requireNonNull(projectedFromWeekOne, "projectedFromWeekOne");
        this.actual = ValidationUtils.requireNonNull(actual, "actual");
    }

    public YearMonth getMonth() {
        return month;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public BigDecimal getProjectedFromWeekOne() {
        return projectedFromWeekOne;
    }

    public BigDecimal getActual() {
        return actual;
    }
}
