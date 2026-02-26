package com.budgetpilot.service.analytics.dto;

import java.util.List;

public class HabitTrendSeries {
    private final List<HabitTrendPoint> points;

    public HabitTrendSeries(List<HabitTrendPoint> points) {
        this.points = List.copyOf(points == null ? List.of() : points);
    }

    public List<HabitTrendPoint> getPoints() {
        return points;
    }
}
