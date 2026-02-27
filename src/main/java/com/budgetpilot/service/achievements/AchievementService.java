package com.budgetpilot.service.achievements;

import com.budgetpilot.model.Goal;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.AchievementCategory;
import com.budgetpilot.model.enums.AchievementStatus;
import com.budgetpilot.model.enums.AchievementTier;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.family.FamilyService;
import com.budgetpilot.service.family.FamilySummary;
import com.budgetpilot.service.forecast.ForecastService;
import com.budgetpilot.service.forecast.ForecastSummary;
import com.budgetpilot.service.goals.GoalProgressSummary;
import com.budgetpilot.service.goals.GoalService;
import com.budgetpilot.service.goals.GoalSummary;
import com.budgetpilot.service.habits.HabitPageSummary;
import com.budgetpilot.service.habits.HabitService;
import com.budgetpilot.service.income.IncomeService;
import com.budgetpilot.service.investments.InvestmentPageSummary;
import com.budgetpilot.service.investments.InvestmentPositionSummary;
import com.budgetpilot.service.investments.InvestmentService;
import com.budgetpilot.service.planner.BudgetSummary;
import com.budgetpilot.service.planner.PlannerService;
import com.budgetpilot.service.savings.SavingsService;
import com.budgetpilot.service.savings.SavingsSummary;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AchievementService {
    private static final List<AchievementDefinition> DEFINITIONS = List.of(
            new AchievementDefinition("FIRST_PROFILE", "First Profile", "Complete your profile to start the BudgetPilot journey.", AchievementCategory.FOUNDATION, AchievementTier.BRONZE, one(), "Finish onboarding with your profile details."),
            new AchievementDefinition("FIRST_PLAN", "First Plan", "Create your first monthly plan.", AchievementCategory.FOUNDATION, AchievementTier.BRONZE, one(), "Open Planner and save a monthly plan."),
            new AchievementDefinition("FIRST_INCOME", "Income Starter", "Add at least one income entry.", AchievementCategory.FOUNDATION, AchievementTier.BRONZE, one(), "Add your first income source in Income."),
            new AchievementDefinition("FIRST_EXPENSE", "Expense Starter", "Log your first expense entry.", AchievementCategory.FOUNDATION, AchievementTier.BRONZE, one(), "Track one expense in Expenses."),

            new AchievementDefinition("EXPENSE_LOGGER_10", "Expense Logger 10", "Track 10 expenses in a month.", AchievementCategory.TRACKING, AchievementTier.BRONZE, value(10), "Keep logging expenses to reach 10 entries."),
            new AchievementDefinition("EXPENSE_LOGGER_50", "Expense Logger 50", "Track 50 expenses in a month.", AchievementCategory.TRACKING, AchievementTier.SILVER, value(50), "Consistent daily tracking unlocks this milestone."),
            new AchievementDefinition("INCOME_STREAMS_3", "Income Streams", "Maintain at least 3 income streams in a month.", AchievementCategory.TRACKING, AchievementTier.SILVER, value(3), "Add additional income sources or side hustles."),

            new AchievementDefinition("HEALTHY_PLAN", "Healthy Plan", "Build a plan that is not overallocated.", AchievementCategory.PLANNING, AchievementTier.SILVER, one(), "Adjust planner budgets until remaining planned is non-negative."),
            new AchievementDefinition("FORECAST_SAFE", "Forecast Safe", "Keep month-end forecast out of overspending risk.", AchievementCategory.PLANNING, AchievementTier.SILVER, one(), "Reduce spending pace and rebalance allocations."),

            new AchievementDefinition("FIRST_BUCKET", "First Bucket", "Create your first savings bucket.", AchievementCategory.SAVINGS, AchievementTier.BRONZE, one(), "Create a savings bucket to start building reserves."),
            new AchievementDefinition("SAVE_100", "Saver 100", "Reach 100 saved across all buckets.", AchievementCategory.SAVINGS, AchievementTier.BRONZE, value(100), "Contribute to savings buckets regularly."),
            new AchievementDefinition("SAVE_500", "Saver 500", "Reach 500 saved across all buckets.", AchievementCategory.SAVINGS, AchievementTier.SILVER, value(500), "Keep contributions consistent to reach 500."),

            new AchievementDefinition("FIRST_GOAL", "Goal Setter", "Create your first financial goal.", AchievementCategory.GOALS, AchievementTier.BRONZE, one(), "Create a goal in Goals page."),
            new AchievementDefinition("GOAL_PROGRESS_25", "Goal Momentum", "Reach 25% progress on any goal.", AchievementCategory.GOALS, AchievementTier.SILVER, value(25), "Contribute to at least one goal."),
            new AchievementDefinition("GOAL_PROGRESS_100", "Goal Completed", "Fully fund any goal.", AchievementCategory.GOALS, AchievementTier.GOLD, value(100), "Stay consistent until a goal reaches 100%."),

            new AchievementDefinition("FIRST_HABIT_RULE", "Habit Architect", "Create your first habit rule.", AchievementCategory.HABITS, AchievementTier.BRONZE, one(), "Add a habit rule in Habits page."),
            new AchievementDefinition("HABIT_CONTROL", "Habit Control", "Keep all active habits on track this month.", AchievementCategory.HABITS, AchievementTier.GOLD, one(), "Reduce habit overspending and warnings."),

            new AchievementDefinition("FAMILY_SUPPORTER", "Family Supporter", "Add at least one active family member.", AchievementCategory.FAMILY, AchievementTier.BRONZE, one(), "Enable Family module and add a member."),
            new AchievementDefinition("FAMILY_PLANNED", "Family Planned", "Track family costs within planned family budget.", AchievementCategory.FAMILY, AchievementTier.SILVER, one(), "Log family expenses and keep them within plan."),

            new AchievementDefinition("FIRST_INVESTMENT", "First Investment", "Create your first investment.", AchievementCategory.INVESTMENTS, AchievementTier.BRONZE, one(), "Add an investment to track growth assets."),
            new AchievementDefinition("INVESTOR_ACTIVE", "Investor Active", "Record 3 investment transactions.", AchievementCategory.INVESTMENTS, AchievementTier.SILVER, value(3), "Add contributions or returns to investments."),
            new AchievementDefinition("ROI_POSITIVE", "ROI Positive", "Have at least one investment with positive ROI.", AchievementCategory.INVESTMENTS, AchievementTier.GOLD, one(), "Update estimated value or add returns."),

            new AchievementDefinition("DASHBOARD_COMMANDER", "Dashboard Commander", "Populate plan, income, expense, savings, and goals together.", AchievementCategory.MILESTONES, AchievementTier.GOLD, value(5), "Use multiple modules to unlock this cross-module milestone."),
            new AchievementDefinition("DISCIPLINED_MONTH", "Disciplined Month", "Plan, avoid forecast risk, log expenses, and contribute to savings.", AchievementCategory.MILESTONES, AchievementTier.GOLD, value(4), "Keep a balanced month across planning, tracking, and savings.")
    );

    private final BudgetStore budgetStore;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final PlannerService plannerService;
    private final ForecastService forecastService;
    private final SavingsService savingsService;
    private final GoalService goalService;
    private final HabitService habitService;
    private final FamilyService familyService;
    private final InvestmentService investmentService;

    public AchievementService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.incomeService = new IncomeService(budgetStore);
        this.expenseService = new ExpenseService(budgetStore);
        this.plannerService = new PlannerService(budgetStore);
        this.forecastService = new ForecastService(budgetStore);
        this.savingsService = new SavingsService(budgetStore);
        this.goalService = new GoalService(budgetStore);
        this.habitService = new HabitService(budgetStore);
        this.familyService = new FamilyService(budgetStore);
        this.investmentService = new InvestmentService(budgetStore);
    }

    public List<AchievementProgress> evaluateAll(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");

        Metrics metrics = buildMetrics(targetMonth);
        List<AchievementProgress> progressList = new ArrayList<>();
        for (AchievementDefinition definition : DEFINITIONS) {
            BigDecimal currentValue = metrics.values().getOrDefault(definition.getCode(), BigDecimal.ZERO.setScale(2));
            progressList.add(toProgress(definition, currentValue));
        }

        progressList.sort(Comparator
                .comparingInt((AchievementProgress progress) -> statusRank(progress.getStatus()))
                .thenComparing(AchievementProgress::getProgressPercent, Comparator.reverseOrder())
                .thenComparing(AchievementProgress::getTitle));

        return List.copyOf(progressList);
    }

    public AchievementPageSummary getAchievementPageSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<AchievementProgress> all = evaluateAll(targetMonth);

        int unlocked = (int) all.stream().filter(item -> item.getStatus() == AchievementStatus.UNLOCKED).count();
        int inProgress = (int) all.stream().filter(item -> item.getStatus() == AchievementStatus.IN_PROGRESS).count();
        int locked = (int) all.stream().filter(item -> item.getStatus() == AchievementStatus.LOCKED).count();

        int bronzeUnlocked = countUnlockedByTier(all, AchievementTier.BRONZE);
        int silverUnlocked = countUnlockedByTier(all, AchievementTier.SILVER);
        int goldUnlocked = countUnlockedByTier(all, AchievementTier.GOLD);

        BigDecimal completion = all.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyUtils.normalize(BigDecimal.valueOf(unlocked)
                .multiply(MoneyUtils.HUNDRED)
                .divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP));

        return new AchievementPageSummary(
                targetMonth,
                all.size(),
                unlocked,
                inProgress,
                locked,
                bronzeUnlocked,
                silverUnlocked,
                goldUnlocked,
                completion,
                all
        );
    }

    public List<AchievementInsight> getInsights(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        AchievementPageSummary summary = getAchievementPageSummary(targetMonth);

        List<AchievementInsight> insights = new ArrayList<>();

        List<AchievementProgress> inProgress = summary.getAchievements().stream()
                .filter(item -> item.getStatus() == AchievementStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(AchievementProgress::getProgressPercent, Comparator.reverseOrder()))
                .toList();

        if (!inProgress.isEmpty()) {
            AchievementProgress closest = inProgress.get(0);
            insights.add(new AchievementInsight(
                    "closest-unlock",
                    "Closest Unlock",
                    "You are close to unlocking " + closest.getTitle() + " (" + closest.getProgressText() + ").",
                    InsightLevel.INFO,
                    closest.getUnlockHint()
            ));
        }

        if (summary.getUnlockedCount() > 0) {
            insights.add(new AchievementInsight(
                    "unlocked-count",
                    "Unlocked Achievements",
                    "You have unlocked " + summary.getUnlockedCount() + " achievements so far.",
                    InsightLevel.SUCCESS,
                    "Keep momentum by advancing in-progress achievements."
            ));
        }

        if (summary.getCompletionPercent().compareTo(new BigDecimal("50.00")) >= 0) {
            insights.add(new AchievementInsight(
                    "halfway",
                    "Halfway Milestone",
                    "Achievement completion is at " + summary.getCompletionPercent().stripTrailingZeros().toPlainString() + "%.",
                    InsightLevel.SUCCESS,
                    "Push for Gold-tier milestones next."
            ));
        }

        if (summary.getInProgressCount() == 0 && summary.getLockedCount() > 0) {
            insights.add(new AchievementInsight(
                    "no-progress",
                    "No Active Progress",
                    "You have locked achievements but no active progress this month.",
                    InsightLevel.WARN,
                    "Use Planner, Expenses, Savings, or Investments to start progress."
            ));
        }

        if (summary.getUnlockedCount() == 0) {
            insights.add(new AchievementInsight(
                    "first-unlock",
                    "First Unlock Pending",
                    "Complete foundational actions to unlock your first achievement.",
                    InsightLevel.INFO,
                    "Start with profile, plan, and first income/expense entries."
            ));
        }

        if (insights.isEmpty()) {
            insights.add(new AchievementInsight(
                    "steady",
                    "Steady Progress",
                    "Your achievement board is balanced and progressing well.",
                    InsightLevel.SUCCESS,
                    "Keep consistent monthly habits to unlock more tiers."
            ));
        }

        return List.copyOf(insights.stream().limit(5).toList());
    }

    public List<AchievementProgress> filterByCategory(List<AchievementProgress> source, AchievementCategory category) {
        if (category == null) {
            return List.copyOf(source == null ? List.of() : source);
        }
        return (source == null ? List.<AchievementProgress>of() : source).stream()
                .filter(item -> item.getCategory() == category)
                .toList();
    }

    public List<AchievementProgress> filterByStatus(List<AchievementProgress> source, AchievementStatus status) {
        if (status == null) {
            return List.copyOf(source == null ? List.of() : source);
        }
        return (source == null ? List.<AchievementProgress>of() : source).stream()
                .filter(item -> item.getStatus() == status)
                .toList();
    }

    private Metrics buildMetrics(YearMonth month) {
        UserProfile profile = budgetStore.getUserProfile();
        boolean familyEnabled = profile != null && profile.isFamilyModuleEnabled();

        int incomeCount = incomeService.listForMonth(month).size();
        int expenseCount = expenseService.listForMonth(month).size();
        int incomeSources = (int) incomeService.listForMonth(month).stream()
                .map(entry -> entry.getSourceName().trim().toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        boolean hasPlan = budgetStore.getMonthlyPlan(month) != null;
        BudgetSummary budgetSummary = plannerService.buildBudgetSummary(month, familyEnabled);
        ForecastSummary forecast = forecastService.buildForecast(month, familyEnabled);

        SavingsSummary savingsSummary = savingsService.getSavingsSummary(month);
        GoalSummary goalsSummary = goalService.getGoalsSummary(month);
        HabitPageSummary habitSummary = habitService.getHabitPageSummary(month);
        FamilySummary familySummary = familyService.getFamilySummary(month);
        InvestmentPageSummary investmentSummary = investmentService.getInvestmentPageSummary(month);

        BigDecimal maxGoalProgressPercent = goalService.listGoalProgressSummaries(month).stream()
                .map(GoalProgressSummary::getProgressPercent)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO.setScale(2));

        long investmentTransactions = investmentSummary.getPositions().stream()
                .mapToLong(InvestmentPositionSummary::getTransactionCount)
                .sum();

        boolean hasPositiveRoi = investmentSummary.getPositions().stream()
                .anyMatch(position -> position.getRoiPercent().compareTo(BigDecimal.ZERO) > 0
                        || position.getNetProfitAmount().compareTo(BigDecimal.ZERO) > 0);

        int dashboardCommanderCurrent = 0;
        dashboardCommanderCurrent += hasPlan ? 1 : 0;
        dashboardCommanderCurrent += incomeCount > 0 ? 1 : 0;
        dashboardCommanderCurrent += expenseCount > 0 ? 1 : 0;
        dashboardCommanderCurrent += savingsSummary.getBucketCount() > 0 ? 1 : 0;
        dashboardCommanderCurrent += goalsSummary.getActiveGoalCount() > 0 ? 1 : 0;

        int disciplinedCurrent = 0;
        disciplinedCurrent += hasPlan ? 1 : 0;
        disciplinedCurrent += !forecast.isOverspendingRisk() ? 1 : 0;
        disciplinedCurrent += expenseCount >= 5 ? 1 : 0;
        disciplinedCurrent += savingsSummary.getMonthlyContributions().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;

        Map<String, BigDecimal> values = Map.ofEntries(
                Map.entry("FIRST_PROFILE", bool(profile != null)),
                Map.entry("FIRST_PLAN", bool(hasPlan)),
                Map.entry("FIRST_INCOME", bool(incomeCount > 0)),
                Map.entry("FIRST_EXPENSE", bool(expenseCount > 0)),

                Map.entry("EXPENSE_LOGGER_10", value(expenseCount)),
                Map.entry("EXPENSE_LOGGER_50", value(expenseCount)),
                Map.entry("INCOME_STREAMS_3", value(incomeSources)),

                Map.entry("HEALTHY_PLAN", bool(hasPlan && !budgetSummary.isOverallocated())),
                Map.entry("FORECAST_SAFE", bool(hasPlan && !forecast.isOverspendingRisk())),

                Map.entry("FIRST_BUCKET", bool(savingsSummary.getBucketCount() > 0)),
                Map.entry("SAVE_100", savingsSummary.getTotalCurrentSavings()),
                Map.entry("SAVE_500", savingsSummary.getTotalCurrentSavings()),

                Map.entry("FIRST_GOAL", bool(goalsSummary.getActiveGoalCount() > 0)),
                Map.entry("GOAL_PROGRESS_25", maxGoalProgressPercent),
                Map.entry("GOAL_PROGRESS_100", maxGoalProgressPercent),

                Map.entry("FIRST_HABIT_RULE", bool(habitSummary.getActiveRulesCount() > 0 || !habitService.listRules().isEmpty())),
                Map.entry("HABIT_CONTROL", bool(habitSummary.getActiveRulesCount() > 0
                        && habitSummary.getWarningCount() == 0
                        && habitSummary.getExceededCount() == 0)),

                Map.entry("FAMILY_SUPPORTER", bool(familyEnabled && familySummary.getActiveMembersCount() > 0)),
                Map.entry("FAMILY_PLANNED", bool(familyEnabled
                        && familySummary.getPlannedFamilyBudget().compareTo(BigDecimal.ZERO) > 0
                        && familySummary.getTotalFamilyCosts().compareTo(BigDecimal.ZERO) > 0
                        && familySummary.getFamilyBudgetVariance().compareTo(BigDecimal.ZERO) <= 0)),

                Map.entry("FIRST_INVESTMENT", bool(!investmentSummary.getPositions().isEmpty())),
                Map.entry("INVESTOR_ACTIVE", value(investmentTransactions)),
                Map.entry("ROI_POSITIVE", bool(hasPositiveRoi)),

                Map.entry("DASHBOARD_COMMANDER", value(dashboardCommanderCurrent)),
                Map.entry("DISCIPLINED_MONTH", value(disciplinedCurrent))
        );

        return new Metrics(values);
    }

    private AchievementProgress toProgress(AchievementDefinition definition, BigDecimal currentValue) {
        BigDecimal target = MoneyUtils.zeroIfNull(definition.getTargetValue());
        BigDecimal current = MoneyUtils.zeroIfNull(currentValue);

        BigDecimal percent;
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            percent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            percent = MoneyUtils.normalize(current.multiply(MoneyUtils.HUNDRED).divide(target, 2, RoundingMode.HALF_UP));
            if (percent.compareTo(MoneyUtils.HUNDRED) > 0) {
                percent = MoneyUtils.HUNDRED;
            }
        }

        AchievementStatus status;
        if (current.compareTo(target) >= 0 && target.compareTo(BigDecimal.ZERO) > 0) {
            status = AchievementStatus.UNLOCKED;
        } else if (current.compareTo(BigDecimal.ZERO) > 0) {
            status = AchievementStatus.IN_PROGRESS;
        } else {
            status = AchievementStatus.LOCKED;
        }

        String progressText = formatProgressText(definition.getCode(), current, target);
        String badgeLabel = definition.getTier().getLabel() + " - " + status.getLabel();

        return new AchievementProgress(
                definition.getCode(),
                definition.getTitle(),
                definition.getDescription(),
                definition.getCategory(),
                definition.getTier(),
                status,
                current,
                target,
                percent,
                progressText,
                definition.getUnlockHint(),
                false,
                badgeLabel
        );
    }

    private String formatProgressText(String code, BigDecimal current, BigDecimal target) {
        if (code.startsWith("GOAL_PROGRESS")) {
            return current.stripTrailingZeros().toPlainString() + "% / " + target.stripTrailingZeros().toPlainString() + "%";
        }
        if (code.startsWith("SAVE_")) {
            return current.stripTrailingZeros().toPlainString() + " / " + target.stripTrailingZeros().toPlainString();
        }
        return current.stripTrailingZeros().toPlainString() + " / " + target.stripTrailingZeros().toPlainString();
    }

    private int countUnlockedByTier(List<AchievementProgress> all, AchievementTier tier) {
        return (int) all.stream()
                .filter(item -> item.getTier() == tier)
                .filter(item -> item.getStatus() == AchievementStatus.UNLOCKED)
                .count();
    }

    private int statusRank(AchievementStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 0;
            case UNLOCKED -> 1;
            case LOCKED -> 2;
        };
    }

    private static BigDecimal one() {
        return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bool(boolean value) {
        return value ? one() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private record Metrics(Map<String, BigDecimal> values) {
    }
}
