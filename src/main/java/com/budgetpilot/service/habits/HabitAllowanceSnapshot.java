package com.budgetpilot.service.habits;

import com.budgetpilot.model.enums.HabitAllowanceMode;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class HabitAllowanceSnapshot {
    private final YearMonth month;
    private final BigDecimal habitPercent;
    private final HabitAllowanceMode habitMode;
    private final BigDecimal availableAfterReserve;
    private final BigDecimal habitPoolAmount;
    private final BigDecimal spentAcrossHabits;
    private final BigDecimal excessAcrossHabits;
    private final BigDecimal remainingPool;
    private final int daysElapsed;
    private final int daysInMonth;
    private final BigDecimal paceRatio;
    private final int totalWeight;
    private final int enabledHabitsCount;
    private final int warningCount;
    private final int exceededCount;
    private final int onTrackCount;
    private final boolean onlyDiscretionaryAndUnplanned;
    private final boolean poolAdjustedDown;
    private final List<HabitAllowanceRow> rows;

    public HabitAllowanceSnapshot(
            YearMonth month,
            BigDecimal habitPercent,
            HabitAllowanceMode habitMode,
            BigDecimal availableAfterReserve,
            BigDecimal habitPoolAmount,
            BigDecimal spentAcrossHabits,
            BigDecimal excessAcrossHabits,
            BigDecimal remainingPool,
            int daysElapsed,
            int daysInMonth,
            BigDecimal paceRatio,
            int totalWeight,
            int enabledHabitsCount,
            int warningCount,
            int exceededCount,
            int onTrackCount,
            boolean onlyDiscretionaryAndUnplanned,
            boolean poolAdjustedDown,
            List<HabitAllowanceRow> rows
    ) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.habitPercent = Objects.requireNonNull(habitPercent, "habitPercent");
        this.habitMode = ValidationUtils.requireNonNull(habitMode, "habitMode");
        this.availableAfterReserve = Objects.requireNonNull(availableAfterReserve, "availableAfterReserve");
        this.habitPoolAmount = Objects.requireNonNull(habitPoolAmount, "habitPoolAmount");
        this.spentAcrossHabits = Objects.requireNonNull(spentAcrossHabits, "spentAcrossHabits");
        this.excessAcrossHabits = Objects.requireNonNull(excessAcrossHabits, "excessAcrossHabits");
        this.remainingPool = Objects.requireNonNull(remainingPool, "remainingPool");
        this.daysElapsed = Math.max(1, daysElapsed);
        this.daysInMonth = Math.max(1, daysInMonth);
        this.paceRatio = Objects.requireNonNull(paceRatio, "paceRatio");
        this.totalWeight = Math.max(0, totalWeight);
        this.enabledHabitsCount = Math.max(0, enabledHabitsCount);
        this.warningCount = Math.max(0, warningCount);
        this.exceededCount = Math.max(0, exceededCount);
        this.onTrackCount = Math.max(0, onTrackCount);
        this.onlyDiscretionaryAndUnplanned = onlyDiscretionaryAndUnplanned;
        this.poolAdjustedDown = poolAdjustedDown;
        this.rows = List.copyOf(rows == null ? List.of() : rows);
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getHabitPercent() {
        return habitPercent;
    }

    public HabitAllowanceMode getHabitMode() {
        return habitMode;
    }

    public BigDecimal getAvailableAfterReserve() {
        return availableAfterReserve;
    }

    public BigDecimal getHabitPoolAmount() {
        return habitPoolAmount;
    }

    public BigDecimal getSpentAcrossHabits() {
        return spentAcrossHabits;
    }

    public BigDecimal getExcessAcrossHabits() {
        return excessAcrossHabits;
    }

    public BigDecimal getRemainingPool() {
        return remainingPool;
    }

    public int getDaysElapsed() {
        return daysElapsed;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public BigDecimal getPaceRatio() {
        return paceRatio;
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public int getEnabledHabitsCount() {
        return enabledHabitsCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getExceededCount() {
        return exceededCount;
    }

    public int getOnTrackCount() {
        return onTrackCount;
    }

    public boolean isOnlyDiscretionaryAndUnplanned() {
        return onlyDiscretionaryAndUnplanned;
    }

    public boolean isPoolAdjustedDown() {
        return poolAdjustedDown;
    }

    public List<HabitAllowanceRow> getRows() {
        return rows;
    }
}
