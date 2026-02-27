package com.budgetpilot.model.enums;

public enum GoalType {
    CAR("Car"),
    LAPTOP("Laptop"),
    VACATION("Vacation"),
    EMERGENCY_FUND("Emergency Fund"),
    TUITION("Tuition"),
    DEBT_PAYOFF("Debt Payoff"),
    CUSTOM("Custom");

    private final String label;

    GoalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
