package com.budgetpilot.model.enums;

public enum RelationshipType {
    CHILD("Child"),
    PARENT("Parent"),
    GRANDPARENT("Grandparent"),
    SIBLING("Sibling"),
    SPOUSE("Spouse"),
    OTHER("Other");

    private final String label;

    RelationshipType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
