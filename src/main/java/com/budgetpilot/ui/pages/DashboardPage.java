package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.service.dashboard.CategorySpendPoint;
import com.budgetpilot.service.dashboard.DashboardKpi;
import com.budgetpilot.service.dashboard.DashboardMetricsService;
import com.budgetpilot.service.dashboard.DashboardSnapshot;
import com.budgetpilot.service.dashboard.WeeklySpendPoint;
import com.budgetpilot.ui.components.InsightListCard;
import com.budgetpilot.ui.components.KpiChartTile;
import com.budgetpilot.ui.components.MetricRow;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.StatusBadge;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.UiUtils;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.util.List;

public class DashboardPage extends VBox {
    public DashboardPage(AppContext appContext) {
        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-dashboard");

        DashboardMetricsService metricsService = new DashboardMetricsService(appContext.getStore());
        DashboardSnapshot snapshot = metricsService.buildSnapshot(appContext.getSelectedMonth());
        String currencyCode = resolveCurrencyCode(appContext);

        getChildren().add(UiUtils.createPageHeader(
                "Dashboard",
                "Financial command center for " + snapshot.getMonthDisplayText()
                        + ". Plan, track, forecast, and act from one view."
        ));

        GridPane kpiGrid = buildKpiGrid(snapshot.getKpis());
        HBox mainGrid = UiUtils.createTwoColumn(
                buildCategoryPanel(snapshot, currencyCode),
                buildWeeklyTrendPanel(snapshot, currencyCode)
        );
        mainGrid.getStyleClass().add("dashboard-main-grid");

        HBox lowerGrid = UiUtils.createTwoColumn(
                buildPlannerVsActualPanel(snapshot, currencyCode),
                buildHealthPanel(snapshot, currencyCode)
        );
        lowerGrid.getStyleClass().add("dashboard-lower-grid");

        SectionCard alertsPanel = buildAlertsPanel(snapshot);

        getChildren().addAll(kpiGrid, mainGrid, lowerGrid, alertsPanel);
    }

    private GridPane buildKpiGrid(List<DashboardKpi> kpis) {
        GridPane kpiGrid = new GridPane();
        kpiGrid.getStyleClass().add("dashboard-kpi-grid");
        kpiGrid.setHgap(UiUtils.CARD_GAP);
        kpiGrid.setVgap(UiUtils.CARD_GAP);

        int index = 0;
        for (DashboardKpi kpi : kpis) {
            KpiChartTile tile = new KpiChartTile();
            tile.setContent(kpi);
            int col = index % 4;
            int row = index / 4;
            kpiGrid.add(tile, col, row);
            GridPane.setHgrow(tile, Priority.ALWAYS);
            GridPane.setVgrow(tile, Priority.ALWAYS);
            GridPane.setFillWidth(tile, true);
            GridPane.setFillHeight(tile, true);
            tile.setMaxWidth(Double.MAX_VALUE);
            index++;
        }
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            kpiGrid.getColumnConstraints().add(column);
        }
        int rowCount = Math.max(1, (int) Math.ceil(kpis.size() / 4.0));
        for (int i = 0; i < rowCount; i++) {
            RowConstraints row = new RowConstraints();
            row.setPrefHeight(154);
            row.setMinHeight(154);
            row.setVgrow(Priority.ALWAYS);
            kpiGrid.getRowConstraints().add(row);
        }
        return kpiGrid;
    }

    private SectionCard buildCategoryPanel(DashboardSnapshot snapshot, String currencyCode) {
        VBox content = new VBox(10);
        content.getStyleClass().add("dashboard-category-list");
        if (snapshot.getCategorySpending().isEmpty()) {
            Label empty = new Label("No category spending yet for this month.");
            empty.getStyleClass().add("empty-panel-state");
            content.getChildren().add(empty);
        } else {
            for (CategorySpendPoint point : snapshot.getCategorySpending()) {
                VBox row = new VBox(6);
                row.getStyleClass().add("category-row");

                HBox head = new HBox(8);
                Label categoryLabel = new Label(point.getCategoryLabel() + " (" + point.getEntryCount() + ")");
                categoryLabel.getStyleClass().add("muted-text");
                Label valueLabel = new Label(
                        MoneyUtils.format(point.getTotal(), currencyCode)
                                + " | "
                                + point.getPercentOfTotal().toPlainString() + "%"
                );
                valueLabel.getStyleClass().add("info-row-value");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                head.getChildren().addAll(categoryLabel, spacer, valueLabel);

                ProgressBar progressBar = new ProgressBar(point.getPercentOfTotal().doubleValue() / 100.0);
                progressBar.getStyleClass().add("category-progress");
                progressBar.setMaxWidth(Double.MAX_VALUE);
                row.getChildren().addAll(head, progressBar);
                content.getChildren().add(row);
            }
        }
        SectionCard card = new SectionCard(
                "Spending by Category",
                "Dominant categories and their share of monthly spend.",
                content
        );
        card.getStyleClass().addAll("dashboard-panel", "chart-card");
        return card;
    }

    private SectionCard buildWeeklyTrendPanel(DashboardSnapshot snapshot, String currencyCode) {
        VBox content = new VBox(10);
        List<WeeklySpendPoint> points = snapshot.getWeeklySpending();
        boolean hasData = points.stream().anyMatch(point -> point.getTotalSpent().compareTo(java.math.BigDecimal.ZERO) > 0);
        if (!hasData) {
            Label empty = new Label("No weekly spending trend available yet.");
            empty.getStyleClass().add("empty-panel-state");
            content.getChildren().add(empty);
        } else {
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Week");
            yAxis.setLabel("Spend");

            BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
            chart.setLegendVisible(false);
            chart.setAnimated(false);
            chart.setCategoryGap(14);
            chart.setBarGap(4);
            chart.setMinHeight(260);
            chart.getStyleClass().add("dashboard-weekly-chart");

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (WeeklySpendPoint point : points) {
                series.getData().add(new XYChart.Data<>(point.getWeekLabel(), point.getTotalSpent()));
            }
            chart.getData().add(series);

            WeeklySpendPoint first = points.get(0);
            WeeklySpendPoint last = points.get(points.size() - 1);
            Label footnote = UiUtils.createMutedLabel(
                    "Range: " + first.getDateRangeLabel() + " to " + last.getDateRangeLabel()
                            + " | Currency: " + currencyCode
            );
            content.getChildren().addAll(chart, footnote);
        }
        SectionCard card = new SectionCard(
                "Weekly Spending Trend",
                "Week-by-week spend distribution for the selected month.",
                content
        );
        card.getStyleClass().addAll("dashboard-panel", "chart-card");
        return card;
    }

    private SectionCard buildPlannerVsActualPanel(DashboardSnapshot snapshot, String currencyCode) {
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new MetricRow("Planned Income", MoneyUtils.format(snapshot.getPlannerVsActual().getPlannedIncome(), currencyCode)),
                new MetricRow("Total Spent (Actual)", MoneyUtils.format(snapshot.getPlannerVsActual().getTotalSpentActual(), currencyCode)),
                new MetricRow("Projected Month-End Spend", MoneyUtils.format(snapshot.getPlannerVsActual().getProjectedMonthEndSpend(), currencyCode)),
                new MetricRow("Planned Expense Budget", MoneyUtils.format(snapshot.getPlannerVsActual().getPlannedExpenseBudget(), currencyCode)),
                new MetricRow("Planned Savings", MoneyUtils.format(snapshot.getPlannerVsActual().getPlannedSavingsAmount(), currencyCode)),
                new MetricRow("Planned Goals", MoneyUtils.format(snapshot.getPlannerVsActual().getPlannedGoalsAmount(), currencyCode)),
                new MetricRow("Remaining Planned", MoneyUtils.format(snapshot.getPlannerVsActual().getPlannerRemainingPlanned(), currencyCode)),
                new MetricRow("Projected Remaining", MoneyUtils.format(snapshot.getPlannerVsActual().getProjectedRemainingAfterPlan(), currencyCode))
        );

        if (!snapshot.isHasMonthlyPlan()) {
            Label warning = new Label("No monthly plan found. Planner comparisons are limited.");
            warning.getStyleClass().add("status-warn");
            content.getChildren().add(warning);
        }

        SectionCard card = new SectionCard(
                "Planner vs Actual",
                "Compare plan assumptions against current and projected spend.",
                content
        );
        card.getStyleClass().add("dashboard-panel");
        return card;
    }

    private SectionCard buildHealthPanel(DashboardSnapshot snapshot, String currencyCode) {
        VBox content = new VBox(10);
        content.getStyleClass().add("health-card");

        Label scoreLabel = new Label(snapshot.getBudgetHealthScore() + "/100");
        scoreLabel.getStyleClass().add("health-score");

        StatusBadge healthBadge = new StatusBadge();
        healthBadge.setText(snapshot.getBudgetHealthLabel());
        healthBadge.setStatus(snapshot.getBudgetHealthLabel());

        Label statusMessage = new Label(snapshot.getPrimaryStatusMessage());
        statusMessage.getStyleClass().add("muted-text");
        statusMessage.setWrapText(true);

        StatusBadge forecastBadge = new StatusBadge();
        forecastBadge.setText(snapshot.isForecastOverspendingRisk() ? "Forecast Risk" : "Forecast Stable");
        forecastBadge.setStatus(snapshot.isForecastOverspendingRisk() ? "danger" : "good");

        StatusBadge plannerBadge = new StatusBadge();
        plannerBadge.setText(snapshot.isPlannerOverallocated() ? "Planner Overallocated" : "Planner Healthy");
        plannerBadge.setStatus(snapshot.isPlannerOverallocated() ? "warn" : "good");

        StatusBadge familyBadge = new StatusBadge();
        boolean familyOverBudget = snapshot.getFamilyBudgetPlanned().compareTo(java.math.BigDecimal.ZERO) > 0
                && snapshot.getFamilyCostsActual().compareTo(snapshot.getFamilyBudgetPlanned()) > 0;
        if (snapshot.getActiveFamilyMembersCount() <= 0) {
            familyBadge.setText("Family Module Idle");
            familyBadge.setStatus("info");
        } else {
            familyBadge.setText(familyOverBudget ? "Family Over Budget" : "Family On Track");
            familyBadge.setStatus(familyOverBudget ? "warn" : "good");
        }

        StatusBadge habitBadge = new StatusBadge();
        if (snapshot.getHabitExceededCount() > 0) {
            habitBadge.setText(snapshot.getHabitExceededCount() + " Habit Exceeded");
            habitBadge.setStatus("danger");
        } else if (snapshot.getHabitWarningCount() > 0) {
            habitBadge.setText(snapshot.getHabitWarningCount() + " Habit Warning");
            habitBadge.setStatus("warn");
        } else {
            habitBadge.setText("Habits On Track");
            habitBadge.setStatus("good");
        }

        StatusBadge investmentBadge = new StatusBadge();
        if (snapshot.getActiveInvestmentsCount() <= 0) {
            investmentBadge.setText("No Investments");
            investmentBadge.setStatus("info");
        } else if (snapshot.getPortfolioNetProfit().compareTo(java.math.BigDecimal.ZERO) >= 0) {
            investmentBadge.setText("Investments Positive");
            investmentBadge.setStatus("good");
        } else {
            investmentBadge.setText("Investments Negative");
            investmentBadge.setStatus("warn");
        }

        StatusBadge achievementBadge = new StatusBadge();
        if (snapshot.getUnlockedAchievementsCount() <= 0) {
            achievementBadge.setText("No Unlocks Yet");
            achievementBadge.setStatus("info");
        } else {
            achievementBadge.setText(snapshot.getUnlockedAchievementsCount() + " Achievements");
            achievementBadge.setStatus("good");
        }

        HBox badges = new HBox(8, forecastBadge, plannerBadge, familyBadge, habitBadge, investmentBadge, achievementBadge);
        content.getChildren().addAll(
                scoreLabel,
                healthBadge,
                statusMessage,
                new MetricRow("Projected Spend", MoneyUtils.format(snapshot.getProjectedMonthEndSpend(), currencyCode)),
                new MetricRow("Projected Remaining", MoneyUtils.format(snapshot.getProjectedRemainingAfterPlan(), currencyCode)),
                new MetricRow("Savings Total", MoneyUtils.format(snapshot.getSavingsCurrentTotal(), currencyCode)),
                new MetricRow("Goals Total", MoneyUtils.format(snapshot.getGoalsCurrentTotal(), currencyCode)),
                new MetricRow("Family Costs", MoneyUtils.format(snapshot.getFamilyCostsActual(), currencyCode)),
                new MetricRow("Family Budget", MoneyUtils.format(snapshot.getFamilyBudgetPlanned(), currencyCode)),
                new MetricRow("Habit Tracked Spend", MoneyUtils.format(snapshot.getHabitTrackedSpend(), currencyCode)),
                new MetricRow("Habit Risk", snapshot.getHabitWarningCount() + " warning / "
                        + snapshot.getHabitExceededCount() + " exceeded"),
                new MetricRow("Portfolio Value", MoneyUtils.format(snapshot.getPortfolioEstimatedValue(), currencyCode)),
                new MetricRow("Portfolio Net Profit", MoneyUtils.format(snapshot.getPortfolioNetProfit(), currencyCode)),
                new MetricRow("Active Investments", String.valueOf(snapshot.getActiveInvestmentsCount())),
                new MetricRow("Achievements", snapshot.getUnlockedAchievementsCount() + " unlocked / "
                        + snapshot.getAchievementInProgressCount() + " in progress"),
                new MetricRow("Achievements Completion", snapshot.getAchievementsCompletionPercent().stripTrailingZeros().toPlainString() + "%"),
                badges
        );

        SectionCard card = new SectionCard(
                "Forecast & Health",
                "Health score, risks, and projected month-end position.",
                content
        );
        card.getStyleClass().add("dashboard-panel");
        return card;
    }

    private SectionCard buildAlertsPanel(DashboardSnapshot snapshot) {
        InsightListCard insightList = new InsightListCard();
        insightList.setAlerts(snapshot.getAlerts());
        SectionCard card = new SectionCard(
                "Alerts & Insights",
                "Actionable signals generated from planner, expenses, and forecast.",
                insightList
        );
        card.getStyleClass().addAll("dashboard-panel", "dashboard-alerts-panel");
        return card;
    }

    private String resolveCurrencyCode(AppContext appContext) {
        UserProfile profile = appContext.getCurrentUser();
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
    }
}

