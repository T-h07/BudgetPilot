package com.budgetpilot.model.enums;

public enum HabitAllowanceMode {
    DYNAMIC("Dynamic"),
    LOCKED("Locked");

    private final String label;

    HabitAllowanceMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
