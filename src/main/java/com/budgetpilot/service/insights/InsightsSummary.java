package com.budgetpilot.service.insights;

import com.budgetpilot.util.ValidationUtils;

import java.time.YearMonth;
import java.util.List;

public class InsightsSummary {
    private final YearMonth month;
    private final List<InsightItem> items;
    private final int dangerCount;
    private final int warningCount;
    private final int infoCount;
    private final int successCount;

    public InsightsSummary(
            YearMonth month,
            List<InsightItem> items,
            int dangerCount,
            int warningCount,
            int infoCount,
            int successCount
    ) {
        this.month = ValidationUtils.requireNonNull(month, "month");
        this.items = List.copyOf(items == null ? List.of() : items);
        this.dangerCount = Math.max(0, dangerCount);
        this.warningCount = Math.max(0, warningCount);
        this.infoCount = Math.max(0, infoCount);
        this.successCount = Math.max(0, successCount);
    }

    public YearMonth getMonth() {
        return month;
    }

    public List<InsightItem> getItems() {
        return items;
    }

    public int getDangerCount() {
        return dangerCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getActionableCount() {
        return dangerCount + warningCount;
    }

    public int getTotalCount() {
        return items.size();
    }
}
