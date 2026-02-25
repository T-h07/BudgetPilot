package com.budgetpilot.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class FamilySummary {
    private final YearMonth month;
    private final BigDecimal totalFamilyCosts;
    private final BigDecimal totalAllowances;
    private final BigDecimal totalMedicalCosts;
    private final BigDecimal totalSupportCosts;
    private final BigDecimal totalSchoolCosts;
    private final BigDecimal totalExtraCosts;
    private final BigDecimal totalEmergencyCosts;
    private final int activeMembersCount;
    private final int memberCount;
    private final BigDecimal plannedFamilyBudget;
    private final BigDecimal familyBudgetVariance;
    private final String budgetStatusMessage;
    private final List<FamilyExpenseSummary> expenseBreakdown;

    public FamilySummary(
            YearMonth month,
            BigDecimal totalFamilyCosts,
            BigDecimal totalAllowances,
            BigDecimal totalMedicalCosts,
            BigDecimal totalSupportCosts,
            BigDecimal totalSchoolCosts,
            BigDecimal totalExtraCosts,
            BigDecimal totalEmergencyCosts,
            int activeMembersCount,
            int memberCount,
            BigDecimal plannedFamilyBudget,
            BigDecimal familyBudgetVariance,
            String budgetStatusMessage,
            List<FamilyExpenseSummary> expenseBreakdown
    ) {
        this.month = Objects.requireNonNull(month, "month");
        this.totalFamilyCosts = Objects.requireNonNull(totalFamilyCosts, "totalFamilyCosts");
        this.totalAllowances = Objects.requireNonNull(totalAllowances, "totalAllowances");
        this.totalMedicalCosts = Objects.requireNonNull(totalMedicalCosts, "totalMedicalCosts");
        this.totalSupportCosts = Objects.requireNonNull(totalSupportCosts, "totalSupportCosts");
        this.totalSchoolCosts = Objects.requireNonNull(totalSchoolCosts, "totalSchoolCosts");
        this.totalExtraCosts = Objects.requireNonNull(totalExtraCosts, "totalExtraCosts");
        this.totalEmergencyCosts = Objects.requireNonNull(totalEmergencyCosts, "totalEmergencyCosts");
        this.activeMembersCount = activeMembersCount;
        this.memberCount = memberCount;
        this.plannedFamilyBudget = Objects.requireNonNull(plannedFamilyBudget, "plannedFamilyBudget");
        this.familyBudgetVariance = Objects.requireNonNull(familyBudgetVariance, "familyBudgetVariance");
        this.budgetStatusMessage = Objects.requireNonNull(budgetStatusMessage, "budgetStatusMessage");
        this.expenseBreakdown = List.copyOf(expenseBreakdown == null ? List.of() : expenseBreakdown);
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalFamilyCosts() {
        return totalFamilyCosts;
    }

    public BigDecimal getTotalAllowances() {
        return totalAllowances;
    }

    public BigDecimal getTotalMedicalCosts() {
        return totalMedicalCosts;
    }

    public BigDecimal getTotalSupportCosts() {
        return totalSupportCosts;
    }

    public BigDecimal getTotalSchoolCosts() {
        return totalSchoolCosts;
    }

    public BigDecimal getTotalExtraCosts() {
        return totalExtraCosts;
    }

    public BigDecimal getTotalEmergencyCosts() {
        return totalEmergencyCosts;
    }

    public int getActiveMembersCount() {
        return activeMembersCount;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public BigDecimal getPlannedFamilyBudget() {
        return plannedFamilyBudget;
    }

    public BigDecimal getFamilyBudgetVariance() {
        return familyBudgetVariance;
    }

    public String getBudgetStatusMessage() {
        return budgetStatusMessage;
    }

    public List<FamilyExpenseSummary> getExpenseBreakdown() {
        return expenseBreakdown;
    }
}
