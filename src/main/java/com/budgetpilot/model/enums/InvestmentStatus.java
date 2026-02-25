package com.budgetpilot.model.enums;

public enum InvestmentStatus {
    PLANNED("Planned"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    InvestmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
