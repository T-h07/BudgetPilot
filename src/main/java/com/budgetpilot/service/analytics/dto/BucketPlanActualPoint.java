package com.budgetpilot.service.analytics.dto;

import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;

public class BucketPlanActualPoint {
    private final PlannerBucket bucket;
    private final String bucketLabel;
    private final BigDecimal planned;
    private final BigDecimal actual;

    public BucketPlanActualPoint(
            PlannerBucket bucket,
            String bucketLabel,
            BigDecimal planned,
            BigDecimal actual
    ) {
        this.bucket = ValidationUtils.requireNonNull(bucket, "bucket");
        this.bucketLabel = ValidationUtils.requireNonBlank(bucketLabel, "bucketLabel");
        this.planned = ValidationUtils.requireNonNull(planned, "planned");
        this.actual = ValidationUtils.requireNonNull(actual, "actual");
    }

    public PlannerBucket getBucket() {
        return bucket;
    }

    public String getBucketLabel() {
        return bucketLabel;
    }

    public BigDecimal getPlanned() {
        return planned;
    }

    public BigDecimal getActual() {
        return actual;
    }
}
