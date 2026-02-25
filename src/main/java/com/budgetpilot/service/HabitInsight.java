package com.budgetpilot.service;

import com.budgetpilot.model.enums.HabitSeverity;

import java.util.Objects;

public class HabitInsight {
    private final String id;
    private final HabitSeverity severity;
    private final String title;
    private final String message;
    private final String actionHint;

    public HabitInsight(String id, HabitSeverity severity, String title, String message, String actionHint) {
        this.id = Objects.requireNonNull(id, "id");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
        this.actionHint = actionHint == null ? "" : actionHint;
    }

    public String getId() {
        return id;
    }

    public HabitSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getActionHint() {
        return actionHint;
    }
}
