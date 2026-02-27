package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.Investment;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.util.ValidationUtils;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryStore implements BudgetStore, FullDataStore {
    private static final Comparator<IncomeEntry> INCOME_SORT =
            Comparator.comparing(IncomeEntry::getReceivedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(IncomeEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<ExpenseEntry> EXPENSE_SORT =
            Comparator.comparing(ExpenseEntry::getExpenseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ExpenseEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<SavingsEntry> SAVINGS_ENTRY_SORT =
            Comparator.comparing(SavingsEntry::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SavingsEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<GoalContribution> GOAL_CONTRIBUTION_SORT =
            Comparator.comparing(GoalContribution::getContributionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(GoalContribution::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<FamilyExpenseEntry> FAMILY_EXPENSE_SORT =
            Comparator.comparing(FamilyExpenseEntry::getExpenseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(FamilyExpenseEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<InvestmentTransaction> INVESTMENT_TRANSACTION_SORT =
            Comparator.comparing(InvestmentTransaction::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(InvestmentTransaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private UserProfile userProfile;
    private final Map<YearMonth, MonthlyPlan> monthlyPlans = new LinkedHashMap<>();
    private final Map<String, IncomeEntry> incomes = new LinkedHashMap<>();
    private final Map<String, ExpenseEntry> expenses = new LinkedHashMap<>();
    private final Map<String, SavingsBucket> savingsBuckets = new LinkedHashMap<>();
    private final Map<String, SavingsEntry> savingsEntries = new LinkedHashMap<>();
    private final Map<String, Goal> goals = new LinkedHashMap<>();
    private final Map<String, GoalContribution> goalContributions = new LinkedHashMap<>();
    private final Map<String, FamilyMember> familyMembers = new LinkedHashMap<>();
    private final Map<String, FamilyExpenseEntry> familyExpenses = new LinkedHashMap<>();
    private final Map<String, HabitRule> habitRules = new LinkedHashMap<>();
    private final Map<String, ExpenseTemplate> expenseTemplates = new LinkedHashMap<>();
    private final Map<String, IncomeTemplate> incomeTemplates = new LinkedHashMap<>();
    private final Map<String, Investment> investments = new LinkedHashMap<>();
    private final Map<String, InvestmentTransaction> investmentTransactions = new LinkedHashMap<>();
    private final Map<String, String> appSettings = new LinkedHashMap<>();

    @Override
    public synchronized UserProfile getUserProfile() {
        return userProfile == null ? null : userProfile.copy();
    }

    @Override
    public synchronized void saveUserProfile(UserProfile profile) {
        ValidationUtils.requireNonNull(profile, "profile");
        this.userProfile = profile.copy();
    }

    @Override
    public synchronized MonthlyPlan getMonthlyPlan(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        MonthlyPlan plan = monthlyPlans.get(normalizedMonth);
        return plan == null ? null : plan.copy();
    }

    @Override
    public synchronized void saveMonthlyPlan(MonthlyPlan plan) {
        ValidationUtils.requireNonNull(plan, "plan");
        MonthlyPlan copy = plan.copy();
        monthlyPlans.put(copy.getMonth(), copy);
    }

    @Override
    public synchronized List<IncomeEntry> listIncomeEntries(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<IncomeEntry> results = incomes.values().stream()
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(INCOME_SORT)
                .map(IncomeEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveIncomeEntry(IncomeEntry entry) {
        ValidationUtils.requireNonNull(entry, "entry");
        IncomeEntry copy = entry.copy();
        incomes.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteIncomeEntry(String id) {
        incomes.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<ExpenseEntry> listExpenseEntries(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<ExpenseEntry> results = expenses.values().stream()
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(EXPENSE_SORT)
                .map(ExpenseEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveExpenseEntry(ExpenseEntry entry) {
        ValidationUtils.requireNonNull(entry, "entry");
        ExpenseEntry copy = normalizeExpenseEntry(entry.copy());
        expenses.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteExpenseEntry(String id) {
        expenses.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<SavingsBucket> listSavingsBuckets() {
        List<SavingsBucket> results = new ArrayList<>();
        for (SavingsBucket bucket : savingsBuckets.values()) {
            results.add(bucket.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveSavingsBucket(SavingsBucket bucket) {
        ValidationUtils.requireNonNull(bucket, "bucket");
        SavingsBucket copy = bucket.copy();
        savingsBuckets.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteSavingsBucket(String id) {
        savingsBuckets.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<SavingsEntry> listSavingsEntries(String bucketId) {
        String normalizedBucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        List<SavingsEntry> results = savingsEntries.values().stream()
                .filter(entry -> normalizedBucketId.equals(entry.getBucketId()))
                .sorted(SAVINGS_ENTRY_SORT)
                .map(SavingsEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<SavingsEntry> listSavingsEntries(String bucketId, YearMonth month) {
        String normalizedBucketId = ValidationUtils.requireNonBlank(bucketId, "bucketId");
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<SavingsEntry> results = savingsEntries.values().stream()
                .filter(entry -> normalizedBucketId.equals(entry.getBucketId()))
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(SAVINGS_ENTRY_SORT)
                .map(SavingsEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<SavingsEntry> listAllSavingsEntries(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<SavingsEntry> results = savingsEntries.values().stream()
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(SAVINGS_ENTRY_SORT)
                .map(SavingsEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveSavingsEntry(SavingsEntry entry) {
        ValidationUtils.requireNonNull(entry, "entry");
        SavingsEntry copy = entry.copy();
        savingsEntries.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteSavingsEntry(String id) {
        savingsEntries.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<Goal> listGoals() {
        List<Goal> results = new ArrayList<>();
        for (Goal goal : goals.values()) {
            results.add(goal.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveGoal(Goal goal) {
        ValidationUtils.requireNonNull(goal, "goal");
        Goal copy = goal.copy();
        goals.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteGoal(String id) {
        goals.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<GoalContribution> listGoalContributions(String goalId) {
        String normalizedGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        List<GoalContribution> results = goalContributions.values().stream()
                .filter(entry -> normalizedGoalId.equals(entry.getGoalId()))
                .sorted(GOAL_CONTRIBUTION_SORT)
                .map(GoalContribution::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<GoalContribution> listGoalContributions(String goalId, YearMonth month) {
        String normalizedGoalId = ValidationUtils.requireNonBlank(goalId, "goalId");
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<GoalContribution> results = goalContributions.values().stream()
                .filter(entry -> normalizedGoalId.equals(entry.getGoalId()))
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(GOAL_CONTRIBUTION_SORT)
                .map(GoalContribution::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<GoalContribution> listAllGoalContributions(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<GoalContribution> results = goalContributions.values().stream()
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(GOAL_CONTRIBUTION_SORT)
                .map(GoalContribution::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveGoalContribution(GoalContribution entry) {
        ValidationUtils.requireNonNull(entry, "entry");
        GoalContribution copy = entry.copy();
        goalContributions.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteGoalContribution(String id) {
        goalContributions.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<FamilyMember> listFamilyMembers() {
        List<FamilyMember> results = new ArrayList<>();
        for (FamilyMember member : familyMembers.values()) {
            results.add(member.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveFamilyMember(FamilyMember member) {
        ValidationUtils.requireNonNull(member, "member");
        FamilyMember copy = member.copy();
        familyMembers.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteFamilyMember(String id) {
        familyMembers.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<FamilyExpenseEntry> listFamilyExpenseEntries(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<FamilyExpenseEntry> results = familyExpenses.values().stream()
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(FAMILY_EXPENSE_SORT)
                .map(FamilyExpenseEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<FamilyExpenseEntry> listFamilyExpenseEntriesForMember(String familyMemberId) {
        String normalizedMemberId = ValidationUtils.requireNonBlank(familyMemberId, "familyMemberId");
        List<FamilyExpenseEntry> results = familyExpenses.values().stream()
                .filter(entry -> normalizedMemberId.equals(entry.getFamilyMemberId()))
                .sorted(FAMILY_EXPENSE_SORT)
                .map(FamilyExpenseEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<FamilyExpenseEntry> listFamilyExpenseEntriesForMember(String familyMemberId, YearMonth month) {
        String normalizedMemberId = ValidationUtils.requireNonBlank(familyMemberId, "familyMemberId");
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<FamilyExpenseEntry> results = familyExpenses.values().stream()
                .filter(entry -> normalizedMemberId.equals(entry.getFamilyMemberId()))
                .filter(entry -> normalizedMonth.equals(entry.getMonth()))
                .sorted(FAMILY_EXPENSE_SORT)
                .map(FamilyExpenseEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveFamilyExpenseEntry(FamilyExpenseEntry entry) {
        ValidationUtils.requireNonNull(entry, "entry");
        FamilyExpenseEntry copy = entry.copy();
        familyExpenses.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteFamilyExpenseEntry(String id) {
        familyExpenses.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<HabitRule> listHabitRules() {
        List<HabitRule> results = new ArrayList<>();
        for (HabitRule rule : habitRules.values()) {
            results.add(rule.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveHabitRule(HabitRule rule) {
        ValidationUtils.requireNonNull(rule, "rule");
        HabitRule copy = rule.copy();
        habitRules.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteHabitRule(String id) {
        habitRules.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<ExpenseTemplate> listExpenseTemplates() {
        List<ExpenseTemplate> results = new ArrayList<>();
        for (ExpenseTemplate template : expenseTemplates.values()) {
            results.add(template.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveExpenseTemplate(ExpenseTemplate template) {
        ValidationUtils.requireNonNull(template, "template");
        ExpenseTemplate copy = template.copy();
        expenseTemplates.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteExpenseTemplate(String id) {
        expenseTemplates.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<IncomeTemplate> listIncomeTemplates() {
        List<IncomeTemplate> results = new ArrayList<>();
        for (IncomeTemplate template : incomeTemplates.values()) {
            results.add(template.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveIncomeTemplate(IncomeTemplate template) {
        ValidationUtils.requireNonNull(template, "template");
        IncomeTemplate copy = template.copy();
        incomeTemplates.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteIncomeTemplate(String id) {
        incomeTemplates.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<Investment> listInvestments() {
        List<Investment> results = new ArrayList<>();
        for (Investment investment : investments.values()) {
            results.add(investment.copy());
        }
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveInvestment(Investment investment) {
        ValidationUtils.requireNonNull(investment, "investment");
        Investment copy = investment.copy();
        investments.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteInvestment(String id) {
        investments.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized List<InvestmentTransaction> listInvestmentTransactions(String investmentId) {
        String normalizedInvestmentId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        List<InvestmentTransaction> results = investmentTransactions.values().stream()
                .filter(tx -> normalizedInvestmentId.equals(tx.getInvestmentId()))
                .sorted(INVESTMENT_TRANSACTION_SORT)
                .map(InvestmentTransaction::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<InvestmentTransaction> listInvestmentTransactions(String investmentId, YearMonth month) {
        String normalizedInvestmentId = ValidationUtils.requireNonBlank(investmentId, "investmentId");
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<InvestmentTransaction> results = investmentTransactions.values().stream()
                .filter(tx -> normalizedInvestmentId.equals(tx.getInvestmentId()))
                .filter(tx -> normalizedMonth.equals(tx.getMonth()))
                .sorted(INVESTMENT_TRANSACTION_SORT)
                .map(InvestmentTransaction::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<InvestmentTransaction> listAllInvestmentTransactions(YearMonth month) {
        YearMonth normalizedMonth = ValidationUtils.requireNonNull(month, "month");
        List<InvestmentTransaction> results = investmentTransactions.values().stream()
                .filter(tx -> normalizedMonth.equals(tx.getMonth()))
                .sorted(INVESTMENT_TRANSACTION_SORT)
                .map(InvestmentTransaction::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized void saveInvestmentTransaction(InvestmentTransaction tx) {
        ValidationUtils.requireNonNull(tx, "tx");
        InvestmentTransaction copy = tx.copy();
        investmentTransactions.put(copy.getId(), copy);
    }

    @Override
    public synchronized void deleteInvestmentTransaction(String id) {
        investmentTransactions.remove(ValidationUtils.requireNonBlank(id, "id"));
    }

    @Override
    public synchronized void purgeMonthsBefore(YearMonth cutoff) {
        YearMonth normalizedCutoff = ValidationUtils.requireNonNull(cutoff, "cutoff");

        incomes.values().removeIf(entry -> isOlderThanCutoff(entry.getMonth(), normalizedCutoff));
        expenses.values().removeIf(entry -> isOlderThanCutoff(entry.getMonth(), normalizedCutoff));
        savingsEntries.values().removeIf(entry -> isOlderThanCutoff(entry.getMonth(), normalizedCutoff));
        goalContributions.values().removeIf(entry -> isOlderThanCutoff(entry.getMonth(), normalizedCutoff));
        familyExpenses.values().removeIf(entry -> isOlderThanCutoff(entry.getMonth(), normalizedCutoff));
        investmentTransactions.values().removeIf(entry -> isOlderThanCutoff(entry.getMonth(), normalizedCutoff));
    }

    @Override
    public synchronized void clearAll() {
        userProfile = null;
        monthlyPlans.clear();
        incomes.clear();
        expenses.clear();
        savingsBuckets.clear();
        savingsEntries.clear();
        goals.clear();
        goalContributions.clear();
        familyMembers.clear();
        familyExpenses.clear();
        habitRules.clear();
        expenseTemplates.clear();
        incomeTemplates.clear();
        investments.clear();
        investmentTransactions.clear();
        appSettings.clear();
    }

    @Override
    public synchronized List<MonthlyPlan> listMonthlyPlans() {
        List<MonthlyPlan> results = monthlyPlans.values().stream()
                .map(MonthlyPlan::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<IncomeEntry> listAllIncomeEntries() {
        List<IncomeEntry> results = incomes.values().stream()
                .sorted(INCOME_SORT)
                .map(IncomeEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<ExpenseEntry> listAllExpenseEntries() {
        List<ExpenseEntry> results = expenses.values().stream()
                .sorted(EXPENSE_SORT)
                .map(ExpenseEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    private ExpenseEntry normalizeExpenseEntry(ExpenseEntry entry) {
        if (entry.getPlannerBucket() == null) {
            entry.setPlannerBucket(PlannerBucket.inferFromCategory(entry.getCategory()));
        }
        return entry;
    }

    @Override
    public synchronized List<SavingsEntry> listAllSavingsEntries() {
        List<SavingsEntry> results = savingsEntries.values().stream()
                .sorted(SAVINGS_ENTRY_SORT)
                .map(SavingsEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<GoalContribution> listAllGoalContributions() {
        List<GoalContribution> results = goalContributions.values().stream()
                .sorted(GOAL_CONTRIBUTION_SORT)
                .map(GoalContribution::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<FamilyExpenseEntry> listAllFamilyExpenseEntries() {
        List<FamilyExpenseEntry> results = familyExpenses.values().stream()
                .sorted(FAMILY_EXPENSE_SORT)
                .map(FamilyExpenseEntry::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized List<InvestmentTransaction> listAllInvestmentTransactions() {
        List<InvestmentTransaction> results = investmentTransactions.values().stream()
                .sorted(INVESTMENT_TRANSACTION_SORT)
                .map(InvestmentTransaction::copy)
                .toList();
        return List.copyOf(results);
    }

    @Override
    public synchronized Map<String, String> listAppSettings() {
        return Map.copyOf(appSettings);
    }

    @Override
    public synchronized String getAppSetting(String key) {
        return appSettings.get(ValidationUtils.requireNonBlank(key, "key"));
    }

    @Override
    public synchronized void saveAppSetting(String key, String value) {
        appSettings.put(
                ValidationUtils.requireNonBlank(key, "key"),
                value == null ? "" : value.trim()
        );
    }

    @Override
    public synchronized void deleteAppSetting(String key) {
        appSettings.remove(ValidationUtils.requireNonBlank(key, "key"));
    }

    private boolean isOlderThanCutoff(YearMonth month, YearMonth cutoff) {
        return month != null && month.isBefore(cutoff);
    }
}
