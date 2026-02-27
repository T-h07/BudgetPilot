package com.budgetpilot.service.dashboard;

import java.util.List;
import java.util.Objects;

public class KpiVisualData {
    private final KpiVisualType visualType;
    private final KpiStatus status;
    private final List<Double> sparkPoints;
    private final double primaryRatio;
    private final double secondaryRatio;
    private final TrendDirection trendDirection;

    public KpiVisualData(
            KpiVisualType visualType,
            KpiStatus status,
            List<Double> sparkPoints,
            double primaryRatio,
            double secondaryRatio,
            TrendDirection trendDirection
    ) {
        this.visualType = Objects.requireNonNull(visualType, "visualType");
        this.status = Objects.requireNonNull(status, "status");
        this.sparkPoints = List.copyOf(sparkPoints == null ? List.of() : sparkPoints);
        this.primaryRatio = clamp(primaryRatio);
        this.secondaryRatio = clamp(secondaryRatio);
        this.trendDirection = trendDirection == null ? TrendDirection.UNKNOWN : trendDirection;
    }

    public KpiVisualType getVisualType() {
        return visualType;
    }

    public KpiStatus getStatus() {
        return status;
    }

    public List<Double> getSparkPoints() {
        return sparkPoints;
    }

    public double getPrimaryRatio() {
        return primaryRatio;
    }

    public double getSecondaryRatio() {
        return secondaryRatio;
    }

    public TrendDirection getTrendDirection() {
        return trendDirection;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value));
    }
}
