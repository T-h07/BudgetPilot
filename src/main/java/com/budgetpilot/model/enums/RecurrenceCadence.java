package com.budgetpilot.model.enums;

public enum RecurrenceCadence {
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    BIWEEKLY("Biweekly"),
    QUARTERLY("Quarterly");

    private final String label;

    RecurrenceCadence(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
