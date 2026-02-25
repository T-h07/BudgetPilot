package com.budgetpilot.model.enums;

public enum GoalFundingSource {
    FREE_MONEY("Free Money"),
    SAVINGS_BUCKET("Savings Bucket"),
    MANUAL("Manual");

    private final String label;

    GoalFundingSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
