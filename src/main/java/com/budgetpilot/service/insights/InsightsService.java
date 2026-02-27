package com.budgetpilot.service.insights;

import com.budgetpilot.core.PageId;
import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.balance.MonthlyBalanceService;
import com.budgetpilot.service.balance.MonthlyBalanceSnapshot;
import com.budgetpilot.service.expenses.ExpenseCategorySummary;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.forecast.ForecastService;
import com.budgetpilot.service.forecast.ForecastSummary;
import com.budgetpilot.service.habits.HabitPageSummary;
import com.budgetpilot.service.habits.HabitService;
import com.budgetpilot.service.planner.PlanVsActualRow;
import com.budgetpilot.service.planner.PlannerService;
import com.budgetpilot.service.templates.TemplateService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InsightsService {
    private static final int MAX_INSIGHTS = 10;
    private static final BigDecimal LOW_AVAILABLE_FLOOR = new BigDecimal("50.00");
    private static final BigDecimal LOW_AVAILABLE_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal PLANNER_DANGER_OVERRUN = new BigDecimal("25.00");
    private static final BigDecimal UNPLANNED_AMOUNT_WARNING = new BigDecimal("50.00");
    private static final BigDecimal UNPLANNED_PERCENT_WARNING = new BigDecimal("10.00");
    private static final BigDecimal CATEGORY_CONCENTRATION_WARNING = new BigDecimal("50.00");
    private static final int DUE_SOON_WINDOW_DAYS = 3;
    private static final int DUE_WEEK_WINDOW_DAYS = 7;
    private static final DateTimeFormatter DUE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());

    private final BudgetStore budgetStore;
    private final MonthlyBalanceService monthlyBalanceService;
    private final PlannerService plannerService;
    private final ForecastService forecastService;
    private final ExpenseService expenseService;
    private final HabitService habitService;
    private final TemplateService templateService;

    public InsightsService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.monthlyBalanceService = new MonthlyBalanceService(budgetStore);
        this.plannerService = new PlannerService(budgetStore);
        this.forecastService = new ForecastService(budgetStore);
        this.expenseService = new ExpenseService(budgetStore);
        this.habitService = new HabitService(budgetStore);
        this.templateService = new TemplateService(budgetStore);
    }

    public List<InsightItem> buildInsights(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        UserProfile profile = budgetStore.getUserProfile();
        String currencyCode = resolveCurrencyCode(profile);
        boolean familyEnabled = profile != null && profile.isFamilyModuleEnabled();
        LocalDateTime createdAt = LocalDateTime.now();

        Map<String, InsightItem> insights = new LinkedHashMap<>();

        MonthlyBalanceSnapshot balance = monthlyBalanceService.buildSnapshot(targetMonth);
        addBalanceInsights(insights, balance, currencyCode, createdAt);
        addPlannerInsights(insights, targetMonth, familyEnabled, currencyCode, createdAt);
        addUnplannedSpendInsight(insights, targetMonth, currencyCode, createdAt);
        addForecastInsights(insights, targetMonth, familyEnabled, currencyCode, createdAt);
        addHabitInsights(insights, targetMonth, createdAt);
        addTemplateDueSoonInsights(insights, targetMonth, balance, createdAt);
        addCategoryConcentrationInsight(insights, targetMonth, createdAt);

        return insights.values().stream()
                .sorted(Comparator
                        .comparingInt((InsightItem item) -> severityRank(item.getLevel()))
                        .thenComparing(InsightItem::getTitle))
                .limit(MAX_INSIGHTS)
                .toList();
    }

    public InsightsSummary buildSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<InsightItem> items = buildInsights(targetMonth);
        int dangerCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        int successCount = 0;
        for (InsightItem item : items) {
            switch (item.getLevel()) {
                case DANGER -> dangerCount++;
                case WARNING -> warningCount++;
                case INFO -> infoCount++;
                case SUCCESS -> successCount++;
            }
        }
        return new InsightsSummary(
                targetMonth,
                items,
                dangerCount,
                warningCount,
                infoCount,
                successCount
        );
    }

    public int countDanger(YearMonth month) {
        return buildSummary(month).getDangerCount();
    }

    public int countWarning(YearMonth month) {
        return buildSummary(month).getWarningCount();
    }

    public int countActionable(YearMonth month) {
        return buildSummary(month).getActionableCount();
    }

    private void addBalanceInsights(
            Map<String, InsightItem> target,
            MonthlyBalanceSnapshot balance,
            String currencyCode,
            LocalDateTime createdAt
    ) {
        BigDecimal available = balance.getAvailableAfterReserve();
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            putInsight(target, "balance", new InsightItem(
                    "balance-negative",
                    InsightLevel.DANGER,
                    "Available balance is negative",
                    "You are short by " + MoneyUtils.format(available.abs(), currencyCode)
                            + " after savings and goal reserves.",
                    "balance",
                    "Open Planner",
                    PageId.PLANNER,
                    balance.getMonth(),
                    createdAt,
                    false
            ));
            return;
        }

        if (isLowAvailable(available, balance.getPlannedIncome())) {
            putInsight(target, "balance", new InsightItem(
                    "balance-low",
                    InsightLevel.WARNING,
                    "Available balance is low",
                    "Only " + MoneyUtils.format(available, currencyCode)
                            + " remains after reserves. Rebalance before new spend.",
                    "balance",
                    "Open Planner",
                    PageId.PLANNER,
                    balance.getMonth(),
                    createdAt,
                    false
            ));
        }
    }

    private void addPlannerInsights(
            Map<String, InsightItem> target,
            YearMonth month,
            boolean familyEnabled,
            String currencyCode,
            LocalDateTime createdAt
    ) {
        List<PlanVsActualRow> rows = plannerService.getPlanVsActual(month, familyEnabled);
        if (rows.isEmpty()) {
            return;
        }

        PlanVsActualRow dangerRow = rows.stream()
                .filter(row -> row.getActual().compareTo(row.getPlanned()) > 0)
                .filter(row -> row.getActual().subtract(row.getPlanned()).compareTo(PLANNER_DANGER_OVERRUN) > 0)
                .max(Comparator.comparing(row -> row.getActual().subtract(row.getPlanned())))
                .orElse(null);

        if (dangerRow != null) {
            BigDecimal overrun = MoneyUtils.normalize(dangerRow.getActual().subtract(dangerRow.getPlanned()));
            putInsight(target, "planner-overrun", new InsightItem(
                    "planner-overrun-" + dangerRow.getBucket().name().toLowerCase(Locale.ROOT),
                    InsightLevel.DANGER,
                    dangerRow.getBucket().getDisplayName() + " is over budget",
                    "Over by " + MoneyUtils.format(overrun, currencyCode) + ". Rebalance this bucket now.",
                    "planner",
                    "Open Planner",
                    PageId.PLANNER,
                    month,
                    createdAt,
                    false
            ));
            return;
        }

        PlanVsActualRow warningRow = rows.stream()
                .filter(row -> row.getUsagePercent().compareTo(new BigDecimal("80.00")) >= 0)
                .max(Comparator.comparing(PlanVsActualRow::getUsagePercent))
                .orElse(null);

        if (warningRow != null) {
            putInsight(target, "planner-usage", new InsightItem(
                    "planner-usage-" + warningRow.getBucket().name().toLowerCase(Locale.ROOT),
                    InsightLevel.WARNING,
                    warningRow.getBucket().getDisplayName() + " is near limit",
                    warningRow.getUsagePercent().stripTrailingZeros().toPlainString()
                            + "% of planned budget already used.",
                    "planner",
                    "Open Planner",
                    PageId.PLANNER,
                    month,
                    createdAt,
                    false
            ));
        }
    }

    private void addUnplannedSpendInsight(
            Map<String, InsightItem> target,
            YearMonth month,
            String currencyCode,
            LocalDateTime createdAt
    ) {
        BigDecimal unplanned = expenseService.getTotalForBucket(month, PlannerBucket.UNPLANNED);
        if (unplanned.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal totalSpent = expenseService.getTotalExpenses(month);
        BigDecimal percent = totalSpent.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyUtils.normalize(unplanned.multiply(MoneyUtils.HUNDRED).divide(totalSpent, 2, RoundingMode.HALF_UP));

        boolean highUnplanned = unplanned.compareTo(UNPLANNED_AMOUNT_WARNING) > 0
                || percent.compareTo(UNPLANNED_PERCENT_WARNING) > 0;
        putInsight(target, "expenses-unplanned", new InsightItem(
                highUnplanned ? "unplanned-warning" : "unplanned-info",
                highUnplanned ? InsightLevel.WARNING : InsightLevel.INFO,
                highUnplanned ? "Unplanned spend is high" : "Unplanned spend detected",
                "Unplanned spending: " + MoneyUtils.format(unplanned, currencyCode)
                        + " (" + percent.stripTrailingZeros().toPlainString() + "% of total spend).",
                "expenses",
                "Open Expenses",
                PageId.EXPENSES,
                month,
                createdAt,
                false
        ));
    }

    private void addForecastInsights(
            Map<String, InsightItem> target,
            YearMonth month,
            boolean familyEnabled,
            String currencyCode,
            LocalDateTime createdAt
    ) {
        ForecastSummary forecast = forecastService.buildForecast(month, familyEnabled);
        if (forecast.isOverspendingRisk()) {
            putInsight(target, "forecast", new InsightItem(
                    "forecast-risk",
                    InsightLevel.DANGER,
                    "Forecast shows overspending risk",
                    forecast.getStatusMessage(),
                    "forecast",
                    "Open Planner",
                    PageId.PLANNER,
                    month,
                    createdAt,
                    false
            ));
            return;
        }

        if (!forecast.isPlanMissing()) {
            putInsight(target, "forecast", new InsightItem(
                    "forecast-stable",
                    InsightLevel.INFO,
                    "Forecast is stable",
                    "Projected spend is " + MoneyUtils.format(forecast.getProjectedExpensesByMonthEnd(), currencyCode)
                            + " and currently within plan.",
                    "forecast",
                    "Open Dashboard",
                    PageId.DASHBOARD,
                    month,
                    createdAt,
                    false
            ));
        }
    }

    private void addHabitInsights(
            Map<String, InsightItem> target,
            YearMonth month,
            LocalDateTime createdAt
    ) {
        HabitPageSummary summary = habitService.getHabitPageSummary(month);
        if (summary.getExceededCount() > 0) {
            putInsight(target, "habits", new InsightItem(
                    "habits-exceeded",
                    InsightLevel.DANGER,
                    "Habit limits exceeded",
                    summary.getExceededCount() + " active habit rule(s) exceeded monthly limits.",
                    "habits",
                    "Open Habits",
                    PageId.HABITS,
                    month,
                    createdAt,
                    false
            ));
            return;
        }

        if (summary.getWarningCount() > 0) {
            putInsight(target, "habits", new InsightItem(
                    "habits-warning",
                    InsightLevel.WARNING,
                    "Habit warnings active",
                    summary.getWarningCount() + " active habit rule(s) are close to their limits.",
                    "habits",
                    "Open Habits",
                    PageId.HABITS,
                    month,
                    createdAt,
                    false
            ));
            return;
        }

        if (summary.getActiveRulesCount() > 0) {
            putInsight(target, "habits", new InsightItem(
                    "habits-healthy",
                    InsightLevel.SUCCESS,
                    "Habit rules are on track",
                    "All " + summary.getActiveRulesCount() + " active habit rule(s) are within safe range.",
                    "habits",
                    "Open Habits",
                    PageId.HABITS,
                    month,
                    createdAt,
                    false
            ));
        }
    }

    private void addTemplateDueSoonInsights(
            Map<String, InsightItem> target,
            YearMonth month,
            MonthlyBalanceSnapshot balance,
            LocalDateTime createdAt
    ) {
        if (!MonthUtils.isCurrentMonth(month)) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<DueTemplate> dueSoon = collectDueTemplates(month, today, DUE_SOON_WINDOW_DAYS);
        if (!dueSoon.isEmpty()) {
            DueTemplate first = dueSoon.get(0);
            String dueText = formatDueIn(first.daysUntil());
            String suffix = dueSoon.size() > 1
                    ? " " + (dueSoon.size() - 1) + " more recurring item(s) due soon."
                    : "";
            putInsight(target, "templates-due", new InsightItem(
                    "template-due-" + slug(first.label()),
                    InsightLevel.INFO,
                    first.label() + " " + dueText,
                    "Recurring template due on " + first.dueDate().format(DUE_DATE_FORMAT) + "." + suffix,
                    "templates",
                    "Open Templates",
                    PageId.TEMPLATES,
                    month,
                    createdAt,
                    false
            ));
        }

        boolean lowAvailable = isLowAvailable(balance.getAvailableAfterReserve(), balance.getPlannedIncome());
        if (lowAvailable) {
            int dueThisWeekCount = collectDueTemplates(month, today, DUE_WEEK_WINDOW_DAYS).size();
            if (dueThisWeekCount > 0) {
                putInsight(target, "templates-low-available", new InsightItem(
                        "templates-due-low-available",
                        InsightLevel.WARNING,
                        "Recurring items due with low availability",
                        dueThisWeekCount + " recurring template item(s) are due this week while available cash is low.",
                        "templates",
                        "Open Planner",
                        PageId.PLANNER,
                        month,
                        createdAt,
                        false
                ));
            }
        }
    }

    private void addCategoryConcentrationInsight(
            Map<String, InsightItem> target,
            YearMonth month,
            LocalDateTime createdAt
    ) {
        List<ExpenseCategorySummary> categories = expenseService.getCategorySummaries(month);
        if (categories.isEmpty()) {
            return;
        }
        ExpenseCategorySummary top = categories.get(0);
        if (top.getPercentOfTotal().compareTo(CATEGORY_CONCENTRATION_WARNING) <= 0) {
            return;
        }

        putInsight(target, "expenses-concentration", new InsightItem(
                "category-concentration-" + top.getCategory().name().toLowerCase(Locale.ROOT),
                InsightLevel.WARNING,
                "Spending concentration detected",
                top.getCategory().getLabel() + " is " + top.getPercentOfTotal().stripTrailingZeros().toPlainString()
                        + "% of monthly spend.",
                "expenses",
                "Open Expenses",
                PageId.EXPENSES,
                month,
                createdAt,
                false
        ));
    }

    private List<DueTemplate> collectDueTemplates(YearMonth month, LocalDate today, int maxDaysOut) {
        List<DueTemplate> due = new ArrayList<>();

        for (ExpenseTemplate template : templateService.listActiveExpenseTemplates()) {
            if (!isTemplateDue(template.getDayOfMonth(), month, today, maxDaysOut)) {
                continue;
            }
            String label = template.getSubcategory() == null || template.getSubcategory().isBlank()
                    ? template.getName()
                    : template.getSubcategory();
            due.add(new DueTemplate(
                    safeLabel(label, "Recurring expense"),
                    dayDistance(today, month, template.getDayOfMonth()),
                    resolveDueDate(month, template.getDayOfMonth())
            ));
        }

        for (IncomeTemplate template : templateService.listActiveIncomeTemplates()) {
            if (!isTemplateDue(template.getDayOfMonth(), month, today, maxDaysOut)) {
                continue;
            }
            due.add(new DueTemplate(
                    safeLabel(template.getSourceName(), "Recurring income"),
                    dayDistance(today, month, template.getDayOfMonth()),
                    resolveDueDate(month, template.getDayOfMonth())
            ));
        }

        due.sort(Comparator
                .comparingLong(DueTemplate::daysUntil)
                .thenComparing(DueTemplate::label));
        return List.copyOf(due);
    }

    private boolean isTemplateDue(
            int dayOfMonth,
            YearMonth month,
            LocalDate today,
            int maxDaysOut
    ) {
        if (!MonthUtils.isCurrentMonth(month) || today == null) {
            return false;
        }
        // BP-PT13 templates currently run rollover generation as monthly placeholders across cadences.
        long daysUntil = dayDistance(today, month, dayOfMonth);
        return daysUntil >= 0 && daysUntil <= maxDaysOut;
    }

    private long dayDistance(LocalDate today, YearMonth month, int dayOfMonth) {
        LocalDate dueDate = resolveDueDate(month, dayOfMonth);
        return ChronoUnit.DAYS.between(today, dueDate);
    }

    private LocalDate resolveDueDate(YearMonth month, int dayOfMonth) {
        int safeDay = Math.max(1, Math.min(dayOfMonth, month.lengthOfMonth()));
        return month.atDay(safeDay);
    }

    private boolean isLowAvailable(BigDecimal availableAfterReserve, BigDecimal plannedIncome) {
        BigDecimal available = MoneyUtils.zeroIfNull(availableAfterReserve);
        if (available.compareTo(LOW_AVAILABLE_FLOOR) < 0) {
            return true;
        }
        BigDecimal income = MoneyUtils.zeroIfNull(plannedIncome);
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal threshold = MoneyUtils.normalize(income.multiply(LOW_AVAILABLE_PERCENT));
        return available.compareTo(threshold) < 0;
    }

    private void putInsight(Map<String, InsightItem> target, String topicKey, InsightItem insight) {
        InsightItem existing = target.get(topicKey);
        if (existing == null || severityRank(insight.getLevel()) < severityRank(existing.getLevel())) {
            target.put(topicKey, insight);
        }
    }

    private int severityRank(InsightLevel level) {
        return switch (level) {
            case DANGER -> 0;
            case WARNING -> 1;
            case INFO, SUCCESS -> 2;
        };
    }

    private String formatDueIn(long daysUntil) {
        if (daysUntil <= 0) {
            return "due today";
        }
        return "due in " + daysUntil + (daysUntil == 1 ? " day" : " days");
    }

    private String resolveCurrencyCode(UserProfile profile) {
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
    }

    private String safeLabel(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
    }

    private String slug(String value) {
        if (value == null || value.isBlank()) {
            return "item";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private record DueTemplate(String label, long daysUntil, LocalDate dueDate) {
    }
}
