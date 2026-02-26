package com.budgetpilot.service.insights;

import com.budgetpilot.core.PageId;
import com.budgetpilot.util.ValidationUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;

public class InsightItem {
    private final String id;
    private final InsightLevel level;
    private final String title;
    private final String message;
    private final String source;
    private final String actionLabel;
    private final PageId actionTarget;
    private final YearMonth month;
    private final LocalDateTime createdAt;
    private final boolean dismissible;

    public InsightItem(
            String id,
            InsightLevel level,
            String title,
            String message,
            String source,
            String actionLabel,
            PageId actionTarget,
            YearMonth month,
            LocalDateTime createdAt,
            boolean dismissible
    ) {
        this.id = ValidationUtils.requireNonBlank(id, "id");
        this.level = ValidationUtils.requireNonNull(level, "level");
        this.title = ValidationUtils.requireNonBlank(title, "title");
        this.message = ValidationUtils.requireNonBlank(message, "message");
        this.source = source == null ? "general" : source.trim().toLowerCase();
        this.actionLabel = actionLabel == null ? "" : actionLabel.trim();
        this.actionTarget = actionTarget;
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.dismissible = dismissible;
    }

    public String getId() {
        return id;
    }

    public InsightLevel getLevel() {
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

    public String getActionLabel() {
        return actionLabel;
    }

    public PageId getActionTarget() {
        return actionTarget;
    }

    public YearMonth getMonth() {
        return month;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isDismissible() {
        return dismissible;
    }
}
