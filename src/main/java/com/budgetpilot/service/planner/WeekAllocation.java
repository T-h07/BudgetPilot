package com.budgetpilot.service.planner;

import java.math.BigDecimal;
import java.util.Objects;

public class WeekAllocation {
    private final String weekLabel;
    private final String dateRange;
    private final BigDecimal plannedTotal;
    private final BigDecimal plannedFood;
    private final BigDecimal plannedTransport;
    private final BigDecimal plannedDiscretionary;
    private final BigDecimal plannedFamily;

    public WeekAllocation(
            String weekLabel,
            String dateRange,
            BigDecimal plannedTotal,
            BigDecimal plannedFood,
            BigDecimal plannedTransport,
            BigDecimal plannedDiscretionary,
            BigDecimal plannedFamily
    ) {
        this.weekLabel = weekLabel;
        this.dateRange = dateRange;
        this.plannedTotal = Objects.requireNonNull(plannedTotal, "plannedTotal");
        this.plannedFood = Objects.requireNonNull(plannedFood, "plannedFood");
        this.plannedTransport = Objects.requireNonNull(plannedTransport, "plannedTransport");
        this.plannedDiscretionary = Objects.requireNonNull(plannedDiscretionary, "plannedDiscretionary");
        this.plannedFamily = Objects.requireNonNull(plannedFamily, "plannedFamily");
    }

    public String getWeekLabel() {
        return weekLabel;
    }

    public String getDateRange() {
        return dateRange;
    }

    public BigDecimal getPlannedTotal() {
        return plannedTotal;
    }

    public BigDecimal getPlannedFood() {
        return plannedFood;
    }

    public BigDecimal getPlannedTransport() {
        return plannedTransport;
    }

    public BigDecimal getPlannedDiscretionary() {
        return plannedDiscretionary;
    }

    public BigDecimal getPlannedFamily() {
        return plannedFamily;
    }
}
