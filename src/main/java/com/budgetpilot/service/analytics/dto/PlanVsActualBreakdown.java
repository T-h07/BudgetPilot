package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class PlanVsActualBreakdown {
    private final YearMonth month;
    private final boolean hasMonthlyPlan;
    private final List<BucketPlanActualPoint> bucketRows;
    private final BigDecimal unplannedSpend;

    public PlanVsActualBreakdown(
            YearMonth month,
            boolean hasMonthlyPlan,
            List<BucketPlanActualPoint> bucketRows,
            BigDecimal unplannedSpend
    ) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.hasMonthlyPlan = hasMonthlyPlan;
        this.bucketRows = List.copyOf(bucketRows == null ? List.of() : bucketRows);
        this.unplannedSpend = ValidationUtils.requireNonNull(unplannedSpend, "unplannedSpend");
    }

    public YearMonth getMonth() {
        return month;
    }

    public boolean isHasMonthlyPlan() {
        return hasMonthlyPlan;
    }

    public List<BucketPlanActualPoint> getBucketRows() {
        return bucketRows;
    }

    public BigDecimal getUnplannedSpend() {
        return unplannedSpend;
    }
}
