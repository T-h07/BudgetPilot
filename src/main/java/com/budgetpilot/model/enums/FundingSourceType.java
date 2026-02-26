package com.budgetpilot.model.enums;

public enum FundingSourceType {
    FREE_MONEY("Free Money (This Month)"),
    SAVINGS_BUCKET("Savings Bucket");

    private final String label;

    FundingSourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
