package com.budgetpilot.service.planner;

public enum PlanVsActualStatus {
    OK("On Track"),
    WARN("Warning"),
    OVER("Over");

    private final String label;

    PlanVsActualStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
