package com.budgetpilot.service.analytics;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.analytics.dto.AnalyticsSnapshot;
import com.budgetpilot.service.analytics.dto.BucketPlanActualPoint;
import com.budgetpilot.service.analytics.dto.CategorySharePoint;
import com.budgetpilot.service.analytics.dto.ForecastAccuracyPoint;
import com.budgetpilot.service.analytics.dto.ForecastAccuracySeries;
import com.budgetpilot.service.analytics.dto.HabitTrendPoint;
import com.budgetpilot.service.analytics.dto.HabitTrendSeries;
import com.budgetpilot.service.analytics.dto.MonthPoint;
import com.budgetpilot.service.analytics.dto.PlanVsActualBreakdown;
import com.budgetpilot.service.analytics.dto.TopSubcategoryRow;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.habits.HabitPageSummary;
import com.budgetpilot.service.habits.HabitService;
import com.budgetpilot.service.income.IncomeService;
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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AnalyticsService {
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault());
    private static final int FORECAST_SAMPLE_DAYS = 7;
    private static final int TOP_SUBCATEGORY_LIMIT = 10;

    private final BudgetStore budgetStore;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final HabitService habitService;

    public AnalyticsService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.expenseService = new ExpenseService(budgetStore);
        this.incomeService = new IncomeService(budgetStore);
        this.habitService = new HabitService(budgetStore);
    }

    public AnalyticsSnapshot buildSnapshot(AnalyticsQuery query) {
        AnalyticsQuery safeQuery = ValidationUtils.requireNonNull(query, "query");
        List<YearMonth> months = resolveMonthRange(safeQuery.getEndMonth(), safeQuery.getMonthsBack());
        Map<YearMonth, List<ExpenseEntry>> expensesByMonth = loadExpenses(months);

        List<MonthPoint> monthlySpendTrend = new ArrayList<>();
        List<MonthPoint> monthlyIncomeTrend = new ArrayList<>();
        List<MonthPoint> monthlyUnplannedTrend = new ArrayList<>();

        BigDecimal totalSpendInRange = BigDecimal.ZERO.setScale(2);
        BigDecimal bestMonthSpend = null;
        BigDecimal worstMonthSpend = null;

        for (YearMonth month : months) {
            List<ExpenseEntry> monthEntries = expensesByMonth.getOrDefault(month, List.of());
            List<ExpenseEntry> filteredEntries = filterEntries(monthEntries, safeQuery);
            BigDecimal monthSpend = sumAmounts(filteredEntries);
            BigDecimal monthIncomePlanned = incomeService.getPlannedIncomeTotal(month);
            BigDecimal monthUnplanned = sumAmounts(filteredEntries.stream()
                    .filter(entry -> resolveBucket(entry) == PlannerBucket.UNPLANNED)
                    .toList());

            String label = month.format(MONTH_LABEL_FORMATTER);
            monthlySpendTrend.add(new MonthPoint(month, label, monthSpend));
            monthlyIncomeTrend.add(new MonthPoint(month, label, monthIncomePlanned));
            monthlyUnplannedTrend.add(new MonthPoint(month, label, monthUnplanned));

            totalSpendInRange = MoneyUtils.normalize(totalSpendInRange.add(monthSpend));
            if (bestMonthSpend == null || monthSpend.compareTo(bestMonthSpend) < 0) {
                bestMonthSpend = monthSpend;
            }
            if (worstMonthSpend == null || monthSpend.compareTo(worstMonthSpend) > 0) {
                worstMonthSpend = monthSpend;
            }
        }

        BigDecimal avgMonthlySpend = months.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : MoneyUtils.normalize(totalSpendInRange.divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP));

        YearMonth endMonth = safeQuery.getEndMonth();
        List<ExpenseEntry> endMonthEntries = expensesByMonth.getOrDefault(endMonth, List.of());

        PlanVsActualBreakdown planVsActual = buildPlanVsActualBreakdown(endMonth, safeQuery, endMonthEntries);
        List<CategorySharePoint> categoryShare = buildCategoryShare(filterEntries(endMonthEntries, safeQuery));
        ForecastAccuracySeries forecastAccuracy = buildForecastAccuracySeries(safeQuery, months, expensesByMonth);
        List<TopSubcategoryRow> topSubcategories = buildTopSubcategories(filterEntries(endMonthEntries, safeQuery));
        HabitTrendSeries habitTrend = buildHabitTrendSeries(months);

        return new AnalyticsSnapshot(
                endMonth,
                safeQuery.getMonthsBack(),
                monthlySpendTrend,
                monthlyIncomeTrend,
                monthlyUnplannedTrend,
                planVsActual,
                categoryShare,
                forecastAccuracy,
                topSubcategories,
                habitTrend,
                totalSpendInRange,
                avgMonthlySpend,
                bestMonthSpend == null ? BigDecimal.ZERO.setScale(2) : bestMonthSpend,
                worstMonthSpend == null ? BigDecimal.ZERO.setScale(2) : worstMonthSpend
        );
    }

    private Map<YearMonth, List<ExpenseEntry>> loadExpenses(List<YearMonth> months) {
        Map<YearMonth, List<ExpenseEntry>> map = new LinkedHashMap<>();
        for (YearMonth month : months) {
            map.put(month, expenseService.listForMonth(month));
        }
        return map;
    }

    private List<YearMonth> resolveMonthRange(YearMonth endMonth, int monthsBack) {
        YearMonth safeEnd = ValidationUtils.requireNonNull(endMonth, "endMonth");
        int safeMonths = Math.max(1, monthsBack);
        List<YearMonth> months = new ArrayList<>(safeMonths);
        YearMonth start = safeEnd.minusMonths(safeMonths - 1L);
        for (int i = 0; i < safeMonths; i++) {
            months.add(start.plusMonths(i));
        }
        return List.copyOf(months);
    }

    private List<ExpenseEntry> filterEntries(List<ExpenseEntry> entries, AnalyticsQuery query) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(entry -> query.getBucketFilter() == null || resolveBucket(entry) == query.getBucketFilter())
                .filter(entry -> query.getCategoryFilter() == null || entry.getCategory() == query.getCategoryFilter())
                .filter(entry -> matchesSearch(entry, query))
                .toList();
    }

    private boolean matchesSearch(ExpenseEntry entry, AnalyticsQuery query) {
        if (!query.hasSearchFilter()) {
            return true;
        }
        String search = query.getTagOrSubcategorySearch();
        return containsIgnoreCase(entry.getTag(), search)
                || containsIgnoreCase(entry.getSubcategory(), search);
    }

    private boolean containsIgnoreCase(String source, String searchLower) {
        if (source == null || searchLower == null || searchLower.isBlank()) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(searchLower);
    }

    private PlannerBucket resolveBucket(ExpenseEntry entry) {
        if (entry == null) {
            return PlannerBucket.DISCRETIONARY;
        }
        if (entry.getPlannerBucket() != null) {
            return entry.getPlannerBucket();
        }
        return PlannerBucket.inferFromCategory(entry.getCategory());
    }

    private PlanVsActualBreakdown buildPlanVsActualBreakdown(
            YearMonth month,
            AnalyticsQuery query,
            List<ExpenseEntry> monthEntries
    ) {
        MonthlyPlan plan = budgetStore.getMonthlyPlan(month);
        boolean hasPlan = plan != null;

        List<PlannerBucket> buckets = analyticsBuckets(isFamilyModuleEnabled());
        PlannerBucket bucketFilter = query.getBucketFilter();
        if (bucketFilter != null) {
            if (bucketFilter == PlannerBucket.UNPLANNED) {
                buckets = List.of();
            } else {
                buckets = buckets.stream()
                        .filter(bucket -> bucket == bucketFilter)
                        .toList();
            }
        }

        List<BucketPlanActualPoint> rows = new ArrayList<>();
        for (PlannerBucket bucket : buckets) {
            BigDecimal planned = hasPlan ? plannedForBucket(plan, bucket) : BigDecimal.ZERO.setScale(2);
            BigDecimal actual = sumAmounts(monthEntries.stream()
                    .filter(entry -> resolveBucket(entry) == bucket)
                    .toList());
            rows.add(new BucketPlanActualPoint(bucket, bucket.getDisplayName(), planned, actual));
        }

        BigDecimal unplanned = sumAmounts(monthEntries.stream()
                .filter(entry -> resolveBucket(entry) == PlannerBucket.UNPLANNED)
                .toList());

        return new PlanVsActualBreakdown(month, hasPlan, rows, unplanned);
    }

    private List<PlannerBucket> analyticsBuckets(boolean familyEnabled) {
        List<PlannerBucket> buckets = new ArrayList<>(List.of(
                PlannerBucket.FIXED_COSTS,
                PlannerBucket.FOOD,
                PlannerBucket.TRANSPORT
        ));
        if (familyEnabled) {
            buckets.add(PlannerBucket.FAMILY);
        }
        buckets.add(PlannerBucket.DISCRETIONARY);
        return List.copyOf(buckets);
    }

    private BigDecimal plannedForBucket(MonthlyPlan plan, PlannerBucket bucket) {
        return switch (Objects.requireNonNull(bucket, "bucket")) {
            case FIXED_COSTS -> MoneyUtils.zeroIfNull(plan.getFixedCostsBudget());
            case FOOD -> MoneyUtils.zeroIfNull(plan.getFoodBudget());
            case TRANSPORT -> MoneyUtils.zeroIfNull(plan.getTransportBudget());
            case FAMILY -> MoneyUtils.zeroIfNull(plan.getFamilyBudget());
            case DISCRETIONARY -> MoneyUtils.zeroIfNull(plan.getDiscretionaryBudget());
            case UNPLANNED -> BigDecimal.ZERO.setScale(2);
        };
    }

    private List<CategorySharePoint> buildCategoryShare(List<ExpenseEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        BigDecimal total = sumAmounts(entries);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        Map<ExpenseCategory, CategoryAggregate> aggregateByCategory = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseEntry entry : entries) {
            ExpenseCategory category = entry.getCategory();
            CategoryAggregate aggregate = aggregateByCategory.computeIfAbsent(category, key -> new CategoryAggregate());
            aggregate.total = MoneyUtils.normalize(aggregate.total.add(entry.getAmount()));
            aggregate.count++;
        }

        return aggregateByCategory.entrySet().stream()
                .map(entry -> {
                    BigDecimal percent = MoneyUtils.normalize(
                            entry.getValue().total.multiply(MoneyUtils.HUNDRED).divide(total, 2, RoundingMode.HALF_UP)
                    );
                    return new CategorySharePoint(
                            entry.getKey(),
                            entry.getKey().getLabel(),
                            entry.getValue().total,
                            percent,
                            entry.getValue().count
                    );
                })
                .sorted(Comparator.comparing(CategorySharePoint::getTotal, Comparator.reverseOrder()))
                .toList();
    }

    private ForecastAccuracySeries buildForecastAccuracySeries(
            AnalyticsQuery query,
            List<YearMonth> months,
            Map<YearMonth, List<ExpenseEntry>> expensesByMonth
    ) {
        List<ForecastAccuracyPoint> points = new ArrayList<>();
        YearMonth currentMonth = MonthUtils.currentMonth();

        for (YearMonth month : months) {
            if (!month.isBefore(currentMonth)) {
                continue;
            }
            List<ExpenseEntry> filtered = filterEntries(expensesByMonth.getOrDefault(month, List.of()), query);
            BigDecimal actual = sumAmounts(filtered);

            int sampleDays = Math.min(FORECAST_SAMPLE_DAYS, month.lengthOfMonth());
            BigDecimal weekOneSpend = sumAmounts(filtered.stream()
                    .filter(entry -> entry.getExpenseDate() != null && entry.getExpenseDate().getDayOfMonth() <= sampleDays)
                    .toList());
            BigDecimal projected = sampleDays <= 0
                    ? BigDecimal.ZERO.setScale(2)
                    : MoneyUtils.normalize(
                    weekOneSpend
                            .divide(BigDecimal.valueOf(sampleDays), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(month.lengthOfMonth()))
            );

            points.add(new ForecastAccuracyPoint(
                    month,
                    month.format(MONTH_LABEL_FORMATTER),
                    projected,
                    actual
            ));
        }

        return new ForecastAccuracySeries(points);
    }

    private List<TopSubcategoryRow> buildTopSubcategories(List<ExpenseEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        Map<String, SubcategoryAggregate> aggregates = new LinkedHashMap<>();
        for (ExpenseEntry entry : entries) {
            String label = resolveDriverLabel(entry);
            SubcategoryAggregate aggregate = aggregates.computeIfAbsent(label, key -> new SubcategoryAggregate());
            aggregate.total = MoneyUtils.normalize(aggregate.total.add(entry.getAmount()));
            aggregate.count++;
        }

        return aggregates.entrySet().stream()
                .map(entry -> new TopSubcategoryRow(entry.getKey(), entry.getValue().total, entry.getValue().count))
                .sorted(Comparator
                        .comparing(TopSubcategoryRow::getAmount, Comparator.reverseOrder())
                        .thenComparing(TopSubcategoryRow::getLabel))
                .limit(TOP_SUBCATEGORY_LIMIT)
                .toList();
    }

    private String resolveDriverLabel(ExpenseEntry entry) {
        if (entry.getSubcategory() != null && !entry.getSubcategory().isBlank()) {
            return entry.getSubcategory().trim();
        }
        if (entry.getTag() != null && !entry.getTag().isBlank()) {
            return entry.getTag().trim();
        }
        ExpenseCategory category = entry.getCategory();
        return category == null ? "Other" : category.getLabel();
    }

    private HabitTrendSeries buildHabitTrendSeries(List<YearMonth> months) {
        List<HabitTrendPoint> points = new ArrayList<>();
        for (YearMonth month : months) {
            HabitPageSummary summary = habitService.getHabitPageSummary(month);
            int active = summary.getActiveRulesCount();
            int onTrack = summary.getOnTrackCount();
            BigDecimal compliance = active <= 0
                    ? BigDecimal.ZERO.setScale(2)
                    : MoneyUtils.normalize(
                    BigDecimal.valueOf(onTrack)
                            .multiply(MoneyUtils.HUNDRED)
                            .divide(BigDecimal.valueOf(active), 2, RoundingMode.HALF_UP)
            );
            points.add(new HabitTrendPoint(
                    month,
                    month.format(MONTH_LABEL_FORMATTER),
                    active,
                    summary.getWarningCount(),
                    summary.getExceededCount(),
                    onTrack,
                    compliance
            ));
        }
        return new HabitTrendSeries(points);
    }

    private BigDecimal sumAmounts(List<ExpenseEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return MoneyUtils.normalize(entries.stream()
                .map(ExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private boolean isFamilyModuleEnabled() {
        UserProfile profile = budgetStore.getUserProfile();
        return profile != null && profile.isFamilyModuleEnabled();
    }

    private static final class CategoryAggregate {
        private BigDecimal total = BigDecimal.ZERO.setScale(2);
        private int count = 0;
    }

    private static final class SubcategoryAggregate {
        private BigDecimal total = BigDecimal.ZERO.setScale(2);
        private int count = 0;
    }
}
