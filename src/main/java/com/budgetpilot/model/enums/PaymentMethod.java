package com.budgetpilot.model.enums;

public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    BANK_TRANSFER("Bank Transfer");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
