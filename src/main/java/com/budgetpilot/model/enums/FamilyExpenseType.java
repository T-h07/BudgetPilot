package com.budgetpilot.model.enums;

public enum FamilyExpenseType {
    ALLOWANCE("Allowance"),
    MEDICAL("Medical"),
    SCHOOL("School"),
    SUPPORT("Support"),
    EXTRA("Extra"),
    EMERGENCY("Emergency");

    private final String label;

    FamilyExpenseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
