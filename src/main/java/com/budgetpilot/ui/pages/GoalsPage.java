package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.GoalContributionType;
import com.budgetpilot.model.enums.GoalType;
import com.budgetpilot.service.BudgetSummary;
import com.budgetpilot.service.GoalProgressSummary;
import com.budgetpilot.service.GoalService;
import com.budgetpilot.service.GoalSummary;
import com.budgetpilot.service.PlannerService;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.MoneyField;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.SummaryStatCard;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GoalsPage extends VBox {
    private final AppContext appContext;
    private final GoalService goalService;
    private final PlannerService plannerService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final SummaryStatCard activeGoalsCard = new SummaryStatCard();
    private final SummaryStatCard totalProgressCard = new SummaryStatCard();
    private final SummaryStatCard totalTargetsCard = new SummaryStatCard();
    private final SummaryStatCard monthlyContributionCard = new SummaryStatCard();
    private final SummaryStatCard plannedVsActualCard = new SummaryStatCard();

    private final TextField goalNameField = textField("Goal name");
    private final ComboBox<GoalType> goalTypeCombo = new ComboBox<>();
    private final MoneyField goalTargetField = new MoneyField("Target Amount", "Target amount");
    private final MoneyField goalCurrentField = new MoneyField("Current Amount", "Initial current amount (optional)");
    private final DatePicker goalTargetDatePicker = new DatePicker();
    private final ComboBox<Integer> priorityCombo = new ComboBox<>();
    private final CheckBox goalActiveCheck = new CheckBox("Active");
    private final TextArea goalNotesArea = new TextArea();
    private final Button saveGoalButton = new Button("Create Goal");
    private final Button clearGoalButton = new Button("Clear Form");

    private final VBox goalsListBox = new VBox(10);

    private final Label selectedGoalTitleLabel = new Label("No goal selected");
    private final Label selectedGoalAmountLabel = new Label("-");
    private final Label selectedGoalMetaLabel = new Label("Select a goal to manage progress.");
    private final ProgressBar selectedGoalProgress = new ProgressBar(0);

    private final MoneyField contributionAmountField = new MoneyField("Amount", "Amount");
    private final DatePicker contributionDatePicker = new DatePicker();
    private final ComboBox<GoalContributionType> contributionTypeCombo = new ComboBox<>();
    private final TextField contributionNoteField = textField("Note (optional)");
    private final Button addContributionButton = new Button("Add Contribution");
    private final Button clearContributionButton = new Button("Clear");

    private final ObservableList<GoalContribution> contributionRows = FXCollections.observableArrayList();
    private final TableView<GoalContribution> contributionTable = new TableView<>(contributionRows);

    private List<Goal> goals = List.of();
    private Map<String, GoalProgressSummary> progressByGoalId = Map.of();
    private String selectedGoalId;
    private String editingGoalId;

    public GoalsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.goalService = new GoalService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));
        this.plannerService = new PlannerService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-goals");

        getChildren().add(UiUtils.createPageHeader(
                "Goals",
                "Create financial goals, contribute consistently, and track progress with clear completion visibility."
        ));

        setupBanner();
        setupGoalForm();
        setupContributionForm();
        setupContributionTable();
        setupActions();

        HBox summaryRow = buildSummaryRow();
        HBox mainRow = buildMainRow();

        getChildren().addAll(summaryRow, bannerLabel, mainRow);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshAll();
    }

    private void setupBanner() {
        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.getStyleClass().add("error-banner");
    }

    private void setupGoalForm() {
        goalTypeCombo.getItems().setAll(GoalType.values());
        goalTypeCombo.getSelectionModel().select(GoalType.CUSTOM);
        goalTypeCombo.getStyleClass().addAll("combo-box", "form-combo");

        priorityCombo.getItems().setAll(1, 2, 3, 4, 5);
        priorityCombo.getSelectionModel().select(Integer.valueOf(3));
        priorityCombo.getStyleClass().addAll("combo-box", "form-combo");

        goalTargetDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");
        goalTargetDatePicker.setPromptText("Optional");

        goalActiveCheck.setSelected(true);

        goalNotesArea.setPromptText("Optional notes");
        goalNotesArea.setPrefRowCount(3);
        goalNotesArea.getStyleClass().addAll("text-input", "form-input");

        goalsListBox.getStyleClass().add("goals-list");
    }

    private void setupContributionForm() {
        selectedGoalTitleLabel.getStyleClass().add("card-title");
        selectedGoalAmountLabel.getStyleClass().add("kpi-value");
        selectedGoalMetaLabel.getStyleClass().add("muted-text");
        selectedGoalProgress.getStyleClass().add("goal-progress-bar");

        contributionDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");
        contributionDatePicker.setValue(defaultDateForSelectedMonth());

        contributionTypeCombo.getItems().setAll(GoalContributionType.values());
        contributionTypeCombo.getSelectionModel().select(GoalContributionType.CONTRIBUTION);
        contributionTypeCombo.getStyleClass().addAll("combo-box", "form-combo");
    }

    private void setupContributionTable() {
        contributionTable.getStyleClass().add("goal-contribution-table");
        contributionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        contributionTable.setPlaceholder(new DataEmptyState(
                "No contributions yet",
                "Add your first contribution to move this goal forward."
        ));

        TableColumn<GoalContribution, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getContributionDate() == null ? "" : data.getValue().getContributionDate().toString()
        ));

        TableColumn<GoalContribution, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().getLabel()));

        TableColumn<GoalContribution, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(formatSignedMoney(data.getValue().getAmount())));
        amountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (empty) {
                    getStyleClass().removeAll("status-good", "status-danger");
                    return;
                }
                GoalContribution entry = getTableView().getItems().get(getIndex());
                getStyleClass().removeAll("status-good", "status-danger");
                if (entry.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    getStyleClass().add("status-good");
                } else {
                    getStyleClass().add("status-danger");
                }
            }
        });

        TableColumn<GoalContribution, String> noteColumn = new TableColumn<>("Note");
        noteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

        TableColumn<GoalContribution, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                deleteButton.getStyleClass().addAll("danger-button", "btn-danger");
                deleteButton.setOnAction(event -> onDeleteContribution(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });

        contributionTable.getColumns().setAll(dateColumn, typeColumn, amountColumn, noteColumn, actionsColumn);
    }

    private void setupActions() {
        saveGoalButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveGoalButton.setOnAction(event -> onSaveGoal());

        clearGoalButton.setOnAction(event -> clearGoalForm());

        addContributionButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        addContributionButton.setOnAction(event -> onAddContribution());

        clearContributionButton.setOnAction(event -> clearContributionForm());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                activeGoalsCard,
                totalProgressCard,
                totalTargetsCard,
                monthlyContributionCard,
                plannedVsActualCard
        );
        row.getStyleClass().add("goals-summary-grid");

        for (Node node : row.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
        }
        return row;
    }

    private HBox buildMainRow() {
        SectionCard leftCard = new SectionCard(
                "Goals Management",
                "Define and organize goals by type, priority, and target amount.",
                buildGoalManagerBody()
        );
        leftCard.getStyleClass().add("goals-list-card");

        SectionCard rightCard = new SectionCard(
                "Goal Details",
                "Contribute, withdraw, and monitor progress with contribution history.",
                buildGoalDetailsBody()
        );
        rightCard.getStyleClass().add("goals-details-card");

        HBox row = new HBox(UiUtils.CARD_GAP, leftCard, rightCard);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);
        rightCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }
    private Node buildGoalManagerBody() {
        VBox section = new VBox(12, buildGoalForm(), new Label("Goals"), goalsListBox);
        section.getChildren().get(1).getStyleClass().add("form-label");
        return section;
    }

    private Node buildGoalForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Goal Name", goalNameField);
        addFormRow(grid, 1, "Goal Type", goalTypeCombo);
        addFormRow(grid, 2, "Target Amount", goalTargetField);
        addFormRow(grid, 3, "Current Amount", goalCurrentField);
        addFormRow(grid, 4, "Target Date", goalTargetDatePicker);
        addFormRow(grid, 5, "Priority", priorityCombo);

        Label activeLabel = new Label("Status");
        activeLabel.getStyleClass().add("form-label");
        grid.add(activeLabel, 0, 6);
        grid.add(goalActiveCheck, 1, 6);

        addFormRow(grid, 7, "Notes", goalNotesArea);

        HBox actions = new HBox(10, saveGoalButton, clearGoalButton);
        actions.setPadding(new Insets(8, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, grid, actions);
    }

    private Node buildGoalDetailsBody() {
        return new VBox(14,
                buildSelectedGoalHeader(),
                buildContributionForm(),
                buildContributionTableSection()
        );
    }

    private Node buildSelectedGoalHeader() {
        return new VBox(8,
                selectedGoalTitleLabel,
                selectedGoalAmountLabel,
                selectedGoalMetaLabel,
                selectedGoalProgress
        );
    }

    private Node buildContributionForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Amount", contributionAmountField);
        addFormRow(grid, 1, "Date", contributionDatePicker);
        addFormRow(grid, 2, "Type", contributionTypeCombo);
        addFormRow(grid, 3, "Note", contributionNoteField);

        HBox actions = new HBox(10, addContributionButton, clearContributionButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(6, 0, 0, 0));

        VBox card = new VBox(10, new Label("Contribution"), grid, actions);
        card.getStyleClass().add("form-card");
        card.getChildren().get(0).getStyleClass().add("form-label");
        return card;
    }

    private Node buildContributionTableSection() {
        contributionTable.setPrefHeight(360);
        return contributionTable;
    }

    private void onSaveGoal() {
        clearBanner();
        try {
            Goal target = editingGoalId == null
                    ? new Goal()
                    : findGoalOrThrow(editingGoalId).copy();

            target.setName(ValidationUtils.requireNonBlank(goalNameField.getText(), "Goal name"));
            target.setGoalType(ValidationUtils.requireNonNull(goalTypeCombo.getValue(), "Goal type"));
            target.setTargetAmount(goalTargetField.parseRequiredPositive());
            target.setCurrentAmount(goalCurrentField.parseNonNegativeOrZero());
            target.setTargetDate(goalTargetDatePicker.getValue());
            target.setPriority(ValidationUtils.requireNonNull(priorityCombo.getValue(), "Priority"));
            target.setActive(goalActiveCheck.isSelected());
            target.setNotes(goalNotesArea.getText());

            goalService.saveGoal(target);
            selectedGoalId = target.getId();
            appContext.notifyContextChanged();
            showSuccess(editingGoalId == null ? "Goal created." : "Goal updated.");
            clearGoalForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onAddContribution() {
        clearBanner();
        try {
            if (selectedGoalId == null) {
                throw new IllegalArgumentException("Select a goal first.");
            }

            LocalDate contributionDate = ValidationUtils.requireNonNull(contributionDatePicker.getValue(), "Date");
            YearMonth contributionMonth = YearMonth.from(contributionDate);
            if (!contributionMonth.equals(appContext.getSelectedMonth())) {
                throw new IllegalArgumentException("Contribution date must be within " + appContext.getCurrentMonthDisplayText() + ".");
            }

            GoalContributionType type = ValidationUtils.requireNonNull(contributionTypeCombo.getValue(), "Type");
            BigDecimal amount = type == GoalContributionType.ADJUSTMENT
                    ? parseSignedAmount(contributionAmountField.getText(), "Adjustment amount")
                    : contributionAmountField.parseRequiredPositive();

            switch (type) {
                case CONTRIBUTION -> goalService.contribute(selectedGoalId, amount, contributionDate, contributionNoteField.getText());
                case WITHDRAWAL -> goalService.withdraw(selectedGoalId, amount, contributionDate, contributionNoteField.getText());
                case ADJUSTMENT -> goalService.adjust(selectedGoalId, amount, contributionDate, contributionNoteField.getText());
            }

            appContext.notifyContextChanged();
            showSuccess("Goal contribution recorded.");
            clearContributionForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteGoal(Goal goal) {
        if (!confirm(
                "Delete Goal",
                "Delete goal \"" + goal.getName() + "\" and all contribution history?"
        )) {
            return;
        }

        goalService.deleteGoal(goal.getId());
        if (goal.getId().equals(selectedGoalId)) {
            selectedGoalId = null;
        }
        if (goal.getId().equals(editingGoalId)) {
            clearGoalForm();
        }

        appContext.notifyContextChanged();
        showSuccess("Goal deleted.");
        refreshAll();
    }

    private void onDeleteContribution(GoalContribution contribution) {
        if (selectedGoalId == null) {
            return;
        }
        if (!confirm(
                "Delete Contribution",
                "Delete this contribution from " + contribution.getContributionDate() + "?"
        )) {
            return;
        }

        try {
            goalService.deleteContribution(selectedGoalId, contribution.getId());
            appContext.notifyContextChanged();
            showSuccess("Contribution deleted.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        goals = goalService.listGoals();
        progressByGoalId = goalService.listGoalProgressSummaries(month).stream()
                .collect(Collectors.toMap(GoalProgressSummary::getGoalId, summary -> summary));

        if (selectedGoalId == null && !goals.isEmpty()) {
            selectedGoalId = goals.get(0).getId();
        }

        if (selectedGoalId != null && goals.stream().noneMatch(goal -> goal.getId().equals(selectedGoalId))) {
            selectedGoalId = goals.isEmpty() ? null : goals.get(0).getId();
        }

        if (editingGoalId != null && goals.stream().noneMatch(goal -> goal.getId().equals(editingGoalId))) {
            clearGoalForm();
        }

        if (contributionDatePicker.getValue() == null
                || !YearMonth.from(contributionDatePicker.getValue()).equals(month)) {
            contributionDatePicker.setValue(defaultDateForSelectedMonth());
        }

        refreshSummaryCards(month);
        refreshGoalsList();
        refreshSelectedGoalDetails(month);
    }

    private void refreshSummaryCards(YearMonth month) {
        String currencyCode = resolveCurrencyCode();
        GoalSummary summary = goalService.getGoalsSummary(month);

        activeGoalsCard.setValues(
                "Active Goals",
                String.valueOf(summary.getActiveGoalCount()),
                summary.getCompletedGoalCount() + " completed"
        );
        totalProgressCard.setValues(
                "Total Goal Progress",
                MoneyUtils.format(summary.getTotalCurrentAmount(), currencyCode),
                summary.getOverallProgressPercent().stripTrailingZeros().toPlainString() + "% overall"
        );
        totalTargetsCard.setValues(
                "Total Targets",
                MoneyUtils.format(summary.getTotalTargetAmount(), currencyCode),
                "Remaining " + MoneyUtils.format(summary.getTotalRemainingAmount(), currencyCode)
        );
        monthlyContributionCard.setValues(
                "Contributed This Month",
                MoneyUtils.format(summary.getMonthlyContributions(), currencyCode),
                "Withdrawn " + MoneyUtils.format(summary.getMonthlyWithdrawals(), currencyCode)
        );

        MonthlyPlan existingPlan = appContext.getStore().getMonthlyPlan(month);
        if (existingPlan == null) {
            plannedVsActualCard.setValues(
                    "Planned vs Actual",
                    "No monthly plan",
                    "Set planner goals target for comparison"
            );
        } else {
            BudgetSummary budgetSummary = plannerService.buildBudgetSummary(month, isFamilyModuleEnabled());
            BigDecimal planned = budgetSummary.getGoalsAmountPlanned();
            BigDecimal delta = MoneyUtils.normalize(summary.getMonthlyContributions().subtract(planned));
            String deltaText = (delta.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + MoneyUtils.format(delta, currencyCode);
            plannedVsActualCard.setValues(
                    "Planned vs Actual",
                    MoneyUtils.format(summary.getMonthlyContributions(), currencyCode),
                    "Plan " + MoneyUtils.format(planned, currencyCode) + " | Delta " + deltaText
            );
        }
    }
    private void refreshGoalsList() {
        goalsListBox.getChildren().clear();
        if (goals.isEmpty()) {
            goalsListBox.getChildren().add(new DataEmptyState(
                    "No goals yet",
                    "Create your first goal (e.g., Car, Laptop, Vacation)."
            ));
            return;
        }

        for (Goal goal : goals) {
            goalsListBox.getChildren().add(createGoalCard(goal));
        }
    }

    private Node createGoalCard(Goal goal) {
        GoalProgressSummary progress = progressByGoalId.get(goal.getId());

        VBox card = new VBox(8);
        card.getStyleClass().add("goal-card");
        if (goal.getId().equals(selectedGoalId)) {
            card.getStyleClass().add("goal-card-selected");
        }

        Label title = new Label(goal.getName());
        title.getStyleClass().add("card-title");

        Label subtitle = UiUtils.createMutedLabel(goal.getGoalType().getLabel() + " | Priority " + goal.getPriority());
        subtitle.getStyleClass().add("goal-priority-badge");

        String line;
        if (progress == null) {
            line = MoneyUtils.format(goal.getCurrentAmount(), resolveCurrencyCode())
                    + " / " + MoneyUtils.format(goal.getTargetAmount(), resolveCurrencyCode());
        } else {
            line = MoneyUtils.format(progress.getCurrentAmount(), resolveCurrencyCode())
                    + " / " + MoneyUtils.format(progress.getTargetAmount(), resolveCurrencyCode())
                    + " | " + progress.getEstimatedCompletionText();
        }
        Label values = UiUtils.createMutedLabel(line);

        ProgressBar progressBar = new ProgressBar(progress == null ? 0 : progress.getProgressPercent().doubleValue() / 100.0);
        progressBar.getStyleClass().add("goal-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Button selectButton = new Button("Select");
        selectButton.setOnAction(event -> {
            selectedGoalId = goal.getId();
            refreshAll();
        });

        Button editButton = new Button("Edit");
        editButton.setOnAction(event -> loadGoalForEdit(goal));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("danger-button", "btn-danger");
        deleteButton.setOnAction(event -> onDeleteGoal(goal));

        HBox actions = new HBox(8, selectButton, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, subtitle, values, progressBar, actions);
        return card;
    }

    private void refreshSelectedGoalDetails(YearMonth month) {
        if (selectedGoalId == null) {
            selectedGoalTitleLabel.setText("No goal selected");
            selectedGoalAmountLabel.setText("-");
            selectedGoalMetaLabel.setText("Select a goal to manage progress.");
            selectedGoalProgress.setProgress(0);
            contributionRows.clear();
            setContributionControlsEnabled(false);
            return;
        }

        Goal goal = findGoalOrThrow(selectedGoalId);
        GoalProgressSummary progress = progressByGoalId.getOrDefault(
                selectedGoalId,
                goalService.getGoalProgress(selectedGoalId, month)
        );

        selectedGoalTitleLabel.setText(goal.getName() + " (" + goal.getGoalType().getLabel() + ")");
        selectedGoalAmountLabel.setText(
                MoneyUtils.format(progress.getCurrentAmount(), resolveCurrencyCode())
                        + " / "
                        + MoneyUtils.format(progress.getTargetAmount(), resolveCurrencyCode())
        );
        selectedGoalMetaLabel.setText(
                "Remaining " + MoneyUtils.format(progress.getRemainingAmount(), resolveCurrencyCode())
                        + " | " + progress.getEstimatedCompletionText()
                        + " | Month +" + MoneyUtils.format(progress.getMonthlyContributions(), resolveCurrencyCode())
                        + " / -" + MoneyUtils.format(progress.getMonthlyWithdrawals(), resolveCurrencyCode())
        );
        selectedGoalProgress.setProgress(progress.getProgressPercent().doubleValue() / 100.0);

        contributionRows.setAll(goalService.listContributions(selectedGoalId));
        setContributionControlsEnabled(true);
    }

    private void loadGoalForEdit(Goal goal) {
        editingGoalId = goal.getId();

        goalNameField.setText(goal.getName());
        goalTypeCombo.getSelectionModel().select(goal.getGoalType());
        goalTargetField.setMoneyValue(goal.getTargetAmount());
        goalCurrentField.setMoneyValue(goal.getCurrentAmount());
        goalTargetDatePicker.setValue(goal.getTargetDate());
        priorityCombo.getSelectionModel().select(Integer.valueOf(goal.getPriority()));
        goalActiveCheck.setSelected(goal.isActive());
        goalNotesArea.setText(goal.getNotes());

        saveGoalButton.setText("Save Changes");
        clearGoalButton.setText("Cancel Edit");
    }

    private void clearGoalForm() {
        editingGoalId = null;

        goalNameField.clear();
        goalTypeCombo.getSelectionModel().select(GoalType.CUSTOM);
        goalTargetField.clear();
        goalCurrentField.clear();
        goalTargetDatePicker.setValue(null);
        priorityCombo.getSelectionModel().select(Integer.valueOf(3));
        goalActiveCheck.setSelected(true);
        goalNotesArea.clear();

        saveGoalButton.setText("Create Goal");
        clearGoalButton.setText("Clear Form");
    }

    private void clearContributionForm() {
        contributionAmountField.clear();
        contributionDatePicker.setValue(defaultDateForSelectedMonth());
        contributionTypeCombo.getSelectionModel().select(GoalContributionType.CONTRIBUTION);
        contributionNoteField.clear();
    }

    private void setContributionControlsEnabled(boolean enabled) {
        contributionAmountField.setDisable(!enabled);
        contributionDatePicker.setDisable(!enabled);
        contributionTypeCombo.setDisable(!enabled);
        contributionNoteField.setDisable(!enabled);
        addContributionButton.setDisable(!enabled);
        clearContributionButton.setDisable(!enabled);
        contributionTable.setDisable(!enabled);
    }

    private Goal findGoalOrThrow(String goalId) {
        return goals.stream()
                .filter(goal -> goal.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Goal not found."));
    }

    private BigDecimal parseSignedAmount(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String normalizedInput = rawValue.trim().replace(',', '.');
        try {
            BigDecimal amount = MoneyUtils.normalize(new BigDecimal(normalizedInput));
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException(fieldName + " must not be zero.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid amount.");
        }
    }

    private String formatSignedMoney(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.zeroIfNull(amount);
        String prefix = normalized.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-";
        return prefix + MoneyUtils.format(normalized.abs(), resolveCurrencyCode());
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void addFormRow(GridPane grid, int rowIndex, String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        grid.add(label, 0, rowIndex);
        grid.add(field, 1, rowIndex);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    private boolean isFamilyModuleEnabled() {
        UserProfile profile = appContext.getCurrentUser();
        return profile != null && profile.isFamilyModuleEnabled();
    }

    private String resolveCurrencyCode() {
        UserProfile profile = appContext.getCurrentUser();
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
    }

    private LocalDate defaultDateForSelectedMonth() {
        YearMonth selectedMonth = appContext.getSelectedMonth();
        YearMonth currentMonth = YearMonth.now();
        if (selectedMonth.equals(currentMonth)) {
            return LocalDate.now();
        }
        return selectedMonth.atDay(1);
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
}
