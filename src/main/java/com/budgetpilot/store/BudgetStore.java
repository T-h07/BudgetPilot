package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.UserProfile;

import java.time.YearMonth;
import java.util.List;

public interface BudgetStore {
    UserProfile getUserProfile();

    void saveUserProfile(UserProfile profile);

    MonthlyPlan getMonthlyPlan(YearMonth month);

    void saveMonthlyPlan(MonthlyPlan plan);

    List<IncomeEntry> listIncomeEntries(YearMonth month);

    void saveIncomeEntry(IncomeEntry entry);

    void deleteIncomeEntry(String id);

    List<ExpenseEntry> listExpenseEntries(YearMonth month);

    void saveExpenseEntry(ExpenseEntry entry);

    void deleteExpenseEntry(String id);

    List<SavingsBucket> listSavingsBuckets();

    void saveSavingsBucket(SavingsBucket bucket);

    void deleteSavingsBucket(String id);

    List<SavingsEntry> listSavingsEntries(String bucketId);

    List<SavingsEntry> listSavingsEntries(String bucketId, YearMonth month);

    List<SavingsEntry> listAllSavingsEntries(YearMonth month);

    void saveSavingsEntry(SavingsEntry entry);

    void deleteSavingsEntry(String id);

    List<Goal> listGoals();

    void saveGoal(Goal goal);

    void deleteGoal(String id);

    List<GoalContribution> listGoalContributions(String goalId);

    List<GoalContribution> listGoalContributions(String goalId, YearMonth month);

    List<GoalContribution> listAllGoalContributions(YearMonth month);

    void saveGoalContribution(GoalContribution entry);

    void deleteGoalContribution(String id);

    List<FamilyMember> listFamilyMembers();

    void saveFamilyMember(FamilyMember member);

    void deleteFamilyMember(String id);

    List<FamilyExpenseEntry> listFamilyExpenseEntries(YearMonth month);

    List<FamilyExpenseEntry> listFamilyExpenseEntriesForMember(String familyMemberId);

    List<FamilyExpenseEntry> listFamilyExpenseEntriesForMember(String familyMemberId, YearMonth month);

    void saveFamilyExpenseEntry(FamilyExpenseEntry entry);

    void deleteFamilyExpenseEntry(String id);

    List<HabitRule> listHabitRules();

    void saveHabitRule(HabitRule rule);

    void deleteHabitRule(String id);

    void clearAll();
}
