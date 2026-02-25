package com.budgetpilot.service.planner;

import com.budgetpilot.model.enums.PlannerBucket;

import java.math.BigDecimal;
import java.util.Objects;

public class PlanVsActualRow {
    private final PlannerBucket bucket;
    private final BigDecimal planned;
    private final BigDecimal actual;
    private final BigDecimal remaining;
    private final BigDecimal usagePercent;
    private final PlanVsActualStatus status;

    public PlanVsActualRow(
            PlannerBucket bucket,
            BigDecimal planned,
            BigDecimal actual,
            BigDecimal remaining,
            BigDecimal usagePercent,
            PlanVsActualStatus status
    ) {
        this.bucket = Objects.requireNonNull(bucket, "bucket");
        this.planned = Objects.requireNonNull(planned, "planned");
        this.actual = Objects.requireNonNull(actual, "actual");
        this.remaining = Objects.requireNonNull(remaining, "remaining");
        this.usagePercent = Objects.requireNonNull(usagePercent, "usagePercent");
        this.status = Objects.requireNonNull(status, "status");
    }

    public PlannerBucket getBucket() {
        return bucket;
    }

    public BigDecimal getPlanned() {
        return planned;
    }

    public BigDecimal getActual() {
        return actual;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public BigDecimal getUsagePercent() {
        return usagePercent;
    }

    public PlanVsActualStatus getStatus() {
        return status;
    }
}
