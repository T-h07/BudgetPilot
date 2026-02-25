package com.budgetpilot.model.enums;

public enum PlannerBucket {
    FIXED_COSTS("Fixed Costs"),
    FOOD("Food"),
    TRANSPORT("Transport"),
    FAMILY("Family"),
    DISCRETIONARY("Discretionary"),
    UNPLANNED("Unplanned (One-time)");

    private final String displayName;

    PlannerBucket(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PlannerBucket inferFromCategory(ExpenseCategory category) {
        if (category == null) {
            return DISCRETIONARY;
        }
        return switch (category) {
            case FOOD -> FOOD;
            case BILLS, SUBSCRIPTIONS -> FIXED_COSTS;
            case CAR -> TRANSPORT;
            case FAMILY -> FAMILY;
            case HEALTH, EDUCATION, CLOTHES, ENTERTAINMENT, OTHER -> DISCRETIONARY;
        };
    }
}
