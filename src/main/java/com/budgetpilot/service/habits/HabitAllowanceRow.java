package com.budgetpilot.service.habits;

import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class HabitAllowanceRow {
    private final String ruleId;
    private final String displayName;
    private final String tag;
    private final ExpenseCategory linkedCategory;
    private final boolean active;
    private final boolean enabledThisMonth;
    private final int weight;
    private final BigDecimal hardLimitAmount;
    private final BigDecimal baselineAmount;
    private final BigDecimal allocatedCapAmount;
    private final BigDecimal finalCapAmount;
    private final BigDecimal spentAmount;
    private final BigDecimal excessSpentAmount;
    private final BigDecimal capToDateAmount;
    private final BigDecimal remainingAmount;
    private final BigDecimal usagePercent;
    private final HabitStatus status;
    private final int matchedExpenseCount;

    public HabitAllowanceRow(
            String ruleId,
            String displayName,
            String tag,
            ExpenseCategory linkedCategory,
            boolean active,
            boolean enabledThisMonth,
            int weight,
            BigDecimal hardLimitAmount,
            BigDecimal baselineAmount,
            BigDecimal allocatedCapAmount,
            BigDecimal finalCapAmount,
            BigDecimal spentAmount,
            BigDecimal excessSpentAmount,
            BigDecimal capToDateAmount,
            BigDecimal remainingAmount,
            BigDecimal usagePercent,
            HabitStatus status,
            int matchedExpenseCount
    ) {
        this.ruleId = ValidationUtils.requireNonBlank(ruleId, "ruleId");
        this.displayName = ValidationUtils.requireNonBlank(displayName, "displayName");
        this.tag = ValidationUtils.requireNonBlank(tag, "tag");
        this.linkedCategory = linkedCategory;
        this.active = active;
        this.enabledThisMonth = enabledThisMonth;
        this.weight = Math.max(1, Math.min(10, weight));
        this.hardLimitAmount = Objects.requireNonNull(hardLimitAmount, "hardLimitAmount");
        this.baselineAmount = Objects.requireNonNull(baselineAmount, "baselineAmount");
        this.allocatedCapAmount = Objects.requireNonNull(allocatedCapAmount, "allocatedCapAmount");
        this.finalCapAmount = Objects.requireNonNull(finalCapAmount, "finalCapAmount");
        this.spentAmount = Objects.requireNonNull(spentAmount, "spentAmount");
        this.excessSpentAmount = Objects.requireNonNull(excessSpentAmount, "excessSpentAmount");
        this.capToDateAmount = Objects.requireNonNull(capToDateAmount, "capToDateAmount");
        this.remainingAmount = Objects.requireNonNull(remainingAmount, "remainingAmount");
        this.usagePercent = Objects.requireNonNull(usagePercent, "usagePercent");
        this.status = Objects.requireNonNull(status, "status");
        this.matchedExpenseCount = Math.max(0, matchedExpenseCount);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTag() {
        return tag;
    }

    public ExpenseCategory getLinkedCategory() {
        return linkedCategory;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEnabledThisMonth() {
        return enabledThisMonth;
    }

    public int getWeight() {
        return weight;
    }

    public BigDecimal getHardLimitAmount() {
        return hardLimitAmount;
    }

    public BigDecimal getBaselineAmount() {
        return baselineAmount;
    }

    public BigDecimal getAllocatedCapAmount() {
        return allocatedCapAmount;
    }

    public BigDecimal getFinalCapAmount() {
        return finalCapAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public BigDecimal getExcessSpentAmount() {
        return excessSpentAmount;
    }

    public BigDecimal getCapToDateAmount() {
        return capToDateAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
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

    public boolean isEffectiveEnabled() {
        return active && enabledThisMonth;
    }
}
