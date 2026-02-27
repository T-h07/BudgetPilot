package com.budgetpilot.service.habits;

import com.budgetpilot.model.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.util.Objects;

public class HabitSpendSummary {
    private final String ruleId;
    private final String tag;
    private final String displayName;
    private final ExpenseCategory linkedCategory;
    private final BigDecimal monthlyLimit;
    private final BigDecimal baselineAmount;
    private final BigDecimal warningThresholdPercent;
    private final BigDecimal actualSpend;
    private final BigDecimal excessSpent;
    private final BigDecimal capToDateAmount;
    private final BigDecimal remainingBeforeLimit;
    private final BigDecimal usagePercent;
    private final HabitStatus status;
    private final int matchedExpenseCount;
    private final boolean active;

    public HabitSpendSummary(
            String ruleId,
            String tag,
            String displayName,
            ExpenseCategory linkedCategory,
            BigDecimal monthlyLimit,
            BigDecimal baselineAmount,
            BigDecimal warningThresholdPercent,
            BigDecimal actualSpend,
            BigDecimal excessSpent,
            BigDecimal capToDateAmount,
            BigDecimal remainingBeforeLimit,
            BigDecimal usagePercent,
            HabitStatus status,
            int matchedExpenseCount,
            boolean active
    ) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.tag = Objects.requireNonNull(tag, "tag");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.linkedCategory = linkedCategory;
        this.monthlyLimit = Objects.requireNonNull(monthlyLimit, "monthlyLimit");
        this.baselineAmount = Objects.requireNonNull(baselineAmount, "baselineAmount");
        this.warningThresholdPercent = Objects.requireNonNull(warningThresholdPercent, "warningThresholdPercent");
        this.actualSpend = Objects.requireNonNull(actualSpend, "actualSpend");
        this.excessSpent = Objects.requireNonNull(excessSpent, "excessSpent");
        this.capToDateAmount = Objects.requireNonNull(capToDateAmount, "capToDateAmount");
        this.remainingBeforeLimit = Objects.requireNonNull(remainingBeforeLimit, "remainingBeforeLimit");
        this.usagePercent = Objects.requireNonNull(usagePercent, "usagePercent");
        this.status = Objects.requireNonNull(status, "status");
        this.matchedExpenseCount = matchedExpenseCount;
        this.active = active;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTag() {
        return tag;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ExpenseCategory getLinkedCategory() {
        return linkedCategory;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public BigDecimal getBaselineAmount() {
        return baselineAmount;
    }

    public BigDecimal getWarningThresholdPercent() {
        return warningThresholdPercent;
    }

    public BigDecimal getActualSpend() {
        return actualSpend;
    }

    public BigDecimal getExcessSpent() {
        return excessSpent;
    }

    public BigDecimal getCapToDateAmount() {
        return capToDateAmount;
    }

    public BigDecimal getRemainingBeforeLimit() {
        return remainingBeforeLimit;
    }

    public BigDecimal getUsagePercent() {
        return usagePercent;
    }

    public HabitStatus getStatus() {
        return status;
    }

    public int getMatchedExpenseCount() {
        return matchedExpenseCount;
    }

    public boolean isActive() {
        return active;
    }
}
