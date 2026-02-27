package com.budgetpilot.service.backup;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.Investment;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.UserProfile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BackupSnapshot {
    private String appVersion;
    private int schemaVersion;
    private LocalDateTime exportedAt;
    private Map<String, String> appSettings = new LinkedHashMap<>();

    private UserProfile userProfile;
    private List<MonthlyPlan> monthlyPlans = new ArrayList<>();
    private List<IncomeEntry> incomeEntries = new ArrayList<>();
    private List<ExpenseEntry> expenseEntries = new ArrayList<>();
    private List<SavingsBucket> savingsBuckets = new ArrayList<>();
    private List<SavingsEntry> savingsEntries = new ArrayList<>();
    private List<Goal> goals = new ArrayList<>();
    private List<GoalContribution> goalContributions = new ArrayList<>();
    private List<FamilyMember> familyMembers = new ArrayList<>();
    private List<FamilyExpenseEntry> familyExpenseEntries = new ArrayList<>();
    private List<HabitRule> habitRules = new ArrayList<>();
    private List<Investment> investments = new ArrayList<>();
    private List<InvestmentTransaction> investmentTransactions = new ArrayList<>();

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }

    public Map<String, String> getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.appSettings = appSettings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(appSettings);
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public List<MonthlyPlan> getMonthlyPlans() {
        return monthlyPlans;
    }

    public void setMonthlyPlans(List<MonthlyPlan> monthlyPlans) {
        this.monthlyPlans = monthlyPlans == null ? new ArrayList<>() : new ArrayList<>(monthlyPlans);
    }

    public List<IncomeEntry> getIncomeEntries() {
        return incomeEntries;
    }

    public void setIncomeEntries(List<IncomeEntry> incomeEntries) {
        this.incomeEntries = incomeEntries == null ? new ArrayList<>() : new ArrayList<>(incomeEntries);
    }

    public List<ExpenseEntry> getExpenseEntries() {
        return expenseEntries;
    }

    public void setExpenseEntries(List<ExpenseEntry> expenseEntries) {
        this.expenseEntries = expenseEntries == null ? new ArrayList<>() : new ArrayList<>(expenseEntries);
    }

    public List<SavingsBucket> getSavingsBuckets() {
        return savingsBuckets;
    }

    public void setSavingsBuckets(List<SavingsBucket> savingsBuckets) {
        this.savingsBuckets = savingsBuckets == null ? new ArrayList<>() : new ArrayList<>(savingsBuckets);
    }

    public List<SavingsEntry> getSavingsEntries() {
        return savingsEntries;
    }

    public void setSavingsEntries(List<SavingsEntry> savingsEntries) {
        this.savingsEntries = savingsEntries == null ? new ArrayList<>() : new ArrayList<>(savingsEntries);
    }

    public List<Goal> getGoals() {
        return goals;
    }

    public void setGoals(List<Goal> goals) {
        this.goals = goals == null ? new ArrayList<>() : new ArrayList<>(goals);
    }

    public List<GoalContribution> getGoalContributions() {
        return goalContributions;
    }

    public void setGoalContributions(List<GoalContribution> goalContributions) {
        this.goalContributions = goalContributions == null ? new ArrayList<>() : new ArrayList<>(goalContributions);
    }

    public List<FamilyMember> getFamilyMembers() {
        return familyMembers;
    }

    public void setFamilyMembers(List<FamilyMember> familyMembers) {
        this.familyMembers = familyMembers == null ? new ArrayList<>() : new ArrayList<>(familyMembers);
    }

    public List<FamilyExpenseEntry> getFamilyExpenseEntries() {
        return familyExpenseEntries;
    }

    public void setFamilyExpenseEntries(List<FamilyExpenseEntry> familyExpenseEntries) {
        this.familyExpenseEntries = familyExpenseEntries == null ? new ArrayList<>() : new ArrayList<>(familyExpenseEntries);
    }

    public List<HabitRule> getHabitRules() {
        return habitRules;
    }

    public void setHabitRules(List<HabitRule> habitRules) {
        this.habitRules = habitRules == null ? new ArrayList<>() : new ArrayList<>(habitRules);
    }

    public List<Investment> getInvestments() {
        return investments;
    }

    public void setInvestments(List<Investment> investments) {
        this.investments = investments == null ? new ArrayList<>() : new ArrayList<>(investments);
    }

    public List<InvestmentTransaction> getInvestmentTransactions() {
        return investmentTransactions;
    }

    public void setInvestmentTransactions(List<InvestmentTransaction> investmentTransactions) {
        this.investmentTransactions = investmentTransactions == null ? new ArrayList<>() : new ArrayList<>(investmentTransactions);
    }
}
