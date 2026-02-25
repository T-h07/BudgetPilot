package com.budgetpilot.service;

import java.util.Objects;

public class AchievementInsight {
    private final String id;
    private final String title;
    private final String message;
    private final InsightLevel level;
    private final String actionHint;

    public AchievementInsight(String id, String title, String message, InsightLevel level, String actionHint) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
        this.level = Objects.requireNonNull(level, "level");
        this.actionHint = actionHint == null ? "" : actionHint;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public InsightLevel getLevel() {
        return level;
    }

    public String getActionHint() {
        return actionHint;
    }
}
