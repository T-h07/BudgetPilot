package com.budgetpilot.service.month;

import java.util.ArrayList;
import java.util.List;

public class MonthRolloverOptions {
    private boolean copyPlannerPlan;
    private boolean carryForwardRecurringIncome;
    private boolean carryForwardRecurringExpenses;
    private List<ExpenseTemplateSelection> selectedExpenseTemplates = new ArrayList<>();

    public boolean isCopyPlannerPlan() {
        return copyPlannerPlan;
    }

    public void setCopyPlannerPlan(boolean copyPlannerPlan) {
        this.copyPlannerPlan = copyPlannerPlan;
    }

    public boolean isCarryForwardRecurringIncome() {
        return carryForwardRecurringIncome;
    }

    public void setCarryForwardRecurringIncome(boolean carryForwardRecurringIncome) {
        this.carryForwardRecurringIncome = carryForwardRecurringIncome;
    }

    public boolean isCarryForwardRecurringExpenses() {
        return carryForwardRecurringExpenses;
    }

    public void setCarryForwardRecurringExpenses(boolean carryForwardRecurringExpenses) {
        this.carryForwardRecurringExpenses = carryForwardRecurringExpenses;
    }

    public List<ExpenseTemplateSelection> getSelectedExpenseTemplates() {
        return List.copyOf(selectedExpenseTemplates);
    }

    public void setSelectedExpenseTemplates(List<ExpenseTemplateSelection> selectedExpenseTemplates) {
        this.selectedExpenseTemplates = selectedExpenseTemplates == null
                ? new ArrayList<>()
                : new ArrayList<>(selectedExpenseTemplates);
    }
}
