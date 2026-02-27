package com.budgetpilot.service.achievements;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class AchievementPageSummary {
    private final YearMonth month;
    private final int totalAchievements;
    private final int unlockedCount;
    private final int inProgressCount;
    private final int lockedCount;
    private final int bronzeUnlocked;
    private final int silverUnlocked;
    private final int goldUnlocked;
    private final BigDecimal completionPercent;
    private final List<AchievementProgress> achievements;

    public AchievementPageSummary(
            YearMonth month,
            int totalAchievements,
            int unlockedCount,
            int inProgressCount,
            int lockedCount,
            int bronzeUnlocked,
            int silverUnlocked,
            int goldUnlocked,
            BigDecimal completionPercent,
            List<AchievementProgress> achievements
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.totalAchievements = totalAchievements;
        this.unlockedCount = unlockedCount;
        this.inProgressCount = inProgressCount;
        this.lockedCount = lockedCount;
        this.bronzeUnlocked = bronzeUnlocked;
        this.silverUnlocked = silverUnlocked;
        this.goldUnlocked = goldUnlocked;
        this.completionPercent = Objects.requireNonNull(completionPercent, "completionPercent");
        this.achievements = List.copyOf(achievements == null ? List.of() : achievements);
    }

    public YearMonth getMonth() {
        return month;
    }

    public int getTotalAchievements() {
        return totalAchievements;
    }

    public int getUnlockedCount() {
        return unlockedCount;
    }

    public int getInProgressCount() {
        return inProgressCount;
    }

    public int getLockedCount() {
        return lockedCount;
    }

    public int getBronzeUnlocked() {
        return bronzeUnlocked;
    }

    public int getSilverUnlocked() {
        return silverUnlocked;
    }

    public int getGoldUnlocked() {
        return goldUnlocked;
    }

    public BigDecimal getCompletionPercent() {
        return completionPercent;
    }

    public List<AchievementProgress> getAchievements() {
        return achievements;
    }
}
