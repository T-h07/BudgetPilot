package com.budgetpilot.service.dashboard;

import java.util.Objects;

public class DashboardAlert {
    private final String id;
    private final AlertLevel level;
    private final String title;
    private final String message;
    private final String source;
    private final String actionHint;

    public DashboardAlert(
            String id,
            AlertLevel level,
            String title,
            String message,
            String source,
            String actionHint
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.level = Objects.requireNonNull(level, "level");
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
        this.source = source == null ? "" : source;
        this.actionHint = actionHint == null ? "" : actionHint;
    }

    public String getId() {
        return id;
    }

    public AlertLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public String getActionHint() {
        return actionHint;
    }
}
