package com.budgetpilot.service.forecast;

import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.income.IncomeService;
import com.budgetpilot.service.planner.BudgetSummary;
import com.budgetpilot.service.planner.PlannerService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public class ForecastService {
    private final BudgetStore budgetStore;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final PlannerService plannerService;

    public ForecastService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.expenseService = new ExpenseService(budgetStore);
        this.incomeService = new IncomeService(budgetStore);
        this.plannerService = new PlannerService(budgetStore);
    }

    public ForecastSummary buildForecast(YearMonth month, boolean familyModuleEnabled) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");

        int daysInMonth = MonthUtils.daysInMonth(targetMonth);
        int daysElapsed = MonthUtils.elapsedDaysForForecast(targetMonth, LocalDate.now());

        BigDecimal actualSpent = expenseService.getTotalExpenses(targetMonth);
        BigDecimal averageDailySpend = daysElapsed <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyUtils.normalize(actualSpent.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP));
        BigDecimal projectedExpenses = daysElapsed <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyUtils.normalize(averageDailySpend.multiply(BigDecimal.valueOf(daysInMonth)));

        BigDecimal plannedIncome = incomeService.getPlannedIncomeTotal(targetMonth);
        BudgetSummary budgetSummary = plannerService.buildBudgetSummary(targetMonth, familyModuleEnabled);
        BigDecimal plannedSavings = budgetSummary.getSavingsAmountPlanned();
        BigDecimal plannedGoals = budgetSummary.getGoalsAmountPlanned();
        BigDecimal safetyBuffer = budgetSummary.getSafetyBufferAmount();
        BigDecimal plannedExpenseBudget = MoneyUtils.normalize(
                budgetSummary.getFixedCostsBudget()
                        .add(budgetSummary.getEssentialsBudget())
                        .add(budgetSummary.getDiscretionaryBudget())
        );

        BigDecimal projectedRemainingAfterExpenses = MoneyUtils.safeSubtract(plannedIncome, projectedExpenses);
        BigDecimal projectedRemainingAfterPlan = MoneyUtils.normalize(
                plannedIncome.subtract(
                        projectedExpenses
                                .add(plannedSavings)
                                .add(plannedGoals)
                                .add(safetyBuffer)
                )
        );

        MonthlyPlan existingPlan = budgetStore.getMonthlyPlan(targetMonth);
        boolean planMissing = existingPlan == null;
        boolean hasNoData = actualSpent.compareTo(BigDecimal.ZERO) <= 0;
        boolean overPlannerBudgetRisk = !planMissing && projectedExpenses.compareTo(plannedExpenseBudget) > 0;
        boolean overspendingRisk = overPlannerBudgetRisk || projectedRemainingAfterPlan.compareTo(BigDecimal.ZERO) < 0;

        String statusMessage = buildStatusMessage(
                targetMonth,
                hasNoData,
                planMissing,
                plannedIncome,
                projectedExpenses,
                plannedExpenseBudget,
                projectedRemainingAfterPlan,
                overPlannerBudgetRisk
        );

        return new ForecastSummary(
                targetMonth,
                daysInMonth,
                daysElapsed,
                actualSpent,
                averageDailySpend,
                projectedExpenses,
                plannedIncome,
                plannedExpenseBudget,
                plannedSavings,
                plannedGoals,
                safetyBuffer,
                projectedRemainingAfterExpenses,
                projectedRemainingAfterPlan,
                hasNoData,
                overspendingRisk,
                overPlannerBudgetRisk,
                planMissing,
                statusMessage
        );
    }

    private String buildStatusMessage(
            YearMonth month,
            boolean hasNoData,
            boolean planMissing,
            BigDecimal plannedIncome,
            BigDecimal projectedExpenses,
            BigDecimal plannedExpenseBudget,
            BigDecimal projectedRemainingAfterPlan,
            boolean overPlannerBudgetRisk
    ) {
        if (month.isAfter(MonthUtils.currentMonth()) && hasNoData) {
            return "No expense data yet for this future month.";
        }
        if (hasNoData) {
            return "No expense data yet for this month.";
        }
        if (planMissing) {
            return "No monthly plan found for this month; forecast comparisons are limited.";
        }
        if (overPlannerBudgetRisk) {
            BigDecimal delta = MoneyUtils.safeSubtract(projectedExpenses, plannedExpenseBudget);
            return "At current pace, expenses are projected to exceed planned expense budget by " + delta + ".";
        }
        if (plannedIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return "No income entered for this month; remaining projections are limited.";
        }
        if (projectedRemainingAfterPlan.compareTo(BigDecimal.ZERO) < 0) {
            return "At current pace, month-end balance may be negative after plan allocations.";
        }
        return "You are on track this month.";
    }
}
