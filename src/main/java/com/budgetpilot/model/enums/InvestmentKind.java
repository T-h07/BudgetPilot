package com.budgetpilot.model.enums;

public enum InvestmentKind {
    MONEY("Money"),
    PERSONAL_DEVELOPMENT("Personal Development");

    private final String label;

    InvestmentKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
