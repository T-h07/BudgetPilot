package com.budgetpilot.service.dashboard;

import java.util.Objects;

public class BudgetHealthScore {
    private final int score;
    private final String label;
    private final String message;

    public BudgetHealthScore(int score, String label, String message) {
        this.score = score;
        this.label = Objects.requireNonNull(label, "label");
        this.message = Objects.requireNonNull(message, "message");
    }

    public int getScore() {
        return score;
    }

    public String getLabel() {
        return label;
    }

    public String getMessage() {
        return message;
    }
}
