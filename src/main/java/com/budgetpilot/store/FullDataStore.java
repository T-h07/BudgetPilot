package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsEntry;

import java.util.List;
import java.util.Map;

public interface FullDataStore {
    List<MonthlyPlan> listMonthlyPlans();

    List<IncomeEntry> listAllIncomeEntries();

    List<ExpenseEntry> listAllExpenseEntries();

    List<SavingsEntry> listAllSavingsEntries();

    List<GoalContribution> listAllGoalContributions();

    List<FamilyExpenseEntry> listAllFamilyExpenseEntries();

    List<InvestmentTransaction> listAllInvestmentTransactions();

    Map<String, String> listAppSettings();

    String getAppSetting(String key);

    void saveAppSetting(String key, String value);

    void deleteAppSetting(String key);
}
