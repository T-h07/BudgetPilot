package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MonthPoint {
    private final YearMonth month;
    private final String monthLabel;
    private final BigDecimal value;

    public MonthPoint(YearMonth month, String monthLabel, BigDecimal value) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.monthLabel = ValidationUtils.requireNonBlank(monthLabel, "monthLabel");
        this.value = ValidationUtils.requireNonNull(value, "value");
    }

    public YearMonth getMonth() {
        return month;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public BigDecimal getValue() {
        return value;
    }
}
