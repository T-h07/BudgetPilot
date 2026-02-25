package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.service.BudgetSummary;
import com.budgetpilot.service.PlannerService;
import com.budgetpilot.service.WeekAllocation;
import com.budgetpilot.service.WeeklyBudgetBreakdown;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.MoneyField;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.SummaryStatCard;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.UiUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.YearMonth;

public class PlannerPage extends VBox {
    private final AppContext appContext;
    private final PlannerService plannerService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final MoneyField fixedCostsField = new MoneyField("Fixed Costs Budget", "0");
    private final MoneyField foodBudgetField = new MoneyField("Food Budget", "0");
    private final MoneyField transportBudgetField = new MoneyField("Transport Budget", "0");
    private final MoneyField familyBudgetField = new MoneyField("Family Budget", "0");
    private final MoneyField discretionaryBudgetField = new MoneyField("Discretionary Budget", "0");
    private final MoneyField safetyBufferField = new MoneyField("Safety Buffer Amount", "0");
    private final TextField savingsPercentField = textField("Savings percent");
    private final TextField goalsPercentField = textField("Goals percent");
    private final TextArea notesArea = new TextArea();

    private final Label familyBudgetLabel = new Label("Family Budget");
    private final HBox statusRow = new HBox();
    private final Label statusLabel = new Label();

    private final SummaryStatCard plannedIncomeCard = new SummaryStatCard();
    private final SummaryStatCard plannedSavingsCard = new SummaryStatCard();
    private final SummaryStatCard plannedGoalsCard = new SummaryStatCard();
    private final SummaryStatCard totalUsageCard = new SummaryStatCard();
    private final SummaryStatCard remainingCard = new SummaryStatCard();

    private final ObservableList<WeekAllocation> weekRows = FXCollections.observableArrayList();
    private final TableView<WeekAllocation> weeklyTable = new TableView<>(weekRows);
    private final TableColumn<WeekAllocation, String> familyColumn = new TableColumn<>("Family");

    private MonthlyPlan loadedPlan;

    public PlannerPage(AppContext appContext) {
        this.appContext = appContext;
        this.plannerService = new PlannerService(appContext.getStore());

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-planner");

        getChildren().add(UiUtils.createPageHeader(
                "Planner",
                "Create your monthly budget plan and review weekly allocations for " + appContext.getCurrentMonthDisplayText() + "."
        ));

        setupFormDefaults();
        setupBanner();
        setupWeeklyTable();

        HBox summaryRow = buildSummaryRow();
        HBox mainRow = buildMainRow();
        SectionCard weeklySection = new SectionCard(
                "Weekly Breakdown",
                "Equal split planning across calendar weeks for the selected month.",
                weeklyTable
        );
        weeklySection.getStyleClass().add("planner-weekly-card");

        getChildren().addAll(summaryRow, bannerLabel, mainRow, weeklySection);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshAll();
    }

    private void setupFormDefaults() {
        savingsPercentField.setText("20");
        goalsPercentField.setText("10");
        notesArea.setPromptText("Optional planner notes");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().addAll("text-input", "form-input");
        familyBudgetLabel.getStyleClass().add("form-label");

        statusRow.getChildren().add(statusLabel);
        statusLabel.getStyleClass().add("muted-text");
    }

    private void setupBanner() {
        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.getStyleClass().add("error-banner");
    }

    private void setupWeeklyTable() {
        weeklyTable.getStyleClass().add("planner-weekly-table");
        weeklyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        weeklyTable.setPlaceholder(new DataEmptyState(
                "No weekly breakdown available",
                "Save a monthly plan to generate weekly budget guidance."
        ));

        TableColumn<WeekAllocation, String> weekLabelColumn = new TableColumn<>("Week");
        weekLabelColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getWeekLabel()));

        TableColumn<WeekAllocation, String> rangeColumn = new TableColumn<>("Date Range");
        rangeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateRange()));

        TableColumn<WeekAllocation, String> totalColumn = moneyColumn("Planned Total", WeekAllocation::getPlannedTotal);
        TableColumn<WeekAllocation, String> foodColumn = moneyColumn("Food", WeekAllocation::getPlannedFood);
        TableColumn<WeekAllocation, String> transportColumn = moneyColumn("Transport", WeekAllocation::getPlannedTransport);
        TableColumn<WeekAllocation, String> discretionaryColumn = moneyColumn("Discretionary", WeekAllocation::getPlannedDiscretionary);

        familyColumn.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(data.getValue().getPlannedFamily(), resolveCurrencyCode())
        ));

        weeklyTable.getColumns().setAll(
                weekLabelColumn,
                rangeColumn,
                totalColumn,
                foodColumn,
                transportColumn,
                discretionaryColumn,
                familyColumn
        );
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                plannedIncomeCard,
                plannedSavingsCard,
                plannedGoalsCard,
                totalUsageCard,
                remainingCard
        );
        row.getStyleClass().add("planner-summary-grid");
        for (Node node : row.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
        }
        return row;
    }

    private HBox buildMainRow() {
        SectionCard formCard = new SectionCard(
                "Monthly Plan",
                "Define your budget allocations for the selected month.",
                buildFormBody()
        );
        formCard.getStyleClass().add("planner-form-card");

        SectionCard computedCard = new SectionCard(
                "Computed Summary",
                "Planner engine outputs based on current plan and income entries.",
                statusRow
        );
        computedCard.getStyleClass().add("planner-computed-card");

        HBox row = new HBox(UiUtils.CARD_GAP, formCard, computedCard);
        HBox.setHgrow(formCard, Priority.ALWAYS);
        HBox.setHgrow(computedCard, Priority.ALWAYS);
        formCard.setMaxWidth(Double.MAX_VALUE);
        computedCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildFormBody() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Fixed Costs Budget", fixedCostsField);
        addFormRow(grid, 1, "Food Budget", foodBudgetField);
        addFormRow(grid, 2, "Transport Budget", transportBudgetField);
        addFormRow(grid, 3, "Family Budget", familyBudgetField, familyBudgetLabel);
        addFormRow(grid, 4, "Discretionary Budget", discretionaryBudgetField);
        addFormRow(grid, 5, "Savings Percent", savingsPercentField);
        addFormRow(grid, 6, "Goals Percent", goalsPercentField);
        addFormRow(grid, 7, "Safety Buffer Amount", safetyBufferField);

        grid.add(new Label("Notes"), 0, 8);
        grid.add(notesArea, 1, 8);

        Button saveButton = new Button("Save Plan");
        saveButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveButton.setOnAction(event -> onSavePlan());

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> populateFormFromPlan(loadedPlan));

        HBox actions = new HBox(10, saveButton, resetButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        return new VBox(10, grid, actions);
    }

    private void addFormRow(GridPane grid, int rowIndex, String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        addFormRow(grid, rowIndex, labelText, field, label);
    }

    private void addFormRow(GridPane grid, int rowIndex, String labelText, Node field, Label label) {
        label.setText(labelText);
        grid.add(label, 0, rowIndex);
        grid.add(field, 1, rowIndex);

        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    private void onSavePlan() {
        clearBanner();
        try {
            MonthlyPlan plan = loadedPlan == null
                    ? new MonthlyPlan(appContext.getSelectedMonth())
                    : loadedPlan.copy();

            plan.setMonth(appContext.getSelectedMonth());
            plan.setFixedCostsBudget(fixedCostsField.parseNonNegativeOrZero());
            plan.setFoodBudget(foodBudgetField.parseNonNegativeOrZero());
            plan.setTransportBudget(transportBudgetField.parseNonNegativeOrZero());
            plan.setFamilyBudget(familyBudgetField.parseNonNegativeOrZero());
            plan.setDiscretionaryBudget(discretionaryBudgetField.parseNonNegativeOrZero());
            plan.setSavingsPercent(parsePercent(savingsPercentField.getText(), "Savings percent"));
            plan.setGoalsPercent(parsePercent(goalsPercentField.getText(), "Goals percent"));
            plan.setSafetyBufferAmount(safetyBufferField.parseNonNegativeOrZero());
            plan.setNotes(notesArea.getText());

            plannerService.saveMonthlyPlan(plan, isFamilyModuleEnabled());
            appContext.notifyContextChanged();
            showSuccess("Monthly plan saved.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        loadedPlan = plannerService.getOrCreateMonthlyPlan(month);
        populateFormFromPlan(loadedPlan);

        boolean familyEnabled = isFamilyModuleEnabled();
        familyBudgetField.setManaged(familyEnabled);
        familyBudgetField.setVisible(familyEnabled);
        familyBudgetLabel.setManaged(familyEnabled);
        familyBudgetLabel.setVisible(familyEnabled);
        familyColumn.setVisible(familyEnabled);

        BudgetSummary summary = plannerService.buildBudgetSummary(month, familyEnabled);
        updateSummaryCards(summary);
        updateStatus(summary);

        WeeklyBudgetBreakdown weeklyBreakdown = plannerService.buildWeeklyBreakdown(month, familyEnabled);
        weekRows.setAll(weeklyBreakdown.getAllocations());
    }

    private void populateFormFromPlan(MonthlyPlan plan) {
        if (plan == null) {
            return;
        }
        fixedCostsField.setMoneyValue(plan.getFixedCostsBudget());
        foodBudgetField.setMoneyValue(plan.getFoodBudget());
        transportBudgetField.setMoneyValue(plan.getTransportBudget());
        familyBudgetField.setMoneyValue(plan.getFamilyBudget());
        discretionaryBudgetField.setMoneyValue(plan.getDiscretionaryBudget());
        savingsPercentField.setText(MoneyUtils.normalize(plan.getSavingsPercent()).toPlainString());
        goalsPercentField.setText(MoneyUtils.normalize(plan.getGoalsPercent()).toPlainString());
        safetyBufferField.setMoneyValue(plan.getSafetyBufferAmount());
        notesArea.setText(plan.getNotes());
    }

    private void updateSummaryCards(BudgetSummary summary) {
        String currencyCode = resolveCurrencyCode();
        plannedIncomeCard.setValues(
                "Planned Income",
                MoneyUtils.format(summary.getPlannedIncome(), currencyCode),
                "Received: " + MoneyUtils.format(summary.getReceivedIncome(), currencyCode)
        );
        plannedSavingsCard.setValues(
                "Planned Savings",
                MoneyUtils.format(summary.getSavingsAmountPlanned(), currencyCode),
                "Savings %: " + summary.getSavingsPercent().toPlainString()
        );
        plannedGoalsCard.setValues(
                "Planned Goals",
                MoneyUtils.format(summary.getGoalsAmountPlanned(), currencyCode),
                "Goals %: " + summary.getGoalsPercent().toPlainString()
        );
        totalUsageCard.setValues(
                "Total Planned Usage",
                MoneyUtils.format(summary.getTotalPlannedUsage(), currencyCode),
                "Includes safety buffer"
        );
        remainingCard.setValues(
                "Remaining Planned",
                MoneyUtils.format(summary.getRemainingPlanned(), currencyCode),
                summary.isOverallocated() ? "Overallocated" : "Available to allocate"
        );
    }

    private void updateStatus(BudgetSummary summary) {
        statusLabel.setText(summary.getStatusMessage());
        statusLabel.getStyleClass().removeAll("planner-status-healthy", "planner-status-warning");
        if (summary.isOverallocated() || summary.getPlannedIncome().compareTo(BigDecimal.ZERO) <= 0) {
            statusLabel.getStyleClass().add("planner-status-warning");
        } else {
            statusLabel.getStyleClass().add("planner-status-healthy");
        }
    }

    private String resolveCurrencyCode() {
        UserProfile profile = appContext.getCurrentUser();
        return (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank())
                ? "EUR"
                : profile.getCurrencyCode();
    }

    private boolean isFamilyModuleEnabled() {
        UserProfile profile = appContext.getCurrentUser();
        return profile != null && profile.isFamilyModuleEnabled();
    }

    private BigDecimal parsePercent(String rawValue, String fieldName) {
        BigDecimal percent = MoneyUtils.parseNonNegativeOrZero(rawValue, fieldName);
        if (percent.compareTo(MoneyUtils.HUNDRED) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100.");
        }
        return percent;
    }

    private void showSuccess(String message) {
        bannerLabel.getStyleClass().remove("error-banner");
        if (!bannerLabel.getStyleClass().contains("success-banner")) {
            bannerLabel.getStyleClass().add("success-banner");
        }
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
        bannerLabel.setText(message);
    }

    private void showError(String message) {
        bannerLabel.getStyleClass().remove("success-banner");
        if (!bannerLabel.getStyleClass().contains("error-banner")) {
            bannerLabel.getStyleClass().add("error-banner");
        }
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
        bannerLabel.setText(message);
    }

    private void clearBanner() {
        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.setText("");
    }

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("text-input", "form-input");
        return field;
    }

    private TableColumn<WeekAllocation, String> moneyColumn(
            String title,
            java.util.function.Function<WeekAllocation, BigDecimal> extractor
    ) {
        TableColumn<WeekAllocation, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(extractor.apply(data.getValue()), resolveCurrencyCode())
        ));
        return column;
    }
}
