package com.budgetpilot.model.enums;

public enum UserProfileType {
    STUDENT("Student"),
    PERSONAL_USE("Personal Use"),
    MAIN_FAMILY_SUPPORTER("Main Family Supporter");

    private final String label;

    UserProfileType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
