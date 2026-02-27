package com.budgetpilot.model.enums;

public enum SavingsEntryType {
    CONTRIBUTION("Contribution"),
    WITHDRAWAL("Withdrawal"),
    ADJUSTMENT("Adjustment");

    private final String label;

    SavingsEntryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
