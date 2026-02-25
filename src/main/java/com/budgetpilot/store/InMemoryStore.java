package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.util.ValidationUtils;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryStore implements BudgetStore {
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

    private UserProfile userProfile;
    private final Map<YearMonth, MonthlyPlan> monthlyPlans = new LinkedHashMap<>();
    private final Map<String, IncomeEntry> incomes = new LinkedHashMap<>();
    private final Map<String, ExpenseEntry> expenses = new LinkedHashMap<>();
    private final Map<String, SavingsBucket> savingsBuckets = new LinkedHashMap<>();
    private final Map<String, SavingsEntry> savingsEntries = new LinkedHashMap<>();
    private final Map<String, Goal> goals = new LinkedHashMap<>();
    private final Map<String, GoalContribution> goalContributions = new LinkedHashMap<>();
    private final Map<String, FamilyMember> familyMembers = new LinkedHashMap<>();
    private final Map<String, HabitRule> habitRules = new LinkedHashMap<>();

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
        ExpenseEntry copy = entry.copy();
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
        habitRules.clear();
    }
}
