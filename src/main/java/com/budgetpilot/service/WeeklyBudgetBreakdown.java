package com.budgetpilot.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class WeeklyBudgetBreakdown {
    private final YearMonth month;
    private final List<WeekAllocation> allocations;

    public WeeklyBudgetBreakdown(YearMonth month, List<WeekAllocation> allocations) {
        this.month = Objects.requireNonNull(month, "month");
        this.allocations = List.copyOf(allocations == null ? List.of() : allocations);
    }

    public YearMonth getMonth() {
        return month;
    }

    public List<WeekAllocation> getAllocations() {
        return allocations;
    }

    public int getWeekCount() {
        return allocations.size();
    }
}
