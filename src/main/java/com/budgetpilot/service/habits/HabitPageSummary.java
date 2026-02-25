package com.budgetpilot.service.habits;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class HabitPageSummary {
    private final YearMonth month;
    private final BigDecimal habitTrackedSpend;
    private final int activeRulesCount;
    private final int warningCount;
    private final int exceededCount;
    private final int onTrackCount;
    private final List<HabitSpendSummary> evaluations;
    private final List<HabitInsight> insights;

    public HabitPageSummary(
            YearMonth month,
            BigDecimal habitTrackedSpend,
            int activeRulesCount,
            int warningCount,
            int exceededCount,
            int onTrackCount,
            List<HabitSpendSummary> evaluations,
            List<HabitInsight> insights
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.habitTrackedSpend = Objects.requireNonNull(habitTrackedSpend, "habitTrackedSpend");
        this.activeRulesCount = activeRulesCount;
        this.warningCount = warningCount;
        this.exceededCount = exceededCount;
        this.onTrackCount = onTrackCount;
        this.evaluations = List.copyOf(evaluations == null ? List.of() : evaluations);
        this.insights = List.copyOf(insights == null ? List.of() : insights);
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getHabitTrackedSpend() {
        return habitTrackedSpend;
    }

    public int getActiveRulesCount() {
        return activeRulesCount;
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

    public List<HabitSpendSummary> getEvaluations() {
        return evaluations;
    }

    public List<HabitInsight> getInsights() {
        return insights;
    }
}
