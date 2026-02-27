package com.budgetpilot.model.enums;

public enum GoalContributionType {
    CONTRIBUTION("Contribution"),
    WITHDRAWAL("Withdrawal"),
    ADJUSTMENT("Adjustment");

    private final String label;

    GoalContributionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
