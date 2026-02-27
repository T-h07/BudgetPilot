package com.budgetpilot.model.enums;

public enum AchievementCategory {
    FOUNDATION("Foundation"),
    PLANNING("Planning"),
    TRACKING("Tracking"),
    SAVINGS("Savings"),
    GOALS("Goals"),
    HABITS("Habits"),
    FAMILY("Family"),
    INVESTMENTS("Investments"),
    MILESTONES("Milestones");

    private final String label;

    AchievementCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
