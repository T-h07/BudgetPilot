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
                targetMonth,
                currencyCode,
                budgetSummary,
                forecastSummary,
                expenseSummary,
                goalsSummary,
                savingsSummary,
                goalsCurrentTotal,
                goalsTargetTotal,
                healthScore,
                monthlyBalance,
                weeklySpending
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
            YearMonth month,
            String currencyCode,
            BudgetSummary budgetSummary,
            ForecastSummary forecastSummary,
            ExpenseSummary expenseSummary,
            GoalSummary goalsSummary,
            SavingsSummary savingsSummary,
            BigDecimal goalsCurrentTotal,
            BigDecimal goalsTargetTotal,
            BudgetHealthScore healthScore,
            MonthlyBalanceSnapshot monthlyBalance,
            List<WeeklySpendPoint> weeklySpending
    ) {
        List<DashboardKpi> kpis = new ArrayList<>();
        BigDecimal plannedIncome = budgetSummary.getPlannedIncome();
        BigDecimal plannedSpendingBudget = forecastSummary.getPlannedExpenseBudget();
        int daysInMonth = Math.max(forecastSummary.getDaysInMonth(), 1);
        int safeDaysElapsed = Math.max(forecastSummary.getDaysElapsed(), 1);
        BigDecimal safeDailyPlan = resolveDailyPlanBudget(plannedSpendingBudget, plannedIncome, daysInMonth);

        KpiStatus moneyRemainingStatus = resolveMoneyRemainingStatus(monthlyBalance.getAvailableAfterAllocations(), plannedIncome);
        double remainingRatio = ratio(monthlyBalance.getAvailableAfterAllocations(), plannedIncome);
        kpis.add(new DashboardKpi(
                "money_remaining",
                "Money Remaining",
                MoneyUtils.format(monthlyBalance.getAvailableAfterAllocations(), currencyCode),
                "Remaining "
                        + MoneyUtils.format(monthlyBalance.getAvailableAfterAllocations(), currencyCode)
                        + " after allocations",
                "After savings/goals allocations",
                moneyRemainingStatus,
                new KpiVisualData(
                        KpiVisualType.BATTERY_BAR,
                        moneyRemainingStatus,
                        List.of(),
                        remainingRatio,
                        0d,
                        TrendDirection.UNKNOWN
                )
        ));

        BigDecimal actualDailySpend = expenseSummary.getTotalExpenses()
                .divide(BigDecimal.valueOf(safeDaysElapsed), 4, RoundingMode.HALF_UP);
        BigDecimal paceRatioValue = safeDailyPlan.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : actualDailySpend.divide(safeDailyPlan, 4, RoundingMode.HALF_UP);
        KpiStatus totalSpentStatus = resolvePaceStatus(paceRatioValue, safeDailyPlan, actualDailySpend);
        kpis.add(new DashboardKpi(
                "total_spent",
                "Total Spent",
                MoneyUtils.format(expenseSummary.getTotalExpenses(), currencyCode),
                "Pace " + paceRatioValue.setScale(2, RoundingMode.HALF_UP).toPlainString() + "x vs plan",
                expenseSummary.getExpenseCount() + " expense entries",
                totalSpentStatus,
                new KpiVisualData(
                        KpiVisualType.DENSITY_CURVE,
                        totalSpentStatus,
                        List.of(),
                        clamp01(paceRatioValue.doubleValue() / 1.5d),
                        0d,
                        TrendDirection.UNKNOWN
                )
        ));

        BigDecimal receivedIncome = budgetSummary.getReceivedIncome();
        BigDecimal receivedRatio = plannedIncome.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : receivedIncome.divide(plannedIncome, 4, RoundingMode.HALF_UP);
        KpiStatus plannedIncomeStatus = resolvePlannedIncomeStatus(
                month,
                plannedIncome,
                receivedIncome,
                forecastSummary.getDaysElapsed()
        );
        kpis.add(new DashboardKpi(
                "planned_income",
                "Planned Income",
                MoneyUtils.format(plannedIncome, currencyCode),
                "Received " + receivedRatio.multiply(MoneyUtils.HUNDRED).setScale(0, RoundingMode.HALF_UP) + "%",
                "Received " + MoneyUtils.format(receivedIncome, currencyCode),
                plannedIncomeStatus,
                new KpiVisualData(
                        KpiVisualType.STACKED_INCOME_BAR,
                        plannedIncomeStatus,
                        List.of(),
                        clamp01(receivedRatio.doubleValue()),
                        0d,
                        TrendDirection.UNKNOWN
                )
        ));

        BigDecimal forecastDelta = MoneyUtils.safeSubtract(
                forecastSummary.getProjectedExpensesByMonthEnd(),
                plannedSpendingBudget
        );
        KpiStatus forecastStatus = resolveForecastStatus(forecastSummary, forecastDelta, plannedSpendingBudget);
        TrendDirection forecastTrend = resolveWeeklyTrendDirection(weeklySpending);
        String forecastAccent = forecastSummary.isPlanMissing()
                ? "Projected month-end spend: " + MoneyUtils.format(forecastSummary.getProjectedExpensesByMonthEnd(), currencyCode)
                : "Delta " + formatSignedCurrency(forecastDelta, currencyCode) + " vs planned";
        String forecastSubtext = forecastSummary.isPlanMissing()
                ? "Set a plan to compare against budgets."
                : "Based on current pace: " + MoneyUtils.format(forecastSummary.getAverageDailySpend(), currencyCode) + "/day";
        kpis.add(new DashboardKpi(
                "forecast_spend",
                "Forecasted Spend",
                MoneyUtils.format(forecastSummary.getProjectedExpensesByMonthEnd(), currencyCode),
                forecastAccent,
                forecastSubtext,
                forecastStatus,
                new KpiVisualData(
                        KpiVisualType.TREND_ARROWS,
                        forecastStatus,
                        List.of(),
                        0d,
                        0d,
                        forecastTrend
                )
        ));

        BigDecimal plannedSavings = budgetSummary.getSavingsAmountPlanned();
        BigDecimal actualSavingsNet = savingsSummary.getMonthlyNetChange();
        KpiStatus plannedSavingsStatus = resolveSavingsStatus(plannedSavings, actualSavingsNet);
        double savingsRatio = plannedSavings.compareTo(BigDecimal.ZERO) <= 0
                ? (actualSavingsNet.compareTo(BigDecimal.ZERO) > 0 ? 1d : 0d)
                : clamp01(actualSavingsNet.divide(plannedSavings, 4, RoundingMode.HALF_UP).doubleValue());
        kpis.add(new DashboardKpi(
                "planned_savings",
                "Planned Savings",
                MoneyUtils.format(plannedSavings, currencyCode),
                "Actual net " + MoneyUtils.format(actualSavingsNet, currencyCode),
                "From " + budgetSummary.getSavingsPercent().toPlainString() + "% target",
                plannedSavingsStatus,
                new KpiVisualData(
                        KpiVisualType.MICRO_BARS,
                        plannedSavingsStatus,
                        buildRatioBars(savingsRatio, 4),
                        savingsRatio,
                        0d,
                        TrendDirection.UNKNOWN
                )
        ));

        BigDecimal goalsMonthlyContrib = goalsSummary.getMonthlyContributions();
        KpiStatus goalsStatus = resolveGoalsStatus(goalsCurrentTotal, goalsTargetTotal, goalsMonthlyContrib);
        List<Double> goalSpark = normalizeSeries(buildGoalContributionSeries(month, 6));
        kpis.add(new DashboardKpi(
                "goals_progress",
                "Goals Progress",
                formatGoalsProgress(goalsCurrentTotal, goalsTargetTotal),
                "Contributed " + MoneyUtils.format(goalsMonthlyContrib, currencyCode) + " this month",
                MoneyUtils.format(goalsCurrentTotal, currencyCode) + " / " + MoneyUtils.format(goalsTargetTotal, currencyCode),
                goalsStatus,
                new KpiVisualData(
                        KpiVisualType.SPARKLINE,
                        goalsStatus,
                        goalSpark,
                        clamp01(ratio(goalsCurrentTotal, goalsTargetTotal)),
                        0d,
                        resolveTrendDirection(goalSpark)
                )
        ));

        BigDecimal burnRate = expenseSummary.getAverageDailySpend();
        BigDecimal burnRatio = safeDailyPlan.compareTo(BigDecimal.ZERO) <= 0
                ? (burnRate.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO)
                : burnRate.divide(safeDailyPlan, 4, RoundingMode.HALF_UP);
        KpiStatus burnRateStatus = resolvePaceStatus(burnRatio, safeDailyPlan, burnRate);
        List<Double> burnBars = normalizeSeries(extractWeeklyTotals(weeklySpending));
        kpis.add(new DashboardKpi(
                "burn_rate",
                "Burn Rate",
                MoneyUtils.format(burnRate, currencyCode) + "/day",
                "Safe daily " + MoneyUtils.format(safeDailyPlan, currencyCode) + "/day",
                "Current daily pace",
                burnRateStatus,
                new KpiVisualData(
                        KpiVisualType.MICRO_BARS,
                        burnRateStatus,
                        burnBars,
                        clamp01(burnRatio.doubleValue()),
                        0d,
                        resolveTrendDirection(burnBars)
                )
        ));

        KpiStatus budgetHealthStatus = resolveBudgetHealthStatus(healthScore.getScore());
        kpis.add(new DashboardKpi(
                "budget_health",
                "Budget Health",
                healthScore.getScore() + "/100",
                healthScore.getLabel(),
                healthScore.getMessage(),
                budgetHealthStatus,
                new KpiVisualData(
                        KpiVisualType.BATTERY_BAR,
                        budgetHealthStatus,
                        List.of(),
                        clamp01(healthScore.getScore() / 100d),
                        0d,
                        TrendDirection.UNKNOWN
                )
        ));
        return List.copyOf(kpis);
    }

    private BigDecimal resolveDailyPlanBudget(BigDecimal plannedSpendingBudget, BigDecimal plannedIncome, int daysInMonth) {
        BigDecimal source = plannedSpendingBudget.compareTo(BigDecimal.ZERO) > 0 ? plannedSpendingBudget : plannedIncome;
        if (source.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return source.divide(BigDecimal.valueOf(Math.max(daysInMonth, 1)), 2, RoundingMode.HALF_UP);
    }

    private KpiStatus resolveMoneyRemainingStatus(BigDecimal remaining, BigDecimal plannedIncome) {
        double ratio = ratio(remaining, plannedIncome);
        if (remaining.compareTo(BigDecimal.ZERO) < 0
                || remaining.compareTo(new BigDecimal("50.00")) < 0
                || (plannedIncome.compareTo(BigDecimal.ZERO) > 0 && ratio < 0.10d)) {
            return KpiStatus.DANGER;
        }
        if (remaining.compareTo(new BigDecimal("200.00")) < 0
                || (plannedIncome.compareTo(BigDecimal.ZERO) > 0 && ratio < 0.25d)) {
            return KpiStatus.WARNING;
        }
        return KpiStatus.GOOD;
    }

    private KpiStatus resolvePaceStatus(BigDecimal paceRatio, BigDecimal safeBaseline, BigDecimal actualDaily) {
        if (safeBaseline.compareTo(BigDecimal.ZERO) <= 0) {
            return actualDaily.compareTo(BigDecimal.ZERO) > 0 ? KpiStatus.WARNING : KpiStatus.GOOD;
        }
        if (paceRatio.compareTo(new BigDecimal("1.10")) > 0) {
            return KpiStatus.DANGER;
        }
        if (paceRatio.compareTo(new BigDecimal("0.90")) > 0) {
            return KpiStatus.WARNING;
        }
        return KpiStatus.GOOD;
    }

    private KpiStatus resolvePlannedIncomeStatus(
            YearMonth month,
            BigDecimal plannedIncome,
            BigDecimal receivedIncome,
            int elapsedDays
    ) {
        if (plannedIncome.compareTo(BigDecimal.ZERO) <= 0) {
            if (receivedIncome.compareTo(BigDecimal.ZERO) > 0) {
                return KpiStatus.WARNING;
            }
            return elapsedDays > 3 && MonthUtils.isCurrentMonth(month) ? KpiStatus.DANGER : KpiStatus.WARNING;
        }

        BigDecimal ratio = receivedIncome.divide(plannedIncome, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.90")) >= 0) {
            return KpiStatus.GOOD;
        }
        if (receivedIncome.compareTo(BigDecimal.ZERO) > 0) {
            return KpiStatus.WARNING;
        }
        return elapsedDays > 3 && MonthUtils.isCurrentMonth(month) ? KpiStatus.DANGER : KpiStatus.WARNING;
    }

    private KpiStatus resolveForecastStatus(
            ForecastSummary forecastSummary,
            BigDecimal delta,
            BigDecimal plannedSpendingBudget
    ) {
        if (forecastSummary.isPlanMissing()) {
            return KpiStatus.WARNING;
        }
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            return KpiStatus.GOOD;
        }
        BigDecimal threshold = MoneyUtils.normalize(
                plannedSpendingBudget.multiply(new BigDecimal("0.10")).max(new BigDecimal("50.00"))
        );
        return delta.compareTo(threshold) <= 0 ? KpiStatus.WARNING : KpiStatus.DANGER;
    }

    private KpiStatus resolveSavingsStatus(BigDecimal plannedSavings, BigDecimal actualSavingsNet) {
        if (actualSavingsNet.compareTo(BigDecimal.ZERO) < 0) {
            return KpiStatus.DANGER;
        }
        if (plannedSavings.compareTo(BigDecimal.ZERO) <= 0) {
            return actualSavingsNet.compareTo(BigDecimal.ZERO) > 0 ? KpiStatus.GOOD : KpiStatus.WARNING;
        }
        BigDecimal ratio = actualSavingsNet.divide(plannedSavings, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE) >= 0) {
            return KpiStatus.GOOD;
        }
        if (ratio.compareTo(new BigDecimal("0.50")) >= 0) {
            return KpiStatus.WARNING;
        }
        return KpiStatus.DANGER;
    }

    private KpiStatus resolveGoalsStatus(
            BigDecimal goalsCurrentTotal,
            BigDecimal goalsTargetTotal,
            BigDecimal monthlyContributions
    ) {
        if (goalsTargetTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return KpiStatus.DANGER;
        }
        if (monthlyContributions.compareTo(BigDecimal.ZERO) > 0
                && goalsCurrentTotal.compareTo(BigDecimal.ZERO) > 0) {
            return KpiStatus.GOOD;
        }
        if (goalsCurrentTotal.compareTo(BigDecimal.ZERO) > 0) {
            return KpiStatus.WARNING;
        }
        return KpiStatus.DANGER;
    }

    private KpiStatus resolveBudgetHealthStatus(int score) {
        if (score >= 80) {
            return KpiStatus.GOOD;
        }
        if (score >= 55) {
            return KpiStatus.WARNING;
        }
        return KpiStatus.DANGER;
    }

    private List<BigDecimal> buildGoalContributionSeries(YearMonth month, int points) {
        List<BigDecimal> series = new ArrayList<>();
        for (int i = points - 1; i >= 0; i--) {
            YearMonth cursor = month.minusMonths(i);
            series.add(goalService.getGoalsSummary(cursor).getMonthlyContributions());
        }
        return series;
    }

    private List<BigDecimal> extractWeeklyTotals(List<WeeklySpendPoint> weeklySpending) {
        List<BigDecimal> totals = new ArrayList<>();
        for (WeeklySpendPoint point : weeklySpending) {
            totals.add(point.getTotalSpent());
        }
        return totals;
    }

    private List<Double> normalizeSeries(List<BigDecimal> source) {
        if (source == null || source.isEmpty()) {
            return List.of(0.5d, 0.5d, 0.5d, 0.5d, 0.5d, 0.5d);
        }
        BigDecimal max = source.stream()
                .map(MoneyUtils::zeroIfNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return source.stream().map(item -> 0.5d).toList();
        }
        return source.stream()
                .map(MoneyUtils::zeroIfNull)
                .map(item -> clamp01(item.divide(max, 6, RoundingMode.HALF_UP).doubleValue()))
                .toList();
    }

    private List<Double> buildRatioBars(double ratio, int barCount) {
        int safeCount = Math.max(1, barCount);
        double clampedRatio = clamp01(ratio);
        List<Double> bars = new ArrayList<>(safeCount);
        for (int i = 1; i <= safeCount; i++) {
            double threshold = (double) i / safeCount;
            // Filled bars stay tall; unfilled bars still render as muted mini stubs.
            bars.add(clampedRatio >= threshold ? 1d : 0.22d);
        }
        return List.copyOf(bars);
    }

    private TrendDirection resolveWeeklyTrendDirection(List<WeeklySpendPoint> weeklySpending) {
        if (weeklySpending == null || weeklySpending.size() < 2) {
            return TrendDirection.UNKNOWN;
        }
        BigDecimal previous = weeklySpending.get(weeklySpending.size() - 2).getTotalSpent();
        BigDecimal latest = weeklySpending.get(weeklySpending.size() - 1).getTotalSpent();
        return resolveTrendDirection(previous, latest);
    }

    private TrendDirection resolveTrendDirection(List<Double> points) {
        if (points == null || points.size() < 2) {
            return TrendDirection.UNKNOWN;
        }
        double previous = points.get(points.size() - 2);
        double latest = points.get(points.size() - 1);
        if (latest - previous > 0.04d) {
            return TrendDirection.UP;
        }
        if (previous - latest > 0.04d) {
            return TrendDirection.DOWN;
        }
        return TrendDirection.FLAT;
    }

    private TrendDirection resolveTrendDirection(BigDecimal previous, BigDecimal latest) {
        BigDecimal delta = MoneyUtils.zeroIfNull(latest).subtract(MoneyUtils.zeroIfNull(previous));
        if (delta.compareTo(new BigDecimal("1.00")) > 0) {
            return TrendDirection.UP;
        }
        if (delta.compareTo(new BigDecimal("-1.00")) < 0) {
            return TrendDirection.DOWN;
        }
        return TrendDirection.FLAT;
    }

    private String formatSignedCurrency(BigDecimal amount, String currencyCode) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + MoneyUtils.format(amount, currencyCode);
        }
        return MoneyUtils.format(amount, currencyCode);
    }

    private double ratio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return 0d;
        }
        return clamp01(numerator.divide(denominator, 6, RoundingMode.HALF_UP).doubleValue());
    }

    private double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value));
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
