package com.budgetpilot.service;

import com.budgetpilot.model.enums.AchievementCategory;
import com.budgetpilot.model.enums.AchievementStatus;
import com.budgetpilot.model.enums.AchievementTier;

import java.math.BigDecimal;
import java.util.Objects;

public class AchievementProgress {
    private final String code;
    private final String title;
    private final String description;
    private final AchievementCategory category;
    private final AchievementTier tier;
    private final AchievementStatus status;
    private final BigDecimal progressCurrent;
    private final BigDecimal progressTarget;
    private final BigDecimal progressPercent;
    private final String progressText;
    private final String unlockHint;
    private final boolean hidden;
    private final String badgeLabel;

    public AchievementProgress(
            String code,
            String title,
            String description,
            AchievementCategory category,
            AchievementTier tier,
            AchievementStatus status,
            BigDecimal progressCurrent,
            BigDecimal progressTarget,
            BigDecimal progressPercent,
            String progressText,
            String unlockHint,
            boolean hidden,
            String badgeLabel
    ) {
        this.code = Objects.requireNonNull(code, "code");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.tier = Objects.requireNonNull(tier, "tier");
        this.status = Objects.requireNonNull(status, "status");
        this.progressCurrent = Objects.requireNonNull(progressCurrent, "progressCurrent");
        this.progressTarget = Objects.requireNonNull(progressTarget, "progressTarget");
        this.progressPercent = Objects.requireNonNull(progressPercent, "progressPercent");
        this.progressText = Objects.requireNonNull(progressText, "progressText");
        this.unlockHint = unlockHint == null ? "" : unlockHint;
        this.hidden = hidden;
        this.badgeLabel = badgeLabel == null ? "" : badgeLabel;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public AchievementTier getTier() {
        return tier;
    }

    public AchievementStatus getStatus() {
        return status;
    }

    public BigDecimal getProgressCurrent() {
        return progressCurrent;
    }

    public BigDecimal getProgressTarget() {
        return progressTarget;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public String getProgressText() {
        return progressText;
    }

    public String getUnlockHint() {
        return unlockHint;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }
}
