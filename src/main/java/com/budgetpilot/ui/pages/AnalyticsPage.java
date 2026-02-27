package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.analytics.AnalyticsQuery;
import com.budgetpilot.service.analytics.AnalyticsService;
import com.budgetpilot.service.analytics.dto.AnalyticsSnapshot;
import com.budgetpilot.service.analytics.dto.BucketPlanActualPoint;
import com.budgetpilot.service.analytics.dto.CategorySharePoint;
import com.budgetpilot.service.analytics.dto.ForecastAccuracyPoint;
import com.budgetpilot.service.analytics.dto.HabitTrendPoint;
import com.budgetpilot.service.analytics.dto.MonthPoint;
import com.budgetpilot.service.analytics.dto.PlanVsActualBreakdown;
import com.budgetpilot.service.analytics.dto.TopSubcategoryRow;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.SummaryStatCard;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class AnalyticsPage extends VBox {
    private final AppContext appContext;
    private final AnalyticsService analyticsService;
    private final Runnable contextListener = this::refreshSnapshot;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(320));

    private final ComboBox<RangePreset> rangeFilter = new ComboBox<>();
    private final ComboBox<PlannerBucket> bucketFilter = new ComboBox<>();
    private final ComboBox<ExpenseCategory> categoryFilter = new ComboBox<>();
    private final TextField searchField = new TextField();

    private final SummaryStatCard totalSpendCard = new SummaryStatCard();
    private final SummaryStatCard avgMonthlySpendCard = new SummaryStatCard();
    private final SummaryStatCard bestMonthSpendCard = new SummaryStatCard();
    private final SummaryStatCard worstMonthSpendCard = new SummaryStatCard();

    private final LineChart<String, Number> spendTrendChart = createLineChart("spend-trend-chart");
    private final Label spendTrendCaption = UiUtils.createMutedLabel("");

    private final BarChart<String, Number> planVsActualChart = createBarChart("plan-vs-actual-chart");
    private final Label noPlanLabel = UiUtils.createMutedLabel("No monthly plan found; Plan vs Actual is limited.");
    private final Label unplannedInfoLabel = new Label();

    private final VBox categoryShareList = new VBox(8);
    private final VBox topDriversList = new VBox(8);

    private final BarChart<String, Number> forecastAccuracyChart = createBarChart("forecast-accuracy-chart");
    private final Label forecastAccuracyEmptyLabel = UiUtils.createMutedLabel("No completed months in selected range.");

    private final LineChart<String, Number> habitTrendChart = createLineChart("habit-trend-chart");
    private final Label habitTrendCaption = UiUtils.createMutedLabel("");

    public AnalyticsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.analyticsService = new AnalyticsService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-analytics");

        getChildren().add(UiUtils.createPageHeader(
                "Analytics",
                "Trends, plan vs actual, and behavior insights."
        ));

        configureFilters();
        configurePanels();

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshSnapshot();
    }

    private void configureFilters() {
        rangeFilter.getStyleClass().addAll("combo-box", "form-combo");
        rangeFilter.getItems().setAll(RangePreset.values());
        rangeFilter.getSelectionModel().select(RangePreset.LAST_3_MONTHS);
        rangeFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RangePreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label);
            }
        });
        rangeFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RangePreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label);
            }
        });
        rangeFilter.setOnAction(event -> refreshSnapshot());

        bucketFilter.getStyleClass().addAll("combo-box", "form-combo");
        bucketFilter.getItems().setAll((PlannerBucket) null);
        bucketFilter.getItems().addAll(PlannerBucket.values());
        bucketFilter.getSelectionModel().selectFirst();
        bucketFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PlannerBucket item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Buckets" : item.getDisplayName());
            }
        });
        bucketFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PlannerBucket item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Buckets" : item.getDisplayName());
            }
        });
        bucketFilter.setOnAction(event -> refreshSnapshot());

        categoryFilter.getStyleClass().addAll("combo-box", "form-combo");
        categoryFilter.getItems().setAll((ExpenseCategory) null);
        categoryFilter.getItems().addAll(ExpenseCategory.values());
        categoryFilter.getSelectionModel().selectFirst();
        categoryFilter.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Categories" : item.getLabel());
            }
        });
        categoryFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All Categories" : item.getLabel());
            }
        });
        categoryFilter.setOnAction(event -> refreshSnapshot());

        searchField.getStyleClass().addAll("text-input", "form-input");
        searchField.setPromptText("Search tag/subcategory...");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            searchDebounce.stop();
            searchDebounce.setOnFinished(event -> refreshSnapshot());
            searchDebounce.playFromStart();
        });
    }

    private void configurePanels() {
        HBox controlsRow = new HBox(10, rangeFilter, bucketFilter, categoryFilter, searchField);
        controlsRow.getStyleClass().add("analytics-controls-row");
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox summaryRow = new HBox(
                UiUtils.CARD_GAP,
                totalSpendCard,
                avgMonthlySpendCard,
                bestMonthSpendCard,
                worstMonthSpendCard
        );
        summaryRow.getStyleClass().add("analytics-summary-row");
        for (Node node : summaryRow.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
        }

        noPlanLabel.getStyleClass().add("analytics-inline-warning");
        noPlanLabel.setVisible(false);
        noPlanLabel.setManaged(false);

        unplannedInfoLabel.getStyleClass().add("info-row-value");
        forecastAccuracyEmptyLabel.getStyleClass().add("analytics-empty-label");
        habitTrendCaption.getStyleClass().add("analytics-empty-label");

        SectionCard spendTrendCard = new SectionCard(
                "Monthly Spend Trend",
                "Total and unplanned spend progression across the selected range.",
                new VBox(10, spendTrendChart, spendTrendCaption)
        );
        spendTrendCard.getStyleClass().add("analytics-panel");

        SectionCard planVsActualCard = new SectionCard(
                "Plan vs Actual by Bucket",
                "Planner buckets for the selected anchor month.",
                new VBox(10, planVsActualChart, noPlanLabel, unplannedInfoLabel)
        );
        planVsActualCard.getStyleClass().add("analytics-panel");

        SectionCard categoryShareCard = new SectionCard(
                "Category Share",
                "Anchor-month category split for current filters.",
                categoryShareList
        );
        categoryShareCard.getStyleClass().add("analytics-panel");

        SectionCard forecastAccuracyCard = new SectionCard(
                "Forecast Accuracy",
                "Projected (week 1 pace) vs actual for completed months.",
                new VBox(10, forecastAccuracyChart, forecastAccuracyEmptyLabel)
        );
        forecastAccuracyCard.getStyleClass().add("analytics-panel");

        SectionCard topDriversCard = new SectionCard(
                "Top Subcategories / Tags",
                "Top spend drivers in anchor month.",
                topDriversList
        );
        topDriversCard.getStyleClass().add("analytics-panel");

        SectionCard habitsCard = new SectionCard(
                "Habits Compliance Trend",
                "Warning and exceeded trend for active habit rules.",
                new VBox(10, habitTrendChart, habitTrendCaption)
        );
        habitsCard.getStyleClass().add("analytics-panel");

        HBox rowOne = UiUtils.createTwoColumn(spendTrendCard, planVsActualCard);
        HBox rowTwo = UiUtils.createTwoColumn(categoryShareCard, forecastAccuracyCard);
        HBox rowThree = UiUtils.createTwoColumn(topDriversCard, habitsCard);
        rowOne.getStyleClass().add("analytics-grid-row");
        rowTwo.getStyleClass().add("analytics-grid-row");
        rowThree.getStyleClass().add("analytics-grid-row");

        getChildren().addAll(controlsRow, summaryRow, rowOne, rowTwo, rowThree);
    }

    private void refreshSnapshot() {
        RangePreset rangePreset = rangeFilter.getValue() == null ? RangePreset.LAST_3_MONTHS : rangeFilter.getValue();
        AnalyticsQuery query = new AnalyticsQuery(
                appContext.getSelectedMonth(),
                rangePreset.monthsBack,
                bucketFilter.getValue(),
                categoryFilter.getValue(),
                searchField.getText()
        );

        AnalyticsSnapshot snapshot = analyticsService.buildSnapshot(query);
        updateSummaryCards(snapshot);
        updateSpendTrend(snapshot);
        updatePlanVsActual(snapshot);
        updateCategoryShare(snapshot);
        updateForecastAccuracy(snapshot);
        updateTopDrivers(snapshot);
        updateHabitTrend(snapshot);
    }

    private void updateSummaryCards(AnalyticsSnapshot snapshot) {
        String currencyCode = resolveCurrencyCode();
        totalSpendCard.setValues(
                "Total Spend (Range)",
                MoneyUtils.format(snapshot.getTotalSpendInRange(), currencyCode),
                snapshot.getMonthsBack() + " month(s) ending " + appContext.getCurrentMonthDisplayText()
        );
        avgMonthlySpendCard.setValues(
                "Avg Monthly Spend",
                MoneyUtils.format(snapshot.getAvgMonthlySpend(), currencyCode),
                "Mean spend across selected range"
        );
        bestMonthSpendCard.setValues(
                "Best Month Spend",
                MoneyUtils.format(snapshot.getBestMonthSpend(), currencyCode),
                "Lowest monthly spend in range"
        );
        worstMonthSpendCard.setValues(
                "Worst Month Spend",
                MoneyUtils.format(snapshot.getWorstMonthSpend(), currencyCode),
                "Highest monthly spend in range"
        );
    }

    private void updateSpendTrend(AnalyticsSnapshot snapshot) {
        spendTrendChart.getData().clear();

        XYChart.Series<String, Number> totalSeries = new XYChart.Series<>();
        totalSeries.setName("Total Spend");
        for (MonthPoint point : snapshot.getMonthlySpendTrend()) {
            totalSeries.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getValue()));
        }

        XYChart.Series<String, Number> unplannedSeries = new XYChart.Series<>();
        unplannedSeries.setName("Unplanned Spend");
        for (MonthPoint point : snapshot.getMonthlyUnplannedTrend()) {
            unplannedSeries.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getValue()));
        }

        spendTrendChart.getData().addAll(totalSeries, unplannedSeries);
        spendTrendCaption.setText("Total spend across last " + snapshot.getMonthsBack() + " months.");
    }

    private void updatePlanVsActual(AnalyticsSnapshot snapshot) {
        planVsActualChart.getData().clear();

        PlanVsActualBreakdown breakdown = snapshot.getSelectedMonthBuckets();
        XYChart.Series<String, Number> plannedSeries = new XYChart.Series<>();
        plannedSeries.setName("Planned");
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual");

        for (BucketPlanActualPoint row : breakdown.getBucketRows()) {
            plannedSeries.getData().add(new XYChart.Data<>(row.getBucketLabel(), row.getPlanned()));
            actualSeries.getData().add(new XYChart.Data<>(row.getBucketLabel(), row.getActual()));
        }

        if (!plannedSeries.getData().isEmpty() || !actualSeries.getData().isEmpty()) {
            planVsActualChart.getData().addAll(plannedSeries, actualSeries);
        }

        noPlanLabel.setVisible(!breakdown.isHasMonthlyPlan());
        noPlanLabel.setManaged(!breakdown.isHasMonthlyPlan());
        unplannedInfoLabel.setText("Unplanned spend: " + MoneyUtils.format(breakdown.getUnplannedSpend(), resolveCurrencyCode()));
    }

    private void updateCategoryShare(AnalyticsSnapshot snapshot) {
        categoryShareList.getChildren().clear();
        List<CategorySharePoint> categoryShare = snapshot.getCategoryShare();
        if (categoryShare.isEmpty()) {
            categoryShareList.getChildren().add(new DataEmptyState(
                    "No category data",
                    "No expenses match current filters in the selected month."
            ));
            return;
        }

        String currencyCode = resolveCurrencyCode();
        for (CategorySharePoint point : categoryShare) {
            VBox row = new VBox(6);
            row.getStyleClass().add("analytics-category-row");

            Label categoryLabel = new Label(point.getCategoryLabel() + " (" + point.getEntryCount() + ")");
            categoryLabel.getStyleClass().add("muted-text");

            Label valueLabel = new Label(
                    MoneyUtils.format(point.getTotal(), currencyCode)
                            + " | "
                            + point.getPercentOfTotal().stripTrailingZeros().toPlainString()
                            + "%"
            );
            valueLabel.getStyleClass().add("info-row-value");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox head = new HBox(8, categoryLabel, spacer, valueLabel);
            head.setAlignment(Pos.CENTER_LEFT);

            ProgressBar progressBar = new ProgressBar(
                    Math.max(0, Math.min(1, point.getPercentOfTotal().doubleValue() / 100.0))
            );
            progressBar.getStyleClass().add("analytics-share-progress");
            progressBar.setMaxWidth(Double.MAX_VALUE);

            row.getChildren().addAll(head, progressBar);
            categoryShareList.getChildren().add(row);
        }
    }

    private void updateForecastAccuracy(AnalyticsSnapshot snapshot) {
        forecastAccuracyChart.getData().clear();
        List<ForecastAccuracyPoint> points = snapshot.getForecastAccuracy().getPoints();
        if (points.isEmpty()) {
            forecastAccuracyEmptyLabel.setVisible(true);
            forecastAccuracyEmptyLabel.setManaged(true);
            return;
        }

        forecastAccuracyEmptyLabel.setVisible(false);
        forecastAccuracyEmptyLabel.setManaged(false);

        XYChart.Series<String, Number> projectedSeries = new XYChart.Series<>();
        projectedSeries.setName("Projected (week 1 pace)");
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual");

        for (ForecastAccuracyPoint point : points) {
            projectedSeries.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getProjectedFromWeekOne()));
            actualSeries.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getActual()));
        }
        forecastAccuracyChart.getData().addAll(projectedSeries, actualSeries);
    }

    private void updateTopDrivers(AnalyticsSnapshot snapshot) {
        topDriversList.getChildren().clear();
        List<TopSubcategoryRow> rows = snapshot.getTopSubcategories();
        if (rows.isEmpty()) {
            topDriversList.getChildren().add(new DataEmptyState(
                    "No top drivers",
                    "No tag/subcategory/category spend matches current filters."
            ));
            return;
        }

        String currencyCode = resolveCurrencyCode();
        for (TopSubcategoryRow rowData : rows) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("analytics-top-row");

            Label label = new Label(rowData.getLabel());
            label.getStyleClass().addAll("card-title", "analytics-top-label");

            Label value = new Label(
                    MoneyUtils.format(rowData.getAmount(), currencyCode)
                            + " | "
                            + rowData.getEntryCount()
                            + " entr"
                            + (rowData.getEntryCount() == 1 ? "y" : "ies")
            );
            value.getStyleClass().add("info-row-value");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(label, spacer, value);
            topDriversList.getChildren().add(row);
        }
    }

    private void updateHabitTrend(AnalyticsSnapshot snapshot) {
        habitTrendChart.getData().clear();
        List<HabitTrendPoint> points = snapshot.getHabitTrend().getPoints();
        if (points.isEmpty()) {
            habitTrendCaption.setText("No habit trend data in selected range.");
            return;
        }

        XYChart.Series<String, Number> warningsSeries = new XYChart.Series<>();
        warningsSeries.setName("Warnings");
        XYChart.Series<String, Number> exceededSeries = new XYChart.Series<>();
        exceededSeries.setName("Exceeded");

        BigDecimal totalCompliance = BigDecimal.ZERO.setScale(2);
        for (HabitTrendPoint point : points) {
            warningsSeries.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getWarningCount()));
            exceededSeries.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getExceededCount()));
            totalCompliance = MoneyUtils.normalize(totalCompliance.add(point.getCompliancePercent()));
        }
        habitTrendChart.getData().addAll(warningsSeries, exceededSeries);

        BigDecimal averageCompliance = points.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : totalCompliance.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
        habitTrendCaption.setText(
                "Average on-track compliance: " + averageCompliance.stripTrailingZeros().toPlainString() + "%"
        );
    }

    private LineChart<String, Number> createLineChart(String styleClass) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(true);
        chart.setMinHeight(250);
        chart.getStyleClass().addAll("analytics-chart", styleClass);
        chart.setMaxWidth(Double.MAX_VALUE);
        return chart;
    }

    private BarChart<String, Number> createBarChart(String styleClass) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCategoryGap(12);
        chart.setBarGap(4);
        chart.setMinHeight(250);
        chart.getStyleClass().addAll("analytics-chart", styleClass);
        chart.setMaxWidth(Double.MAX_VALUE);
        return chart;
    }

    private String resolveCurrencyCode() {
        UserProfile profile = appContext.getCurrentUser();
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
    }

    private enum RangePreset {
        LAST_1_MONTH(1, "Last 1 month"),
        LAST_3_MONTHS(3, "Last 3 months"),
        LAST_6_MONTHS(6, "Last 6 months"),
        LAST_12_MONTHS(12, "Last 12 months");

        private final int monthsBack;
        private final String label;

        RangePreset(int monthsBack, String label) {
            this.monthsBack = monthsBack;
            this.label = label;
        }
    }
}
