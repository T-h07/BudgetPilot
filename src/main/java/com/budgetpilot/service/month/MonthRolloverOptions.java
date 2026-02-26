package com.budgetpilot.service.month;

import java.util.ArrayList;
import java.util.List;

public class MonthRolloverOptions {
    private boolean copyPlannerPlan;
    private List<String> selectedExpenseTemplateIds = new ArrayList<>();
    private List<String> selectedIncomeTemplateIds = new ArrayList<>();

    public boolean isCopyPlannerPlan() {
        return copyPlannerPlan;
    }

    public void setCopyPlannerPlan(boolean copyPlannerPlan) {
        this.copyPlannerPlan = copyPlannerPlan;
    }

    public List<String> getSelectedExpenseTemplateIds() {
        return List.copyOf(selectedExpenseTemplateIds);
    }

    public void setSelectedExpenseTemplateIds(List<String> selectedExpenseTemplateIds) {
        this.selectedExpenseTemplateIds = selectedExpenseTemplateIds == null
                ? new ArrayList<>()
                : new ArrayList<>(selectedExpenseTemplateIds);
    }

    public List<String> getSelectedIncomeTemplateIds() {
        return List.copyOf(selectedIncomeTemplateIds);
    }

    public void setSelectedIncomeTemplateIds(List<String> selectedIncomeTemplateIds) {
        this.selectedIncomeTemplateIds = selectedIncomeTemplateIds == null
                ? new ArrayList<>()
                : new ArrayList<>(selectedIncomeTemplateIds);
    }
}
