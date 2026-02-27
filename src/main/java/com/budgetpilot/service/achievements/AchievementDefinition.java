package com.budgetpilot.service.achievements;

import com.budgetpilot.model.enums.AchievementCategory;
import com.budgetpilot.model.enums.AchievementTier;

import java.math.BigDecimal;
import java.util.Objects;

public class AchievementDefinition {
    private final String code;
    private final String title;
    private final String description;
    private final AchievementCategory category;
    private final AchievementTier tier;
    private final BigDecimal targetValue;
    private final String unlockHint;

    public AchievementDefinition(
            String code,
            String title,
            String description,
            AchievementCategory category,
            AchievementTier tier,
            BigDecimal targetValue,
            String unlockHint
    ) {
        this.code = Objects.requireNonNull(code, "code");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.tier = Objects.requireNonNull(tier, "tier");
        this.targetValue = Objects.requireNonNull(targetValue, "targetValue");
        this.unlockHint = unlockHint == null ? "" : unlockHint;
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

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public String getUnlockHint() {
        return unlockHint;
    }
}
