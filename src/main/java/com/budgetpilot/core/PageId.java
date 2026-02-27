package com.budgetpilot.core;

public enum PageId {
    LOGIN("Login"),
    DASHBOARD("Dashboard"),
    EXPENSES("Expenses"),
    PLANNER("Planner"),
    SAVINGS("Savings"),
    INVESTMENTS("Investments"),
    GOALS("Goals"),
    INCOME("Income"),
    ACHIEVEMENTS("Achievements"),
    FAMILY("Family"),
    HABITS("Habits"),
    SETTINGS("Settings"),
    ONBOARDING("Onboarding");

    private final String displayLabel;

    PageId(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
