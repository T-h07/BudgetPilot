package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.HabitSeverity;
import com.budgetpilot.model.enums.HabitAllowanceMode;
import com.budgetpilot.service.habits.HabitAllowanceRow;
import com.budgetpilot.service.habits.HabitAllowanceSnapshot;
import com.budgetpilot.service.habits.HabitInsight;
import com.budgetpilot.service.habits.HabitPageSummary;
import com.budgetpilot.service.habits.HabitService;
import com.budgetpilot.service.habits.HabitStatus;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.MoneyField;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.SummaryStatCard;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class HabitsPage extends VBox {
    private final AppContext appContext;
    private final HabitService habitService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();
    private final Label poolWarningLabel = new Label();
    private final Label allowanceMetaLabel = UiUtils.createMutedLabel("");
    private final Label baselineHintLabel = UiUtils.createMutedLabel(
            "Only spending above baseline counts against your habit allowance."
    );

    private final SummaryStatCard totalHabitSpentCard = new SummaryStatCard();
    private final SummaryStatCard totalHabitExcessCard = new SummaryStatCard();
    private final SummaryStatCard habitPoolCard = new SummaryStatCard();
    private final SummaryStatCard remainingCard = new SummaryStatCard();
    private final SummaryStatCard habitConfigCard = new SummaryStatCard();

    private final CheckBox discretionaryOnlyCheck = new CheckBox("Only count discretionary/unplanned");

    private final TextField displayNameField = textField("Display name");
    private final TextField tagField = textField("Tag (e.g. #snacks)");
    private final ComboBox<ExpenseCategory> linkedCategoryCombo = new ComboBox<>();
    private final MoneyField monthlyLimitField = new MoneyField("Hard Limit (optional)", "0");
    private final MoneyField baselineAmountField = new MoneyField("Baseline", "0");
    private final TextField warningThresholdField = textField("Warning threshold %");
    private final CheckBox activeCheck = new CheckBox("Active rule");
    private final TextArea notesArea = new TextArea();
    private final Button saveRuleButton = new Button("Add Rule");
    private final Button clearRuleButton = new Button("Clear Form");

    private final VBox ruleListBox = new VBox(10);
    private final VBox evaluationListBox = new VBox(10);
    private final VBox insightsBox = new VBox(10);

    private List<HabitRule> rules = List.of();
    private Map<String, HabitAllowanceRow> allowanceRowsByRuleId = new HashMap<>();
    private String editingRuleId;

    public HabitsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.habitService = new HabitService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-habits");

        getChildren().add(UiUtils.createPageHeader(
                "Habits",
                "Behavior allowance that automatically scales with your real monthly availability."
        ));

        setupBanners();
        setupRuleForm();
        setupActions();

        HBox allowanceRow = buildAllowanceRow();
        HBox mainRow = buildMainRow();

        getChildren().addAll(allowanceRow, allowanceMetaLabel, poolWarningLabel, bannerLabel, mainRow);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshAll();
    }

    private void setupBanners() {
        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.getStyleClass().add("error-banner");

        poolWarningLabel.setManaged(false);
        poolWarningLabel.setVisible(false);
        poolWarningLabel.getStyleClass().add("habit-pool-warning-banner");
    }

    private void setupRuleForm() {
        configureCategoryCombo(linkedCategoryCombo);
        linkedCategoryCombo.getItems().setAll((ExpenseCategory) null);
        linkedCategoryCombo.getItems().addAll(ExpenseCategory.values());
        linkedCategoryCombo.getSelectionModel().selectFirst();

        warningThresholdField.setText("70");
        activeCheck.setSelected(true);

        discretionaryOnlyCheck.setSelected(true);
        discretionaryOnlyCheck.getStyleClass().add("habit-discretionary-toggle");
        discretionaryOnlyCheck.setOnAction(event -> refreshAll());

        notesArea.setPromptText("Optional notes");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().addAll("text-area", "form-textarea");

        ruleListBox.getStyleClass().add("habit-rule-list");
        evaluationListBox.getStyleClass().add("habit-evaluation-list");
        insightsBox.getStyleClass().add("habit-insights-panel");
        allowanceMetaLabel.getStyleClass().add("habit-allowance-meta");
        baselineHintLabel.getStyleClass().add("habit-baseline-hint");
    }

    private void setupActions() {
        saveRuleButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveRuleButton.setOnAction(event -> onSaveRule());
        clearRuleButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearRuleButton.setOnAction(event -> clearRuleForm());
    }

    private HBox buildAllowanceRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                totalHabitSpentCard,
                totalHabitExcessCard,
                habitPoolCard,
                remainingCard,
                habitConfigCard
        );
        row.getStyleClass().add("habits-summary-grid");
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
                "Habit Rules",
                "Create rules, then tune month-level enablement and weights.",
                buildRulesBody()
        );

        SectionCard rightCard = new SectionCard(
                "Allowance Evaluations",
                "Live cap allocation and status based on current month cashflow.",
                buildEvaluationsBody()
        );

        leftCard.getStyleClass().add("habit-rules-card");
        rightCard.getStyleClass().add("habit-evaluations-card");

        HBox row = new HBox(UiUtils.CARD_GAP, leftCard, rightCard);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);
        rightCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildRulesBody() {
        VBox section = new VBox(12, buildRuleForm(), new Label("Rules"), ruleListBox);
        section.getChildren().get(1).getStyleClass().add("form-label");
        return section;
    }

    private Node buildRuleForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Display Name", displayNameField);
        addFormRow(grid, 1, "Tag", tagField);
        addFormRow(grid, 2, "Linked Category", linkedCategoryCombo);
        addFormRow(grid, 3, "Hard Limit", monthlyLimitField);
        addFormRow(grid, 4, "Baseline (tolerance) per month", baselineAmountField);
        addFormRow(grid, 5, "Warning Threshold %", warningThresholdField);
        grid.add(baselineHintLabel, 1, 6);

        Label activeLabel = new Label("Status");
        activeLabel.getStyleClass().add("form-label");
        grid.add(activeLabel, 0, 7);
        grid.add(activeCheck, 1, 7);

        addFormRow(grid, 8, "Notes", notesArea);

        HBox actions = new HBox(10, saveRuleButton, clearRuleButton);
        actions.setPadding(new Insets(8, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, grid, actions);
    }

    private Node buildEvaluationsBody() {
        VBox section = new VBox(14,
                discretionaryOnlyCheck,
                new Label("Habit Allowance Cards"),
                evaluationListBox,
                new Label("Insights"),
                insightsBox
        );
        section.getChildren().get(1).getStyleClass().add("form-label");
        section.getChildren().get(3).getStyleClass().add("form-label");
        return section;
    }

    private void onSaveRule() {
        clearBanner();
        try {
            HabitRule target = editingRuleId == null
                    ? new HabitRule()
                    : findRuleOrThrow(editingRuleId).copy();

            target.setDisplayName(ValidationUtils.requireNonBlank(displayNameField.getText(), "Habit name"));
            target.setTag(normalizeTag(tagField.getText()));
            target.setLinkedCategory(linkedCategoryCombo.getValue());
            target.setMonthlyLimit(monthlyLimitField.parseNonNegativeOrZero());
            target.setBaselineAmount(baselineAmountField.parseNonNegativeOrZero());
            target.setWarningThresholdPercent(parseThreshold(warningThresholdField.getText()));
            target.setActive(activeCheck.isSelected());
            target.setNotes(notesArea.getText());

            habitService.saveRule(target);
            appContext.notifyContextChanged();
            showSuccess(editingRuleId == null ? "Habit rule added." : "Habit rule updated.");
            clearRuleForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteRule(HabitRule rule) {
        if (!confirm("Delete Habit Rule", "Delete rule \"" + rule.getDisplayName() + "\"?")) {
            return;
        }
        habitService.deleteRule(rule.getId());
        if (rule.getId().equals(editingRuleId)) {
            clearRuleForm();
        }
        appContext.notifyContextChanged();
        showSuccess("Habit rule deleted.");
        refreshAll();
    }

    private void onToggleEnabled(HabitRule rule, boolean enabled) {
        try {
            HabitRule updated = rule.copy();
            updated.setEnabledThisMonth(enabled);
            habitService.saveRule(updated);
            appContext.notifyContextChanged();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onWeightChanged(HabitRule rule, int weight) {
        try {
            HabitRule updated = rule.copy();
            updated.setWeight(weight);
            habitService.saveRule(updated);
            appContext.notifyContextChanged();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        HabitPageSummary summary = habitService.getHabitPageSummary(month, discretionaryOnlyCheck.isSelected());
        HabitAllowanceSnapshot allowanceSnapshot = summary.getAllowanceSnapshot();

        rules = habitService.listRules();
        allowanceRowsByRuleId = allowanceSnapshot.getRows().stream()
                .collect(Collectors.toMap(HabitAllowanceRow::getRuleId, row -> row));

        if (editingRuleId != null && rules.stream().noneMatch(rule -> rule.getId().equals(editingRuleId))) {
            clearRuleForm();
        }

        updateAllowanceCards(allowanceSnapshot);
        updateAllowanceWarnings(allowanceSnapshot, summary);
        refreshRuleList();
        refreshEvaluations(allowanceSnapshot);
        refreshInsights(summary.getInsights());
    }

    private void updateAllowanceCards(HabitAllowanceSnapshot snapshot) {
        String currencyCode = resolveCurrencyCode();
        totalHabitSpentCard.setValues(
                "Total Habit Spent",
                MoneyUtils.format(snapshot.getSpentAcrossHabits(), currencyCode),
                appContext.getCurrentMonthDisplayText()
        );

        totalHabitExcessCard.setValues(
                "Total Habit Excess",
                MoneyUtils.format(snapshot.getExcessAcrossHabits(), currencyCode),
                "Above baseline tolerance"
        );

        habitPoolCard.setValues(
                "Habit Pool",
                MoneyUtils.format(snapshot.getHabitPoolAmount(), currencyCode),
                "Excess allowance budget"
        );

        remainingCard.setValues(
                "Remaining Pool",
                MoneyUtils.format(snapshot.getRemainingPool(), currencyCode),
                snapshot.getRemainingPool().compareTo(BigDecimal.ZERO) < 0 ? "Over allowance" : "Available"
        );

        String modeLabel = snapshot.getHabitMode() == HabitAllowanceMode.LOCKED ? "Locked" : "Dynamic";
        habitConfigCard.setValues(
                "Habit Config",
                snapshot.getHabitPercent().stripTrailingZeros().toPlainString() + "%",
                "Mode: " + modeLabel
        );

        allowanceMetaLabel.setText(
                "Available after reserve: " + MoneyUtils.format(snapshot.getAvailableAfterReserve(), currencyCode)
                        + " | Pace: " + snapshot.getDaysElapsed() + "/" + snapshot.getDaysInMonth()
                        + " (" + snapshot.getPaceRatio().multiply(MoneyUtils.HUNDRED).setScale(1, RoundingMode.HALF_UP) + "%)"
                        + " | Enabled habits: " + snapshot.getEnabledHabitsCount()
                        + " | Total weight: " + snapshot.getTotalWeight()
                        + " | Warnings: " + snapshot.getWarningCount()
                        + " | Exceeded: " + snapshot.getExceededCount()
        );
    }

    private void updateAllowanceWarnings(HabitAllowanceSnapshot snapshot, HabitPageSummary summary) {
        boolean showPoolWarning = snapshot.getHabitMode() == HabitAllowanceMode.DYNAMIC
                && snapshot.isPoolAdjustedDown()
                && summary.getExceededCount() > 0;

        if (showPoolWarning) {
            poolWarningLabel.setText("Habit allowance adjusted down because monthly availability decreased.");
            poolWarningLabel.setManaged(true);
            poolWarningLabel.setVisible(true);
        } else {
            poolWarningLabel.setManaged(false);
            poolWarningLabel.setVisible(false);
            poolWarningLabel.setText("");
        }
    }

    private void refreshRuleList() {
        ruleListBox.getChildren().clear();
        if (rules.isEmpty()) {
            ruleListBox.getChildren().add(new DataEmptyState(
                    "No habit rules",
                    "Create your first habit rule (e.g., #snacks, #coffee)."
            ));
            return;
        }

        for (HabitRule rule : rules) {
            HabitAllowanceRow row = allowanceRowsByRuleId.get(rule.getId());
            ruleListBox.getChildren().add(createRuleCard(rule, row));
        }
    }

    private Node createRuleCard(HabitRule rule, HabitAllowanceRow row) {
        HabitAllowanceRow safeRow = row == null ? fallbackRow(rule) : row;

        VBox card = new VBox(8);
        card.getStyleClass().add("habit-rule-card");

        Label title = new Label(rule.getDisplayName());
        title.getStyleClass().add("card-title");

        String hardLimitText = rule.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0
                ? MoneyUtils.format(rule.getMonthlyLimit(), resolveCurrencyCode())
                : "None";

        String meta = normalizeTag(rule.getTag())
                + (rule.getLinkedCategory() == null ? " | Any category" : " | " + rule.getLinkedCategory().getLabel())
                + " | Hard cap " + hardLimitText;
        Label metaLabel = UiUtils.createMutedLabel(meta);

        Label allocationLabel = UiUtils.createMutedLabel(
                "Allocated cap " + MoneyUtils.format(safeRow.getFinalCapAmount(), resolveCurrencyCode())
                        + " | Spent " + MoneyUtils.format(safeRow.getSpentAmount(), resolveCurrencyCode())
                        + " | Baseline " + MoneyUtils.format(safeRow.getBaselineAmount(), resolveCurrencyCode())
                        + " | Excess " + MoneyUtils.format(safeRow.getExcessSpentAmount(), resolveCurrencyCode())
                        + " | Remaining " + MoneyUtils.format(safeRow.getRemainingAmount(), resolveCurrencyCode())
                        + " | " + safeRow.getMatchedExpenseCount() + " matched"
        );
        Label paceHint = UiUtils.createMutedLabel(
                "Expected by today: " + MoneyUtils.format(safeRow.getCapToDateAmount(), resolveCurrencyCode()) + " (pace-adjusted)"
        );
        paceHint.getStyleClass().add("habit-pace-hint");

        ProgressBar progressBar = new ProgressBar(clampUsage(safeRow.getUsagePercent()));
        progressBar.getStyleClass().add("habit-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label(statusText(safeRow.getStatus()) + " - "
                + safeRow.getUsagePercent().stripTrailingZeros().toPlainString() + "% usage");
        statusLabel.getStyleClass().add(statusStyleClass(safeRow.getStatus()));

        CheckBox enabledCheck = new CheckBox("Enabled this month");
        enabledCheck.setSelected(rule.isEnabledThisMonth());
        enabledCheck.setOnAction(event -> onToggleEnabled(rule, enabledCheck.isSelected()));

        ComboBox<Integer> weightCombo = new ComboBox<>();
        weightCombo.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        weightCombo.getSelectionModel().select(Integer.valueOf(rule.getWeight()));
        weightCombo.getStyleClass().addAll("combo-box", "form-combo", "habit-weight-combo");
        weightCombo.setOnAction(event -> {
            Integer selected = weightCombo.getValue();
            if (selected != null) {
                onWeightChanged(rule, selected);
            }
        });

        Label weightLabel = UiUtils.createMutedLabel("Weight");
        HBox allocationControls = new HBox(8, enabledCheck, weightLabel, weightCombo);
        allocationControls.setAlignment(Pos.CENTER_LEFT);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        editButton.setOnAction(event -> loadRuleForEdit(rule));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
        deleteButton.setOnAction(event -> onDeleteRule(rule));

        HBox actions = new HBox(8, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, metaLabel, allocationLabel, paceHint, progressBar, statusLabel, allocationControls, actions);
        return card;
    }

    private void refreshEvaluations(HabitAllowanceSnapshot snapshot) {
        evaluationListBox.getChildren().clear();

        List<HabitAllowanceRow> enabledRows = snapshot.getRows().stream()
                .filter(HabitAllowanceRow::isEffectiveEnabled)
                .toList();

        if (enabledRows.isEmpty()) {
            evaluationListBox.getChildren().add(new DataEmptyState(
                    "No enabled habit rules",
                    "Enable at least one rule to distribute the monthly habit allowance pool."
            ));
            return;
        }

        for (HabitAllowanceRow row : enabledRows) {
            evaluationListBox.getChildren().add(createEvaluationCard(row));
        }
    }

    private Node createEvaluationCard(HabitAllowanceRow row) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label title = new Label(row.getDisplayName() + " (" + row.getTag() + ")");
        title.getStyleClass().add("card-title");

        Label detail = UiUtils.createMutedLabel(
                "Weight " + row.getWeight()
                        + " | Allocated cap " + MoneyUtils.format(row.getFinalCapAmount(), resolveCurrencyCode())
                        + " | Spent " + MoneyUtils.format(row.getSpentAmount(), resolveCurrencyCode())
                        + " | Baseline " + MoneyUtils.format(row.getBaselineAmount(), resolveCurrencyCode())
                        + " | Excess " + MoneyUtils.format(row.getExcessSpentAmount(), resolveCurrencyCode())
                        + " | Remaining " + MoneyUtils.format(row.getRemainingAmount(), resolveCurrencyCode())
        );
        Label paceHint = UiUtils.createMutedLabel(
                "Expected by today: " + MoneyUtils.format(row.getCapToDateAmount(), resolveCurrencyCode()) + " (pace-adjusted)"
        );
        paceHint.getStyleClass().add("habit-pace-hint");

        ProgressBar progressBar = new ProgressBar(clampUsage(row.getUsagePercent()));
        progressBar.getStyleClass().add("habit-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label(statusText(row.getStatus()) + " - "
                + row.getUsagePercent().stripTrailingZeros().toPlainString() + "% usage");
        status.getStyleClass().add(statusStyleClass(row.getStatus()));

        card.getChildren().addAll(title, detail, paceHint, progressBar, status);
        return card;
    }

    private void refreshInsights(List<HabitInsight> insights) {
        insightsBox.getChildren().clear();
        if (insights.isEmpty()) {
            insightsBox.getChildren().add(new DataEmptyState(
                    "No habit insights",
                    "No insight data available for this month yet."
            ));
            return;
        }

        for (HabitInsight insight : insights) {
            VBox row = new VBox(4);
            row.getStyleClass().add("alert-item");

            Label title = new Label(insight.getTitle());
            title.getStyleClass().add("card-title");
            title.getStyleClass().add(insightSeverityClass(insight.getSeverity()));

            Label message = UiUtils.createMutedLabel(insight.getMessage());
            row.getChildren().addAll(title, message);

            if (!insight.getActionHint().isBlank()) {
                Label action = UiUtils.createMutedLabel("Action: " + insight.getActionHint());
                row.getChildren().add(action);
            }

            insightsBox.getChildren().add(row);
        }
    }

    private HabitAllowanceRow fallbackRow(HabitRule rule) {
        return new HabitAllowanceRow(
                rule.getId(),
                rule.getDisplayName(),
                normalizeTag(rule.getTag()),
                rule.getLinkedCategory(),
                rule.isActive(),
                rule.isEnabledThisMonth(),
                rule.getWeight(),
                rule.getMonthlyLimit(),
                rule.getBaselineAmount(),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                HabitStatus.ON_TRACK,
                0
        );
    }

    private void loadRuleForEdit(HabitRule rule) {
        editingRuleId = rule.getId();
        displayNameField.setText(rule.getDisplayName());
        tagField.setText(normalizeTag(rule.getTag()));
        linkedCategoryCombo.getSelectionModel().select(rule.getLinkedCategory());
        monthlyLimitField.setMoneyValue(rule.getMonthlyLimit());
        baselineAmountField.setMoneyValue(rule.getBaselineAmount());
        warningThresholdField.setText(rule.getWarningThresholdPercent().stripTrailingZeros().toPlainString());
        activeCheck.setSelected(rule.isActive());
        notesArea.setText(rule.getNotes());

        saveRuleButton.setText("Save Changes");
        clearRuleButton.setText("Cancel Edit");
    }

    private void clearRuleForm() {
        editingRuleId = null;
        displayNameField.clear();
        tagField.clear();
        linkedCategoryCombo.getSelectionModel().selectFirst();
        monthlyLimitField.clear();
        baselineAmountField.clear();
        warningThresholdField.setText("70");
        activeCheck.setSelected(true);
        notesArea.clear();

        saveRuleButton.setText("Add Rule");
        clearRuleButton.setText("Clear Form");
    }

    private HabitRule findRuleOrThrow(String ruleId) {
        return rules.stream()
                .filter(rule -> rule.getId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Habit rule not found."));
    }

    private String normalizeTag(String rawTag) {
        String normalized = ValidationUtils.requireNonBlank(rawTag, "Tag").toLowerCase(Locale.ROOT);
        return normalized.startsWith("#") ? normalized : "#" + normalized;
    }

    private BigDecimal parseThreshold(String rawValue) {
        BigDecimal threshold = MoneyUtils.parseNonNegativeOrZero(rawValue, "Warning threshold");
        if (threshold.compareTo(MoneyUtils.HUNDRED) > 0) {
            throw new IllegalArgumentException("Warning threshold must be between 0 and 100.");
        }
        return threshold;
    }

    private double clampUsage(BigDecimal usagePercent) {
        if (usagePercent == null) {
            return 0;
        }
        double progress = usagePercent.doubleValue() / 100.0;
        return Math.max(0, Math.min(1, progress));
    }

    private String statusText(HabitStatus status) {
        return switch (status) {
            case ON_TRACK -> "OK";
            case WARNING -> "Warning";
            case EXCEEDED -> "Exceeded";
        };
    }

    private String statusStyleClass(HabitStatus status) {
        return switch (status) {
            case ON_TRACK -> "habit-status-ontrack";
            case WARNING -> "habit-status-warning";
            case EXCEEDED -> "habit-status-exceeded";
        };
    }

    private String insightSeverityClass(HabitSeverity severity) {
        return switch (severity) {
            case INFO -> "alert-info";
            case WARNING -> "alert-warning";
            case DANGER -> "alert-danger";
        };
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

    private void configureCategoryCombo(ComboBox<ExpenseCategory> comboBox) {
        comboBox.getStyleClass().addAll("combo-box", "form-combo");
        comboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Any Category");
                } else {
                    setText(item.getLabel());
                }
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Any Category");
                } else {
                    setText(item.getLabel());
                }
            }
        });
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String resolveCurrencyCode() {
        UserProfile profile = appContext.getCurrentUser();
        if (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank()) {
            return "EUR";
        }
        return profile.getCurrencyCode();
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
