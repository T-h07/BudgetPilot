package com.budgetpilot.service.export;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.habits.HabitAllowanceRow;
import com.budgetpilot.service.habits.HabitAllowanceSnapshot;
import com.budgetpilot.service.habits.HabitService;
import com.budgetpilot.service.income.IncomeService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.ValidationUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ExportService {
    private static final String DATASET_EXPENSES = "expenses";
    private static final String DATASET_INCOME = "income";
    private static final String DATASET_SAVINGS_ENTRIES = "savingsEntries";
    private static final String DATASET_GOAL_CONTRIBUTIONS = "goalContributions";
    private static final String DATASET_PLANNER_PLANS = "plannerPlans";
    private static final String DATASET_HABITS_SUMMARY = "habitsSummary";
    private static final String DATASET_FAMILY_EXPENSES = "familyExpenses";
    private static final String DATASET_INVESTMENT_TRANSACTIONS = "investmentTransactions";

    private static final Comparator<String> STRING_ASC = Comparator.nullsLast(String::compareTo);
    private static final Comparator<ExpenseEntry> EXPENSE_ASC = Comparator
            .comparing(ExpenseEntry::getExpenseDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ExpenseEntry::getId, STRING_ASC);
    private static final Comparator<IncomeEntry> INCOME_ASC = Comparator
            .comparing(IncomeEntry::getReceivedDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IncomeEntry::getId, STRING_ASC);
    private static final Comparator<SavingsEntry> SAVINGS_ASC = Comparator
            .comparing(SavingsEntry::getEntryDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SavingsEntry::getId, STRING_ASC);
    private static final Comparator<GoalContribution> GOAL_ASC = Comparator
            .comparing(GoalContribution::getContributionDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(GoalContribution::getId, STRING_ASC);
    private static final Comparator<FamilyExpenseEntry> FAMILY_ASC = Comparator
            .comparing(FamilyExpenseEntry::getExpenseDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(FamilyExpenseEntry::getId, STRING_ASC);
    private static final Comparator<InvestmentTransaction> INVESTMENT_TX_ASC = Comparator
            .comparing(InvestmentTransaction::getTransactionDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(InvestmentTransaction::getId, STRING_ASC);
    private static final Comparator<MonthlyPlan> PLAN_ASC = Comparator
            .comparing(MonthlyPlan::getMonth, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(MonthlyPlan::getId, STRING_ASC);
    private static final Comparator<HabitAllowanceRow> HABIT_ROW_ASC = Comparator
            .comparing(HabitAllowanceRow::getRuleId, STRING_ASC);

    private static final Map<String, List<String>> CSV_HEADERS = Map.of(
            DATASET_EXPENSES, List.of(
                    "id", "month", "date", "amount", "plannerBucket", "category", "subcategory", "tag", "paymentMethod", "note"
            ),
            DATASET_INCOME, List.of(
                    "id", "month", "date", "amount", "type", "source", "recurring", "received", "note"
            ),
            DATASET_SAVINGS_ENTRIES, List.of(
                    "id", "bucketId", "month", "date", "amount", "type", "relatedGoalId", "note"
            ),
            DATASET_GOAL_CONTRIBUTIONS, List.of(
                    "id", "goalId", "month", "date", "amount", "type", "sourceType", "sourceRefId", "note"
            ),
            DATASET_PLANNER_PLANS, List.of(
                    "id", "month", "plannedIncome", "fixedCostsBudget", "foodBudget", "transportBudget",
                    "familyBudget", "discretionaryBudget", "savingsPercent", "goalsPercent", "habitPercent",
                    "habitMode", "safetyBufferAmount", "notes"
            ),
            DATASET_HABITS_SUMMARY, List.of(
                    "month", "habitId", "habitName", "habitTag", "enabled", "allocatedCap", "baseline",
                    "spent", "excess", "status", "habitPoolAmount", "totalHabitSpent", "totalHabitExcess"
            ),
            DATASET_FAMILY_EXPENSES, List.of(
                    "id", "familyMemberId", "month", "date", "amount", "expenseType", "note", "relatedExpenseEntryId"
            ),
            DATASET_INVESTMENT_TRANSACTIONS, List.of(
                    "id", "investmentId", "month", "date", "amount", "type", "note"
            )
    );

    private final BudgetStore budgetStore;
    private final HabitService habitService;
    private final IncomeService incomeService;
    private final ObjectMapper mapper;

    public ExportService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.habitService = new HabitService(this.budgetStore);
        this.incomeService = new IncomeService(this.budgetStore);
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ExportResult export(ExportRequest request) {
        ExportRequest req = validateRequest(request);
        List<YearMonth> months = resolveMonthRange(req.getStartMonth(), req.getEndMonth());
        Path outputDir = ensureOutputDir(req.getOutputDir());
        String rangeToken = rangeToken(req.getStartMonth(), req.getEndMonth());

        LinkedHashMap<String, List<Map<String, Object>>> datasets = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> rowCounts = new LinkedHashMap<>();
        List<Path> filesWritten = new ArrayList<>();

        if (req.isIncludeExpenses()) {
            List<Map<String, Object>> rows = mapExpenses(loadExpenses(months, req));
            datasets.put(DATASET_EXPENSES, rows);
            rowCounts.put(DATASET_EXPENSES, rows.size());
        }
        if (req.isIncludeIncome()) {
            List<Map<String, Object>> rows = mapIncome(loadIncome(months));
            datasets.put(DATASET_INCOME, rows);
            rowCounts.put(DATASET_INCOME, rows.size());
        }
        if (req.isIncludeSavingsEntries()) {
            List<Map<String, Object>> rows = mapSavingsEntries(loadSavingsEntries(months));
            datasets.put(DATASET_SAVINGS_ENTRIES, rows);
            rowCounts.put(DATASET_SAVINGS_ENTRIES, rows.size());
        }
        if (req.isIncludeGoalContributions()) {
            List<Map<String, Object>> rows = mapGoalContributions(loadGoalContributions(months));
            datasets.put(DATASET_GOAL_CONTRIBUTIONS, rows);
            rowCounts.put(DATASET_GOAL_CONTRIBUTIONS, rows.size());
        }
        if (req.isIncludePlannerPlans()) {
            List<Map<String, Object>> rows = mapPlannerPlans(loadPlannerPlans(months));
            datasets.put(DATASET_PLANNER_PLANS, rows);
            rowCounts.put(DATASET_PLANNER_PLANS, rows.size());
        }
        if (req.isIncludeHabitsSummary()) {
            List<Map<String, Object>> rows = mapHabitsSummary(months);
            datasets.put(DATASET_HABITS_SUMMARY, rows);
            rowCounts.put(DATASET_HABITS_SUMMARY, rows.size());
        }
        if (req.isIncludeFamilyExpenses()) {
            List<Map<String, Object>> rows = mapFamilyExpenses(loadFamilyExpenses(months));
            datasets.put(DATASET_FAMILY_EXPENSES, rows);
            rowCounts.put(DATASET_FAMILY_EXPENSES, rows.size());
        }
        if (req.isIncludeInvestmentTransactions()) {
            List<Map<String, Object>> rows = mapInvestmentTransactions(loadInvestmentTransactions(months));
            datasets.put(DATASET_INVESTMENT_TRANSACTIONS, rows);
            rowCounts.put(DATASET_INVESTMENT_TRANSACTIONS, rows.size());
        }

        if (req.getFormat() == ExportFormat.CSV) {
            for (Map.Entry<String, List<Map<String, Object>>> entry : datasets.entrySet()) {
                String datasetKey = entry.getKey();
                List<String> headers = CSV_HEADERS.get(datasetKey);
                if (headers == null) {
                    continue;
                }
                Path filePath = outputDir.resolve("BudgetPilot_" + rangeToken + "_" + datasetFileToken(datasetKey) + ".csv");
                writeCsv(filePath, headers, entry.getValue());
                filesWritten.add(filePath);
            }
        } else {
            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            root.put("meta", buildMeta(req));
            for (Map.Entry<String, List<Map<String, Object>>> entry : datasets.entrySet()) {
                root.put(entry.getKey(), entry.getValue());
            }

            Path filePath = outputDir.resolve("BudgetPilot_" + rangeToken + "_export.json");
            writeJson(filePath, root);
            filesWritten.add(filePath);
        }

        String message = "Export completed: " + filesWritten.size() + " file(s) written.";
        return new ExportResult(filesWritten, rowCounts, message);
    }

    private ExportRequest validateRequest(ExportRequest request) {
        ExportRequest req = ValidationUtils.requireNonNull(request, "request");
        if (!req.hasAnyDatasetSelected()) {
            throw new IllegalArgumentException("Select at least one dataset to export.");
        }
        if (req.getStartMonth().isAfter(req.getEndMonth())) {
            throw new IllegalArgumentException("Start month must be before or equal to end month.");
        }
        return req;
    }

    private Path ensureOutputDir(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
            if (!Files.isDirectory(outputDir)) {
                throw new IllegalArgumentException("Output path is not a directory: " + outputDir);
            }
            return outputDir;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create output directory: " + outputDir, ex);
        }
    }

    private List<YearMonth> resolveMonthRange(YearMonth startMonth, YearMonth endMonth) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return List.copyOf(months);
    }

    private String rangeToken(YearMonth startMonth, YearMonth endMonth) {
        if (Objects.equals(startMonth, endMonth)) {
            return startMonth.toString();
        }
        return startMonth + "_to_" + endMonth;
    }

    private String datasetFileToken(String datasetKey) {
        return switch (datasetKey) {
            case DATASET_SAVINGS_ENTRIES -> "savings_transactions";
            case DATASET_GOAL_CONTRIBUTIONS -> "goal_contributions";
            case DATASET_PLANNER_PLANS -> "planner_plan";
            case DATASET_HABITS_SUMMARY -> "habits_summary";
            case DATASET_FAMILY_EXPENSES -> "family_expenses";
            case DATASET_INVESTMENT_TRANSACTIONS -> "investment_transactions";
            default -> datasetKey;
        };
    }

    private Map<String, Object> buildMeta(ExportRequest request) {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("app", "BudgetPilot");
        meta.put("exportedAt", LocalDateTime.now());

        LinkedHashMap<String, Object> range = new LinkedHashMap<>();
        range.put("startMonth", request.getStartMonth().toString());
        range.put("endMonth", request.getEndMonth().toString());
        meta.put("range", range);

        meta.put("formatVersion", 1);
        return meta;
    }

    private List<ExpenseEntry> loadExpenses(List<YearMonth> months, ExportRequest request) {
        List<ExpenseEntry> entries = new ArrayList<>();
        for (YearMonth month : months) {
            entries.addAll(budgetStore.listExpenseEntries(month));
        }
        entries.removeIf(entry -> !matchesExpenseFilters(entry, request));
        entries.sort(EXPENSE_ASC);
        return List.copyOf(entries);
    }

    private List<IncomeEntry> loadIncome(List<YearMonth> months) {
        List<IncomeEntry> entries = new ArrayList<>();
        for (YearMonth month : months) {
            entries.addAll(budgetStore.listIncomeEntries(month));
        }
        entries.sort(INCOME_ASC);
        return List.copyOf(entries);
    }

    private List<SavingsEntry> loadSavingsEntries(List<YearMonth> months) {
        List<SavingsEntry> entries = new ArrayList<>();
        for (YearMonth month : months) {
            entries.addAll(budgetStore.listAllSavingsEntries(month));
        }
        entries.sort(SAVINGS_ASC);
        return List.copyOf(entries);
    }

    private List<GoalContribution> loadGoalContributions(List<YearMonth> months) {
        List<GoalContribution> entries = new ArrayList<>();
        for (YearMonth month : months) {
            entries.addAll(budgetStore.listAllGoalContributions(month));
        }
        entries.sort(GOAL_ASC);
        return List.copyOf(entries);
    }

    private List<MonthlyPlan> loadPlannerPlans(List<YearMonth> months) {
        List<MonthlyPlan> plans = new ArrayList<>();
        for (YearMonth month : months) {
            MonthlyPlan plan = budgetStore.getMonthlyPlan(month);
            if (plan != null) {
                plans.add(plan);
            }
        }
        plans.sort(PLAN_ASC);
        return List.copyOf(plans);
    }

    private List<FamilyExpenseEntry> loadFamilyExpenses(List<YearMonth> months) {
        List<FamilyExpenseEntry> entries = new ArrayList<>();
        for (YearMonth month : months) {
            entries.addAll(budgetStore.listFamilyExpenseEntries(month));
        }
        entries.sort(FAMILY_ASC);
        return List.copyOf(entries);
    }

    private List<InvestmentTransaction> loadInvestmentTransactions(List<YearMonth> months) {
        List<InvestmentTransaction> entries = new ArrayList<>();
        for (YearMonth month : months) {
            entries.addAll(budgetStore.listAllInvestmentTransactions(month));
        }
        entries.sort(INVESTMENT_TX_ASC);
        return List.copyOf(entries);
    }

    private boolean matchesExpenseFilters(ExpenseEntry entry, ExportRequest request) {
        if (entry == null) {
            return false;
        }

        PlannerBucket bucket = resolveBucket(entry);
        if (request.getExpenseBucketFilter() != null && bucket != request.getExpenseBucketFilter()) {
            return false;
        }
        if (request.getExpenseCategoryFilter() != null && entry.getCategory() != request.getExpenseCategoryFilter()) {
            return false;
        }
        if (request.hasExpenseTagFilter() && !containsIgnoreCase(entry.getTag(), request.getExpenseTagContains())) {
            return false;
        }
        if (request.hasExpenseSubcategoryFilter()
                && !containsIgnoreCase(entry.getSubcategory(), request.getExpenseSubcategoryContains())) {
            return false;
        }
        return true;
    }

    private PlannerBucket resolveBucket(ExpenseEntry entry) {
        if (entry.getPlannerBucket() != null) {
            return entry.getPlannerBucket();
        }
        return PlannerBucket.inferFromCategory(entry.getCategory());
    }

    private boolean containsIgnoreCase(String source, String searchLower) {
        if (source == null || searchLower == null || searchLower.isBlank()) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(searchLower);
    }

    private List<Map<String, Object>> mapExpenses(List<ExpenseEntry> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExpenseEntry entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.getId());
            row.put("month", entry.getMonth());
            row.put("date", entry.getExpenseDate());
            row.put("amount", entry.getAmount());
            row.put("plannerBucket", resolveBucket(entry).name());
            row.put("category", entry.getCategory() == null ? "" : entry.getCategory().name());
            row.put("subcategory", empty(entry.getSubcategory()));
            row.put("tag", empty(entry.getTag()));
            row.put("paymentMethod", entry.getPaymentMethod() == null ? "" : entry.getPaymentMethod().name());
            row.put("note", empty(entry.getNote()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapIncome(List<IncomeEntry> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (IncomeEntry entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.getId());
            row.put("month", entry.getMonth());
            row.put("date", entry.getReceivedDate());
            row.put("amount", entry.getAmount());
            row.put("type", entry.getIncomeType() == null ? "" : entry.getIncomeType().name());
            row.put("source", empty(entry.getSourceName()));
            row.put("recurring", entry.isRecurring());
            row.put("received", entry.isReceived());
            row.put("note", empty(entry.getNotes()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapSavingsEntries(List<SavingsEntry> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SavingsEntry entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.getId());
            row.put("bucketId", entry.getBucketId());
            row.put("month", entry.getMonth());
            row.put("date", entry.getEntryDate());
            row.put("amount", entry.getAmount());
            row.put("type", entry.getEntryType() == null ? "" : entry.getEntryType().name());
            row.put("relatedGoalId", empty(entry.getRelatedGoalId()));
            row.put("note", empty(entry.getNote()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapGoalContributions(List<GoalContribution> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (GoalContribution entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.getId());
            row.put("goalId", entry.getGoalId());
            row.put("month", entry.getMonth());
            row.put("date", entry.getContributionDate());
            row.put("amount", entry.getAmount());
            row.put("type", entry.getType() == null ? "" : entry.getType().name());
            row.put("sourceType", entry.getSourceType() == null ? "" : entry.getSourceType().name());
            row.put("sourceRefId", empty(entry.getSourceRefId()));
            row.put("note", empty(entry.getNote()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapPlannerPlans(List<MonthlyPlan> plans) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MonthlyPlan plan : plans) {
            BigDecimal plannedIncome = incomeService.getPlannedIncomeTotal(plan.getMonth());
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", plan.getId());
            row.put("month", plan.getMonth());
            row.put("plannedIncome", plannedIncome);
            row.put("fixedCostsBudget", plan.getFixedCostsBudget());
            row.put("foodBudget", plan.getFoodBudget());
            row.put("transportBudget", plan.getTransportBudget());
            row.put("familyBudget", plan.getFamilyBudget());
            row.put("discretionaryBudget", plan.getDiscretionaryBudget());
            row.put("savingsPercent", plan.getSavingsPercent());
            row.put("goalsPercent", plan.getGoalsPercent());
            row.put("habitPercent", plan.getHabitPercent());
            row.put("habitMode", plan.getHabitMode() == null ? "" : plan.getHabitMode().name());
            row.put("safetyBufferAmount", plan.getSafetyBufferAmount());
            row.put("notes", empty(plan.getNotes()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapHabitsSummary(List<YearMonth> months) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (YearMonth month : months) {
            HabitAllowanceSnapshot snapshot = habitService.buildAllowanceSnapshot(month, true);
            List<HabitAllowanceRow> monthRows = new ArrayList<>(snapshot.getRows());
            monthRows.sort(HABIT_ROW_ASC);

            if (monthRows.isEmpty()) {
                LinkedHashMap<String, Object> emptyRow = new LinkedHashMap<>();
                emptyRow.put("month", month);
                emptyRow.put("habitId", "");
                emptyRow.put("habitName", "");
                emptyRow.put("habitTag", "");
                emptyRow.put("enabled", false);
                emptyRow.put("allocatedCap", BigDecimal.ZERO.setScale(2));
                emptyRow.put("baseline", BigDecimal.ZERO.setScale(2));
                emptyRow.put("spent", BigDecimal.ZERO.setScale(2));
                emptyRow.put("excess", BigDecimal.ZERO.setScale(2));
                emptyRow.put("status", "N/A");
                emptyRow.put("habitPoolAmount", snapshot.getHabitPoolAmount());
                emptyRow.put("totalHabitSpent", snapshot.getSpentAcrossHabits());
                emptyRow.put("totalHabitExcess", snapshot.getExcessAcrossHabits());
                rows.add(emptyRow);
                continue;
            }

            for (HabitAllowanceRow rowData : monthRows) {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                row.put("month", month);
                row.put("habitId", rowData.getRuleId());
                row.put("habitName", rowData.getDisplayName());
                row.put("habitTag", rowData.getTag());
                row.put("enabled", rowData.isEffectiveEnabled());
                row.put("allocatedCap", rowData.getFinalCapAmount());
                row.put("baseline", rowData.getBaselineAmount());
                row.put("spent", rowData.getSpentAmount());
                row.put("excess", rowData.getExcessSpentAmount());
                row.put("status", rowData.getStatus().name());
                row.put("habitPoolAmount", snapshot.getHabitPoolAmount());
                row.put("totalHabitSpent", snapshot.getSpentAcrossHabits());
                row.put("totalHabitExcess", snapshot.getExcessAcrossHabits());
                rows.add(row);
            }
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapFamilyExpenses(List<FamilyExpenseEntry> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FamilyExpenseEntry entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.getId());
            row.put("familyMemberId", entry.getFamilyMemberId());
            row.put("month", entry.getMonth());
            row.put("date", entry.getExpenseDate());
            row.put("amount", entry.getAmount());
            row.put("expenseType", entry.getExpenseType() == null ? "" : entry.getExpenseType().name());
            row.put("note", empty(entry.getNote()));
            row.put("relatedExpenseEntryId", empty(entry.getRelatedExpenseEntryId()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private List<Map<String, Object>> mapInvestmentTransactions(List<InvestmentTransaction> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InvestmentTransaction entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.getId());
            row.put("investmentId", entry.getInvestmentId());
            row.put("month", entry.getMonth());
            row.put("date", entry.getTransactionDate());
            row.put("amount", entry.getAmount());
            row.put("type", entry.getType() == null ? "" : entry.getType().name());
            row.put("note", empty(entry.getNote()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    private void writeCsv(Path filePath, List<String> headers, List<Map<String, Object>> rows) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            writer.write(joinCsv(headers));
            writer.newLine();

            for (Map<String, Object> row : rows) {
                List<String> values = new ArrayList<>(headers.size());
                for (String column : headers) {
                    Object value = row == null ? "" : row.getOrDefault(column, "");
                    values.add(csvValue(value));
                }
                writer.write(String.join(",", values));
                writer.newLine();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed writing CSV export: " + filePath, ex);
        }
    }

    private String joinCsv(List<String> values) {
        List<String> escaped = new ArrayList<>(values.size());
        for (String value : values) {
            escaped.add(csvValue(value));
        }
        return String.join(",", escaped);
    }

    private String csvValue(Object value) {
        String raw = value == null ? "" : String.valueOf(value);
        boolean needsQuotes = raw.contains(",")
                || raw.contains("\"")
                || raw.contains("\n")
                || raw.contains("\r");
        if (!needsQuotes) {
            return raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private void writeJson(Path filePath, Map<String, Object> root) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), root);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed writing JSON export: " + filePath, ex);
        }
    }
}
