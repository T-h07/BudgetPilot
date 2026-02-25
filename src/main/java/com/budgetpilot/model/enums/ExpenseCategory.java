package com.budgetpilot.model.enums;

public enum ExpenseCategory {
    FOOD("Food"),
    CLOTHES("Clothes"),
    ENTERTAINMENT("Entertainment"),
    CAR("Car"),
    BILLS("Bills"),
    HEALTH("Health"),
    FAMILY("Family"),
    EDUCATION("Education"),
    SUBSCRIPTIONS("Subscriptions"),
    OTHER("Other");

    private final String label;

    ExpenseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
