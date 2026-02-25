package com.budgetpilot.service.dashboard;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.achievements.AchievementPageSummary;
import com.budgetpilot.service.achievements.AchievementService;
import com.budgetpilot.service.balance.MonthlyBalanceService;
import com.budgetpilot.service.balance.MonthlyBalanceSnapshot;
import com.budgetpilot.service.planner.BudgetSummary;
import com.budgetpilot.service.expenses.ExpenseCategorySummary;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.expenses.ExpenseSummary;
import com.budgetpilot.service.forecast.ForecastService;
import com.budgetpilot.service.forecast.ForecastSummary;
import com.budgetpilot.service.family.FamilyService;
import com.budgetpilot.service.family.FamilySummary;
import com.budgetpilot.service.goals.GoalService;
import com.budgetpilot.service.goals.GoalSummary;
import com.budgetpilot.service.habits.HabitPageSummary;
import com.budgetpilot.service.habits.HabitService;
import com.budgetpilot.service.income.IncomeService;
import com.budgetpilot.service.investments.InvestmentPageSummary;
import com.budgetpilot.service.investments.InvestmentService;
import com.budgetpilot.service.planner.PlannerService;
import com.budgetpilot.service.savings.SavingsService;
import com.budgetpilot.service.savings.SavingsSummary;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardMetricsService {
    private static final DateTimeFormatter RANGE_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());

    private final BudgetStore budgetStore;
    private final IncomeService incomeService;
    private final PlannerService plannerService;
    private final ExpenseService expenseService;
    private final ForecastService forecastService;
    private final GoalService goalService;
    private final SavingsService savingsService;
    private final FamilyService familyService;
    private final HabitService habitService;
    private final InvestmentService investmentService;
    private final AchievementService achievementService;
    private final MonthlyBalanceService monthlyBalanceService;

    public DashboardMetricsService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.incomeService = new IncomeService(budgetStore);
        this.plannerService = new PlannerService(budgetStore);
        this.expenseService = new ExpenseService(budgetStore);
        this.forecastService = new ForecastService(budgetStore);
        this.goalService = new GoalService(budgetStore);
        this.savingsService = new SavingsService(budgetStore);
        this.familyService = new FamilyService(budgetStore);
        this.habitService = new HabitService(budgetStore);
        this.investmentService = new InvestmentService(budgetStore);
        this.achievementService = new AchievementService(budgetStore);
        this.monthlyBalanceService = new MonthlyBalanceService(budgetStore);
    }

    public DashboardSnapshot buildSnapshot(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        UserProfile profile = budgetStore.getUserProfile();
        String currencyCode = resolveCurrencyCode(profile);
        boolean familyEnabled = profile != null && profile.isFamilyModuleEnabled();
        boolean investmentsEnabled = profile != null && profile.isInvestmentsModuleEnabled();
        boolean achievementsEnabled = profile != null && profile.isAchievementsModuleEnabled();

        List<IncomeEntry> incomeEntries = incomeService.listForMonth(targetMonth);
        List<ExpenseEntry> expenseEntries = expenseService.listForMonth(targetMonth);
        GoalSummary goalsSummary = goalService.getGoalsSummary(targetMonth);
        SavingsSummary savingsSummary = savingsService.getSavingsSummary(targetMonth);
        FamilySummary familySummary = familyService.getFamilySummary(targetMonth);
        HabitPageSummary habitPageSummary = habitService.getHabitPageSummary(targetMonth);
        InvestmentPageSummary investmentSummary = investmentService.getInvestmentPageSummary(targetMonth);
        AchievementPageSummary achievementSummary = achievementService.getAchievementPageSummary(targetMonth);
        MonthlyBalanceSnapshot monthlyBalance = monthlyBalanceService.buildSnapshot(targetMonth);

        BudgetSummary budgetSummary = plannerService.buildBudgetSummary(targetMonth, familyEnabled);
        ForecastSummary forecastSummary = forecastService.buildForecast(targetMonth, familyEnabled);
        ExpenseSummary expenseSummary = expenseService.getExpenseSummary(targetMonth);
        BigDecimal unplannedSpend = expenseService.getTotalForBucket(targetMonth, PlannerBucket.UNPLANNED);

        boolean hasMonthlyPlan = budgetStore.getMonthlyPlan(targetMonth) != null;
        boolean hasIncomeData = !incomeEntries.isEmpty();
        boolean hasExpenseData = !expenseEntries.isEmpty();

        BigDecimal goalsCurrentTotal = goalsSummary.getTotalCurrentAmount();
        BigDecimal goalsTargetTotal = goalsSummary.getTotalTargetAmount();
        BigDecimal savingsCurrentTotal = savingsSummary.getTotalCurrentSavings();

        List<CategorySpendPoint> categorySpending = mapCategorySpending(expenseService.getCategorySummaries(targetMonth));
        List<WeeklySpendPoint> weeklySpending = buildWeeklySpending(targetMonth, expenseEntries);

        BudgetHealthScore healthScore = calculateHealthScore(
                targetMonth,
                hasMonthlyPlan,
                hasIncomeData,
                hasExpenseData,
                budgetSummary.isOverallocated(),
                forecastSummary.isOverspendingRisk(),
                budgetSummary.getRemainingPlanned(),
                forecastSummary.getProjectedRemainingAfterPlan()
        );

        PlannerVsActualSummary plannerVsActual = new PlannerVsActualSummary(
                budgetSummary.getPlannedIncome(),
                expenseSummary.getTotalExpenses(),
                forecastSummary.getProjectedExpensesByMonthEnd(),
                forecastSummary.getPlannedExpenseBudget(),
                budgetSummary.getSavingsAmountPlanned(),
                budgetSummary.getGoalsAmountPlanned(),
                budgetSummary.getRemainingPlanned(),
                forecastSummary.getProjectedRemainingAfterPlan()
        );

        List<DashboardAlert> alerts = buildAlerts(
                targetMonth,
                hasMonthlyPlan,
                hasIncomeData,
                hasExpenseData,
                familyEnabled,
                investmentsEnabled,
                achievementsEnabled,
                budgetSummary,
                forecastSummary,
                unplannedSpend,
                categorySpending,
                familySummary,
                habitPageSummary,
                investmentSummary,
                achievementSummary
        );

        List<DashboardKpi> kpis = buildKpis(
                currencyCode,
                budgetSummary,
                forecastSummary,
                expenseSummary,
                goalsCurrentTotal,
                goalsTargetTotal,
                healthScore,
                monthlyBalance
        );

        String primaryStatusMessage = alerts.isEmpty()
                ? healthScore.getMessage()
                : alerts.get(0).getMessage();

        return new DashboardSnapshot(
                targetMonth,
                MonthUtils.format(targetMonth),
                budgetSummary.getPlannedIncome(),
                budgetSummary.getReceivedIncome(),
                expenseSummary.getTotalExpenses(),
                expenseSummary.getAverageDailySpend(),
                forecastSummary.getProjectedExpensesByMonthEnd(),
                budgetSummary.getSavingsAmountPlanned(),
                budgetSummary.getGoalsAmountPlanned(),
                budgetSummary.getRemainingPlanned(),
                forecastSummary.getProjectedRemainingAfterPlan(),
                expenseSummary.getExpenseCount(),
                incomeEntries.size(),
                goalsSummary.getActiveGoalCount(),
                savingsSummary.getActiveBucketCount(),
                healthScore.getScore(),
                healthScore.getLabel(),
                primaryStatusMessage,
                hasMonthlyPlan,
                hasIncomeData,
                hasExpenseData,
                forecastSummary.isOverspendingRisk(),
                budgetSummary.isOverallocated(),
                categorySpending,
                weeklySpending,
                alerts,
                plannerVsActual,
                kpis,
                goalsCurrentTotal,
                goalsTargetTotal,
                savingsCurrentTotal,
                familyEnabled ? familySummary.getTotalFamilyCosts() : BigDecimal.ZERO.setScale(2),
                familyEnabled ? familySummary.getPlannedFamilyBudget() : BigDecimal.ZERO.setScale(2),
                familyEnabled ? familySummary.getActiveMembersCount() : 0,
                habitPageSummary.getWarningCount(),
                habitPageSummary.getExceededCount(),
                habitPageSummary.getHabitTrackedSpend(),
                investmentsEnabled ? investmentSummary.getTotalEstimatedValue() : BigDecimal.ZERO.setScale(2),
                investmentsEnabled ? investmentSummary.getTotalNetProfit() : BigDecimal.ZERO.setScale(2),
                investmentsEnabled ? investmentSummary.getActiveInvestmentCount() : 0,
                achievementsEnabled ? achievementSummary.getUnlockedCount() : 0,
                achievementsEnabled ? achievementSummary.getCompletionPercent() : BigDecimal.ZERO.setScale(2),
                achievementsEnabled ? achievementSummary.getInProgressCount() : 0
        );
    }

    private List<DashboardKpi> buildKpis(
            String currencyCode,
            BudgetSummary budgetSummary,
            ForecastSummary forecastSummary,
            ExpenseSummary expenseSummary,
            BigDecimal goalsCurrentTotal,
            BigDecimal goalsTargetTotal,
            BudgetHealthScore healthScore,
            MonthlyBalanceSnapshot monthlyBalance
    ) {
        List<DashboardKpi> kpis = new ArrayList<>();
        kpis.add(new DashboardKpi(
                "money_remaining",
                "Money Remaining",
                MoneyUtils.format(monthlyBalance.getAvailableAfterAllocations(), currencyCode),
                "After savings/goals allocations"
        ));
        kpis.add(new DashboardKpi(
                "total_spent",
                "Total Spent",
                MoneyUtils.format(expenseSummary.getTotalExpenses(), currencyCode),
                expenseSummary.getExpenseCount() + " expense entries"
        ));
        kpis.add(new DashboardKpi(
                "planned_income",
                "Planned Income",
                MoneyUtils.format(budgetSummary.getPlannedIncome(), currencyCode),
                "Received " + MoneyUtils.format(budgetSummary.getReceivedIncome(), currencyCode)
        ));
        kpis.add(new DashboardKpi(
                "forecast_spend",
                "Forecasted Spend",
                MoneyUtils.format(forecastSummary.getProjectedExpensesByMonthEnd(), currencyCode),
                forecastSummary.getStatusMessage()
        ));
        kpis.add(new DashboardKpi(
                "planned_savings",
                "Planned Savings",
                MoneyUtils.format(budgetSummary.getSavingsAmountPlanned(), currencyCode),
                "From " + budgetSummary.getSavingsPercent().toPlainString() + "%"
        ));
        kpis.add(new DashboardKpi(
                "goals_progress",
                "Goals Progress",
                formatGoalsProgress(goalsCurrentTotal, goalsTargetTotal),
                MoneyUtils.format(goalsCurrentTotal, currencyCode) + " / " + MoneyUtils.format(goalsTargetTotal, currencyCode)
        ));
        kpis.add(new DashboardKpi(
                "burn_rate",
                "Burn Rate",
                MoneyUtils.format(expenseSummary.getAverageDailySpend(), currencyCode) + "/day",
                "Current daily pace"
        ));
        kpis.add(new DashboardKpi(
                "budget_health",
                "Budget Health",
                healthScore.getScore() + "/100",
                healthScore.getLabel()
        ));
        return List.copyOf(kpis);
    }

    private String formatGoalsProgress(BigDecimal goalsCurrentTotal, BigDecimal goalsTargetTotal) {
        if (goalsTargetTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return "No target";
        }
        BigDecimal percent = goalsCurrentTotal
                .multiply(MoneyUtils.HUNDRED)
                .divide(goalsTargetTotal, 1, RoundingMode.HALF_UP);
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    private List<CategorySpendPoint> mapCategorySpending(List<ExpenseCategorySummary> source) {
        List<CategorySpendPoint> points = new ArrayList<>();
        for (ExpenseCategorySummary summary : source) {
            points.add(new CategorySpendPoint(
                    summary.getCategory(),
                    summary.getTotal(),
                    summary.getPercentOfTotal(),
                    summary.getEntryCount()
            ));
        }
        return List.copyOf(points);
    }

    private List<WeeklySpendPoint> buildWeeklySpending(YearMonth month, List<ExpenseEntry> expenses) {
        List<WeeklySpendPoint> points = new ArrayList<>();
        for (MonthUtils.WeekRange range : MonthUtils.calendarWeekRanges(month)) {
            BigDecimal total = BigDecimal.ZERO;
            for (ExpenseEntry expense : expenses) {
                if (expense.getExpenseDate() == null) {
                    continue;
                }
                if (!expense.getExpenseDate().isBefore(range.getStartDate())
                        && !expense.getExpenseDate().isAfter(range.getEndDate())) {
                    total = total.add(expense.getAmount());
                }
            }
            points.add(new WeeklySpendPoint(
                    range.getWeekLabel(),
                    RANGE_FORMATTER.format(range.getStartDate()) + " - " + RANGE_FORMATTER.format(range.getEndDate()),
                    MoneyUtils.normalize(total)
            ));
        }
        return List.copyOf(points);
    }

    private BudgetHealthScore calculateHealthScore(
            YearMonth month,
            boolean hasMonthlyPlan,
            boolean hasIncomeData,
            boolean hasExpenseData,
            boolean plannerOverallocated,
            boolean forecastOverspendingRisk,
            BigDecimal plannerRemainingPlanned,
            BigDecimal projectedRemainingAfterPlan
    ) {
        int score = 100;
        if (!hasMonthlyPlan) {
            score -= 20;
        }
        if (!hasIncomeData) {
            score -= 20;
        }
        if (plannerOverallocated) {
            score -= 25;
        }
        if (forecastOverspendingRisk) {
            score -= 20;
        }
        if (MonthUtils.isCurrentMonth(month)
                && !hasExpenseData
                && MonthUtils.elapsedDaysForForecast(month, null) > 3) {
            score -= 5;
        }
        if (plannerRemainingPlanned.compareTo(BigDecimal.ZERO) < 0) {
            score -= 10;
        }
        if (projectedRemainingAfterPlan.compareTo(BigDecimal.ZERO) < 0) {
            score -= 15;
        }

        score = Math.max(0, Math.min(100, score));
        String label;
        String message;
        if (score >= 80) {
            label = "Healthy";
            message = "Budget posture is stable for the selected month.";
        } else if (score >= 55) {
            label = "Caution";
            message = "Some budget signals require attention.";
        } else {
            label = "At Risk";
            message = "Financial risk is elevated. Review allocations and spending pace.";
        }
        return new BudgetHealthScore(score, label, message);
    }

    private List<DashboardAlert> buildAlerts(
            YearMonth month,
            boolean hasMonthlyPlan,
            boolean hasIncomeData,
            boolean hasExpenseData,
            boolean familyEnabled,
            boolean investmentsEnabled,
            boolean achievementsEnabled,
            BudgetSummary budgetSummary,
            ForecastSummary forecastSummary,
            BigDecimal unplannedSpend,
            List<CategorySpendPoint> categoryPoints,
            FamilySummary familySummary,
            HabitPageSummary habitPageSummary,
            InvestmentPageSummary investmentSummary,
            AchievementPageSummary achievementSummary
    ) {
        List<DashboardAlert> alerts = new ArrayList<>();

        if (!hasMonthlyPlan) {
            alerts.add(new DashboardAlert(
                    "planner-missing",
                    AlertLevel.WARNING,
                    "Monthly plan missing",
                    "No monthly plan exists for " + MonthUtils.format(month) + ".",
                    "planner",
                    "Create a monthly plan in Planner."
            ));
        }

        if (!hasIncomeData) {
            alerts.add(new DashboardAlert(
                    "income-missing",
                    AlertLevel.DANGER,
                    "No income entries",
                    "Add at least one income entry to improve forecast and planner accuracy.",
                    "income",
                    "Open Income page and add your expected income."
            ));
        }

        if (budgetSummary.isOverallocated()) {
            alerts.add(new DashboardAlert(
                    "planner-overallocated",
                    AlertLevel.DANGER,
                    "Planner overallocated",
                    "Planner is overallocated by " + MoneyUtils.normalize(budgetSummary.getRemainingPlanned().abs()) + ".",
                    "planner",
                    "Reduce discretionary/fixed allocations or increase income."
            ));
        }

        if (forecastSummary.isOverspendingRisk()) {
            alerts.add(new DashboardAlert(
                    "forecast-risk",
                    AlertLevel.DANGER,
                    "Forecast overspending risk",
                    forecastSummary.getStatusMessage(),
                    "forecast",
                    "Adjust spending pace and review planner targets."
            ));
        }

        if (!hasExpenseData
                && MonthUtils.isCurrentMonth(month)
                && MonthUtils.elapsedDaysForForecast(month, null) > 3) {
            alerts.add(new DashboardAlert(
                    "expenses-none",
                    AlertLevel.INFO,
                    "No expenses tracked yet",
                    "No expenses logged so far this month.",
                    "expenses",
                    "Record expenses regularly for better burn-rate insights."
            ));
        }

        if (!categoryPoints.isEmpty() && categoryPoints.get(0).getPercentOfTotal().compareTo(new BigDecimal("50")) > 0) {
            CategorySpendPoint top = categoryPoints.get(0);
            alerts.add(new DashboardAlert(
                    "category-concentration",
                    AlertLevel.WARNING,
                    "Category concentration",
                    top.getCategoryLabel() + " represents " + top.getPercentOfTotal().toPlainString() + "% of total spend.",
                    "expenses",
                    "Review category limits and weekly pace."
            ));
        }

        if (unplannedSpend.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalSpent = expenseService.getTotalExpenses(month);
            BigDecimal percent = totalSpent.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO
                    : unplannedSpend.multiply(MoneyUtils.HUNDRED).divide(totalSpent, 2, RoundingMode.HALF_UP);
            boolean significant = unplannedSpend.compareTo(new BigDecimal("50.00")) > 0
                    || percent.compareTo(new BigDecimal("10.00")) > 0;
            alerts.add(new DashboardAlert(
                    "unplanned-spend",
                    significant ? AlertLevel.WARNING : AlertLevel.INFO,
                    "Unplanned spend",
                    "Unplanned spend this month: " + unplannedSpend.stripTrailingZeros().toPlainString() + ".",
                    "expenses",
                    significant
                            ? "Review one-time purchases and rebalance discretionary spending."
                            : "Keep one-time spending controlled."
            ));
        }

        if (familyEnabled
                && familySummary.getPlannedFamilyBudget().compareTo(BigDecimal.ZERO) > 0
                && familySummary.getFamilyBudgetVariance().compareTo(BigDecimal.ZERO) > 0) {
            alerts.add(new DashboardAlert(
                    "family-budget-overrun",
                    AlertLevel.WARNING,
                    "Family budget overrun",
                    "Family costs are over planned budget by "
                            + familySummary.getFamilyBudgetVariance().stripTrailingZeros().toPlainString() + ".",
                    "family",
                    "Review support and medical allocations in Family page."
            ));
        }

        if (familyEnabled
                && familySummary.getTotalFamilyCosts().compareTo(BigDecimal.ZERO) > 0
                && familySummary.getTotalMedicalCosts().multiply(MoneyUtils.HUNDRED)
                .divide(familySummary.getTotalFamilyCosts(), 2, RoundingMode.HALF_UP)
                .compareTo(new BigDecimal("50.00")) > 0) {
            alerts.add(new DashboardAlert(
                    "family-medical-high",
                    AlertLevel.WARNING,
                    "Family medical costs are high",
                    "Medical spend accounts for more than half of family costs this month.",
                    "family",
                    "Check medical spending trends and reserve planning."
            ));
        }

        if (habitPageSummary.getExceededCount() > 0) {
            alerts.add(new DashboardAlert(
                    "habit-exceeded",
                    AlertLevel.DANGER,
                    "Habit limits exceeded",
                    habitPageSummary.getExceededCount() + " habit rules exceeded their monthly limits.",
                    "habits",
                    "Open Habits page and adjust spending behavior."
            ));
        }

        if (habitPageSummary.getWarningCount() > 0) {
            alerts.add(new DashboardAlert(
                    "habit-warning",
                    AlertLevel.WARNING,
                    "Habit warnings active",
                    habitPageSummary.getWarningCount() + " habit rules are near their monthly limits.",
                    "habits",
                    "Take corrective actions before limits are exceeded."
            ));
        } else if (habitPageSummary.getActiveRulesCount() > 0) {
            alerts.add(new DashboardAlert(
                    "habit-on-track",
                    AlertLevel.INFO,
                    "Habits on track",
                    "All active habit rules are currently on track.",
                    "habits",
                    "Maintain current spending discipline."
            ));
        }

        if (investmentsEnabled) {
            if (investmentSummary.getActiveInvestmentCount() == 0) {
                alerts.add(new DashboardAlert(
                        "investments-empty",
                        AlertLevel.INFO,
                        "No investments added",
                        "Add your first investment to track growth and ROI.",
                        "investments",
                        "Open Investments page and create a position."
                ));
            } else if (investmentSummary.getTotalNetProfit().compareTo(BigDecimal.ZERO) > 0) {
                alerts.add(new DashboardAlert(
                        "investments-positive-roi",
                        AlertLevel.INFO,
                        "Portfolio ROI positive",
                        "Portfolio net profit is currently positive.",
                        "investments",
                        "Keep monitoring returns and contribution cadence."
                ));
            }
        }

        if (achievementsEnabled) {
            if (achievementSummary.getInProgressCount() > 0) {
                alerts.add(new DashboardAlert(
                        "achievements-progress",
                        AlertLevel.INFO,
                        "Achievements progressing",
                        achievementSummary.getInProgressCount() + " achievements are currently in progress.",
                        "achievements",
                        "Open Achievements page to view closest unlocks."
                ));
            }
            if (achievementSummary.getUnlockedCount() > 0) {
                alerts.add(new DashboardAlert(
                        "achievements-unlocked",
                        AlertLevel.INFO,
                        "Achievements unlocked",
                        "Unlocked achievements: " + achievementSummary.getUnlockedCount() + ".",
                        "achievements",
                        "Maintain consistency to unlock higher tiers."
                ));
            }
        }

        if (alerts.isEmpty()) {
            alerts.add(new DashboardAlert(
                    "all-good",
                    AlertLevel.INFO,
                    "On track",
                    "Everything looks on track this month.",
                    "dashboard",
                    "Keep consistent tracking for stronger forecasts."
            ));
        }

        alerts.sort(Comparator
                .comparingInt((DashboardAlert alert) -> severityRank(alert.getLevel()))
                .thenComparing(DashboardAlert::getTitle));

        return List.copyOf(alerts.stream().limit(5).toList());
    }

    private int severityRank(AlertLevel level) {
        return switch (level) {
            case DANGER -> 0;
            case WARNING -> 1;
            case INFO -> 2;
        };
    }

    private String resolveCurrencyCode(UserProfile profile) {
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
    }
}
