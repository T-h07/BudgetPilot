package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.ui.components.KpiTile;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.UiUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

public class DashboardPage extends VBox {
    public DashboardPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        BudgetStore store = appContext.getStore();
        YearMonth selectedMonth = appContext.getSelectedMonth();
        String currencyCode = resolveCurrencyCode(appContext, store);

        List<IncomeEntry> incomes = store == null ? List.of() : store.listIncomeEntries(selectedMonth);
        List<ExpenseEntry> expenses = store == null ? List.of() : store.listExpenseEntries(selectedMonth);
        List<Goal> goals = store == null ? List.of() : store.listGoals();
        List<SavingsBucket> savingsBuckets = store == null ? List.of() : store.listSavingsBuckets();
        int habitRulesCount = store == null ? 0 : store.listHabitRules().size();
        MonthlyPlan plan = store == null ? null : store.getMonthlyPlan(selectedMonth);

        BigDecimal totalIncome = incomes.stream()
                .map(IncomeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream()
                .map(ExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingMoney = MoneyUtils.safeSubtract(totalIncome, totalExpenses);
        long activeGoalsCount = goals.stream().filter(Goal::isActive).count();

        String monthText = appContext.getCurrentMonthDisplayText();
        getChildren().add(UiUtils.createPageHeader(
                "Dashboard",
                "Live cockpit for " + monthText + ". Real-time summary from your in-memory financial workspace."
        ));

        GridPane kpiGrid = new GridPane();
        kpiGrid.getStyleClass().add("kpi-grid");
        kpiGrid.setHgap(UiUtils.CARD_GAP);
        kpiGrid.setVgap(UiUtils.CARD_GAP);

        kpiGrid.add(new KpiTile(
                "Money Remaining",
                MoneyUtils.format(remainingMoney, currencyCode),
                "Income " + MoneyUtils.format(totalIncome, currencyCode) + " vs expenses " + MoneyUtils.format(totalExpenses, currencyCode)
        ), 0, 0);

        kpiGrid.add(new KpiTile(
                "Income This Month",
                MoneyUtils.format(totalIncome, currencyCode),
                incomes.size() + " income entries tracked"
        ), 1, 0);

        kpiGrid.add(new KpiTile(
                "Expenses This Month",
                MoneyUtils.format(totalExpenses, currencyCode),
                expenses.size() + " expense entries tracked"
        ), 2, 0);

        kpiGrid.add(new KpiTile(
                "Active Goals",
                String.valueOf(activeGoalsCount),
                goals.size() + " total goals in workspace"
        ), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            kpiGrid.getColumnConstraints().add(column);
        }

        BigDecimal averageExpense = expenses.isEmpty()
                ? BigDecimal.ZERO
                : totalExpenses.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        SectionCard spendingOverview = new SectionCard(
                "Spending Overview",
                "Current-month spending stats based on stored expense entries.",
                createSummaryRows(new String[][]{
                        {"Expense Entries", String.valueOf(expenses.size())},
                        {"Average Expense", MoneyUtils.format(averageExpense, currencyCode)},
                        {"Total Expenses", MoneyUtils.format(totalExpenses, currencyCode)},
                        {"Month", monthText}
                })
        );

        String plannedDiscretionary = plan == null
                ? "No plan seeded"
                : MoneyUtils.format(plan.getDiscretionaryBudget(), currencyCode);
        String plannedSafety = plan == null
                ? "No plan seeded"
                : MoneyUtils.format(plan.getSafetyBufferAmount(), currencyCode);

        SectionCard plannerSummary = new SectionCard(
                "Planner Summary",
                "High-level snapshot from the monthly plan object.",
                createSummaryRows(new String[][]{
                        {"Fixed Costs Budget", plan == null ? "No plan seeded" : MoneyUtils.format(plan.getFixedCostsBudget(), currencyCode)},
                        {"Food Budget", plan == null ? "No plan seeded" : MoneyUtils.format(plan.getFoodBudget(), currencyCode)},
                        {"Discretionary Budget", plannedDiscretionary},
                        {"Safety Buffer", plannedSafety}
                })
        );

        SectionCard dataSnapshot = new SectionCard(
                "Data Snapshot",
                "Store-level counts used to power the dashboard foundation.",
                createSummaryRows(new String[][]{
                        {"Expense Entries", String.valueOf(expenses.size())},
                        {"Savings Buckets", String.valueOf(savingsBuckets.size())},
                        {"Habit Rules", String.valueOf(habitRulesCount)}
                })
        );

        HBox summaryRow = UiUtils.createTwoColumn(spendingOverview, plannerSummary);
        getChildren().addAll(kpiGrid, summaryRow, dataSnapshot);
    }

    private String resolveCurrencyCode(AppContext appContext, BudgetStore store) {
        UserProfile profile = appContext.getCurrentUser();
        if (profile == null && store != null) {
            profile = store.getUserProfile();
        }
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
    }

    private Node createSummaryRows(String[][] rows) {
        VBox container = new VBox(10);
        for (String[] row : rows) {
            HBox line = new HBox();
            line.setAlignment(Pos.CENTER_LEFT);

            Label label = UiUtils.createMutedLabel(row[0]);
            Label value = new Label(row[1]);
            value.getStyleClass().add("info-row-value");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            line.getChildren().addAll(label, spacer, value);
            container.getChildren().add(line);
        }
        return container;
    }
}
