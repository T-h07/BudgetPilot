package com.budgetpilot.model.enums;

public enum InvestmentTransactionType {
    CONTRIBUTION("Contribution"),
    RETURN("Return"),
    WITHDRAWAL("Withdrawal"),
    ADJUSTMENT("Adjustment"),
    FEE("Fee");

    private final String label;

    InvestmentTransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
