package com.budgetpilot.model.enums;

public enum AchievementTier {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold");

    private final String label;

    AchievementTier(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
