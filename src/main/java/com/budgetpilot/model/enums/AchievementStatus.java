package com.budgetpilot.model.enums;

public enum AchievementStatus {
    LOCKED("Locked"),
    IN_PROGRESS("In Progress"),
    UNLOCKED("Unlocked");

    private final String label;

    AchievementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
