package com.budgetpilot.model.enums;

public enum InvestmentType {
    STOCKS("Stocks"),
    CRYPTO("Crypto"),
    BUSINESS("Business"),
    COURSE("Course"),
    EQUIPMENT("Equipment"),
    REAL_ESTATE("Real Estate"),
    TUITION("Tuition"),
    OTHER("Other");

    private final String label;

    InvestmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
