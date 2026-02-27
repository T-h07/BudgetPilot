package com.budgetpilot.service.balance;

import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.goals.GoalService;
import com.budgetpilot.service.income.IncomeService;
import com.budgetpilot.service.investments.InvestmentService;
import com.budgetpilot.service.savings.SavingsService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MonthlyBalanceService {
    private static final BigDecimal LOW_CASH_FLOOR = new BigDecimal("50.00");
    private static final BigDecimal LOW_CASH_PERCENT = new BigDecimal("0.10");

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final SavingsService savingsService;
    private final GoalService goalService;
    private final InvestmentService investmentService;

    public MonthlyBalanceService(BudgetStore budgetStore) {
        BudgetStore store = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.incomeService = new IncomeService(store);
        this.expenseService = new ExpenseService(store);
        this.savingsService = new SavingsService(store);
        this.goalService = new GoalService(store);
        this.investmentService = new InvestmentService(store);
    }

    public MonthlyBalanceSnapshot buildSnapshot(YearMonth month) {
        return buildProjectedSnapshot(month, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public MonthlyBalanceSnapshot buildProjectedSnapshot(
            YearMonth month,
            BigDecimal additionalSavingsAllocation,
            BigDecimal additionalGoalAllocation
    ) {
        return buildProjectedSnapshot(month, additionalSavingsAllocation, additionalGoalAllocation, BigDecimal.ZERO);
    }

    public MonthlyBalanceSnapshot buildProjectedSnapshot(
            YearMonth month,
            BigDecimal additionalSavingsAllocation,
            BigDecimal additionalGoalAllocation,
            BigDecimal additionalInvestmentAllocation
    ) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        BigDecimal additionalSavings = MoneyUtils.zeroIfNull(additionalSavingsAllocation);
        BigDecimal additionalGoals = MoneyUtils.zeroIfNull(additionalGoalAllocation);
        BigDecimal additionalInvestments = MoneyUtils.zeroIfNull(additionalInvestmentAllocation);

        BigDecimal plannedIncome = incomeService.getPlannedIncomeTotal(targetMonth);
        BigDecimal receivedIncome = incomeService.getReceivedIncomeTotal(targetMonth);
        BigDecimal totalExpenses = expenseService.getTotalExpenses(targetMonth);

        BigDecimal netSavingsAllocations = MoneyUtils.normalize(
                savingsService.getMonthlyNetAllocationsTotal(targetMonth).add(additionalSavings)
        );
        BigDecimal netGoalAllocations = MoneyUtils.normalize(
                goalService.getMonthlyNetAllocationsTotal(targetMonth).add(additionalGoals)
        );
        BigDecimal netInvestmentAllocations = MoneyUtils.normalize(
                investmentService.getMonthlyNetAllocationsTotal(targetMonth).add(additionalInvestments)
        );

        BigDecimal availableBeforeAllocations = MoneyUtils.normalize(plannedIncome.subtract(totalExpenses));
        BigDecimal availableAfterAllocations = MoneyUtils.normalize(
                availableBeforeAllocations
                        .subtract(netSavingsAllocations)
                        .subtract(netGoalAllocations)
                        .subtract(netInvestmentAllocations)
        );

        MonthlyBalanceWarningLevel warningLevel = resolveWarningLevel(
                plannedIncome,
                availableAfterAllocations
        );
        String warningMessage = buildWarningMessage(warningLevel, availableAfterAllocations);

        return new MonthlyBalanceSnapshot(
                targetMonth,
                plannedIncome,
                receivedIncome,
                totalExpenses,
                netSavingsAllocations,
                netGoalAllocations,
                netInvestmentAllocations,
                availableBeforeAllocations,
                availableAfterAllocations,
                warningLevel,
                warningMessage
        );
    }

    private MonthlyBalanceWarningLevel resolveWarningLevel(
            BigDecimal plannedIncome,
            BigDecimal availableAfterAllocations
    ) {
        if (availableAfterAllocations.compareTo(BigDecimal.ZERO) < 0) {
            return MonthlyBalanceWarningLevel.NEGATIVE;
        }

        BigDecimal safePlannedIncome = MoneyUtils.zeroIfNull(plannedIncome);
        if (safePlannedIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return MonthlyBalanceWarningLevel.LOW;
        }

        BigDecimal tenPercentThreshold = MoneyUtils.normalize(safePlannedIncome.multiply(LOW_CASH_PERCENT));
        if (availableAfterAllocations.compareTo(tenPercentThreshold) < 0
                || availableAfterAllocations.compareTo(LOW_CASH_FLOOR) < 0) {
            return MonthlyBalanceWarningLevel.LOW;
        }

        return MonthlyBalanceWarningLevel.OK;
    }

    private String buildWarningMessage(MonthlyBalanceWarningLevel warningLevel, BigDecimal availableAfterAllocations) {
        return switch (warningLevel) {
            case NEGATIVE -> "Monthly available balance is negative by "
                    + availableAfterAllocations.abs().stripTrailingZeros().toPlainString()
                    + ".";
            case LOW -> "Monthly available balance is low: "
                    + availableAfterAllocations.stripTrailingZeros().toPlainString()
                    + ".";
            case OK -> "Monthly available balance is healthy.";
        };
    }
}
