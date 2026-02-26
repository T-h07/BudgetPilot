package com.budgetpilot.service.analytics.dto;

import java.util.List;

public class ForecastAccuracySeries {
    private final List<ForecastAccuracyPoint> points;

    public ForecastAccuracySeries(List<ForecastAccuracyPoint> points) {
        this.points = List.copyOf(points == null ? List.of() : points);
    }

    public List<ForecastAccuracyPoint> getPoints() {
        return points;
    }
}
