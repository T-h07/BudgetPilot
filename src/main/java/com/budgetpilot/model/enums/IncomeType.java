package com.budgetpilot.model.enums;

public enum IncomeType {
    SALARY("Salary"),
    SIDE_HUSTLE("Side Hustle"),
    FREELANCE("Freelance"),
    SCHOLARSHIP("Scholarship"),
    BUSINESS("Business"),
    OTHER("Other");

    private final String label;

    IncomeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
