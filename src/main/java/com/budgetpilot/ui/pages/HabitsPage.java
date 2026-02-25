package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.HabitSeverity;
import com.budgetpilot.service.HabitInsight;
import com.budgetpilot.service.HabitPageSummary;
import com.budgetpilot.service.HabitService;
import com.budgetpilot.service.HabitSpendSummary;
import com.budgetpilot.service.HabitStatus;
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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class HabitsPage extends VBox {
    private final AppContext appContext;
    private final HabitService habitService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final SummaryStatCard trackedSpendCard = new SummaryStatCard();
    private final SummaryStatCard activeRulesCard = new SummaryStatCard();
    private final SummaryStatCard warningRulesCard = new SummaryStatCard();
    private final SummaryStatCard exceededRulesCard = new SummaryStatCard();
    private final SummaryStatCard onTrackCard = new SummaryStatCard();

    private final TextField displayNameField = textField("Display name");
    private final TextField tagField = textField("Tag (e.g. #snacks)");
    private final ComboBox<ExpenseCategory> linkedCategoryCombo = new ComboBox<>();
    private final MoneyField monthlyLimitField = new MoneyField("Monthly Limit", "Monthly limit");
    private final TextField warningThresholdField = textField("Warning threshold %");
    private final CheckBox activeCheck = new CheckBox("Active");
    private final TextArea notesArea = new TextArea();
    private final Button saveRuleButton = new Button("Add Rule");
    private final Button clearRuleButton = new Button("Clear Form");

    private final VBox ruleListBox = new VBox(10);
    private final VBox evaluationListBox = new VBox(10);
    private final VBox insightsBox = new VBox(10);

    private List<HabitRule> rules = List.of();
    private Map<String, HabitSpendSummary> evaluationMap = new HashMap<>();
    private String selectedRuleId;
    private String editingRuleId;

    public HabitsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.habitService = new HabitService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-habits");

        getChildren().add(UiUtils.createPageHeader(
                "Habits",
                "Control spending behavior with monthly limits, warning thresholds, and actionable coaching insights."
        ));

        setupBanner();
        setupRuleForm();
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

    private void setupRuleForm() {
        configureCategoryCombo(linkedCategoryCombo);
        linkedCategoryCombo.getItems().setAll((ExpenseCategory) null);
        linkedCategoryCombo.getItems().addAll(ExpenseCategory.values());
        linkedCategoryCombo.getSelectionModel().selectFirst();

        warningThresholdField.setText("70");
        activeCheck.setSelected(true);

        notesArea.setPromptText("Optional notes");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().add("text-input");

        ruleListBox.getStyleClass().add("habit-rule-list");
        evaluationListBox.getStyleClass().add("habit-evaluation-list");
        insightsBox.getStyleClass().add("habit-insights-panel");
    }

    private void setupActions() {
        saveRuleButton.getStyleClass().add("quick-add-button");
        saveRuleButton.setOnAction(event -> onSaveRule());
        clearRuleButton.setOnAction(event -> clearRuleForm());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                trackedSpendCard,
                activeRulesCard,
                warningRulesCard,
                exceededRulesCard,
                onTrackCard
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
                "Create and manage spending habit controls by tag and category.",
                buildRulesBody()
        );

        SectionCard rightCard = new SectionCard(
                "Evaluations & Insights",
                "Live month-to-date behavior analysis powered by your expense data.",
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
        addFormRow(grid, 3, "Monthly Limit", monthlyLimitField);
        addFormRow(grid, 4, "Warning Threshold %", warningThresholdField);

        Label activeLabel = new Label("Status");
        activeLabel.getStyleClass().add("form-label");
        grid.add(activeLabel, 0, 5);
        grid.add(activeCheck, 1, 5);

        addFormRow(grid, 6, "Notes", notesArea);

        HBox actions = new HBox(10, saveRuleButton, clearRuleButton);
        actions.setPadding(new Insets(8, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, grid, actions);
    }

    private Node buildEvaluationsBody() {
        VBox section = new VBox(14,
                new Label("Habit Evaluations"),
                evaluationListBox,
                new Label("Insights"),
                insightsBox
        );
        section.getChildren().get(0).getStyleClass().add("form-label");
        section.getChildren().get(2).getStyleClass().add("form-label");
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
            target.setMonthlyLimit(monthlyLimitField.parseRequiredPositive());
            target.setWarningThresholdPercent(parseThreshold(warningThresholdField.getText()));
            target.setActive(activeCheck.isSelected());
            target.setNotes(notesArea.getText());

            habitService.saveRule(target);
            selectedRuleId = target.getId();
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
        if (rule.getId().equals(selectedRuleId)) {
            selectedRuleId = null;
        }
        if (rule.getId().equals(editingRuleId)) {
            clearRuleForm();
        }
        appContext.notifyContextChanged();
        showSuccess("Habit rule deleted.");
        refreshAll();
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        HabitPageSummary summary = habitService.getHabitPageSummary(month);

        rules = habitService.listRules();
        evaluationMap = new HashMap<>();
        for (HabitSpendSummary evaluation : summary.getEvaluations()) {
            evaluationMap.put(evaluation.getRuleId(), evaluation);
        }

        if (selectedRuleId == null && !rules.isEmpty()) {
            selectedRuleId = rules.get(0).getId();
        }
        if (selectedRuleId != null && rules.stream().noneMatch(rule -> rule.getId().equals(selectedRuleId))) {
            selectedRuleId = rules.isEmpty() ? null : rules.get(0).getId();
        }
        if (editingRuleId != null && rules.stream().noneMatch(rule -> rule.getId().equals(editingRuleId))) {
            clearRuleForm();
        }

        updateSummaryCards(summary);
        refreshRuleList();
        refreshEvaluations(summary);
        refreshInsights(summary.getInsights());
    }

    private void updateSummaryCards(HabitPageSummary summary) {
        String currencyCode = resolveCurrencyCode();
        trackedSpendCard.setValues(
                "Habit-Tracked Spend",
                MoneyUtils.format(summary.getHabitTrackedSpend(), currencyCode),
                appContext.getCurrentMonthDisplayText()
        );
        activeRulesCard.setValues(
                "Active Habit Rules",
                String.valueOf(summary.getActiveRulesCount()),
                rules.size() + " total rules"
        );
        warningRulesCard.setValues(
                "Warning Rules",
                String.valueOf(summary.getWarningCount()),
                "Near threshold/limit"
        );
        exceededRulesCard.setValues(
                "Exceeded Rules",
                String.valueOf(summary.getExceededCount()),
                "Monthly limit exceeded"
        );
        onTrackCard.setValues(
                "On-Track Rules",
                String.valueOf(summary.getOnTrackCount()),
                "Healthy spending behavior"
        );
    }

    private void refreshRuleList() {
        ruleListBox.getChildren().clear();
        if (rules.isEmpty()) {
            ruleListBox.getChildren().add(new DataEmptyState(
                    "No habit rules",
                    "Create your first habit rule (e.g., #snacks, #clothes, #coffee)."
            ));
            return;
        }

        for (HabitRule rule : rules) {
            ruleListBox.getChildren().add(createRuleCard(rule));
        }
    }

    private Node createRuleCard(HabitRule rule) {
        HabitSpendSummary evaluation = evaluationMap.get(rule.getId());
        HabitStatus status = evaluation == null ? HabitStatus.ON_TRACK : evaluation.getStatus();

        VBox card = new VBox(8);
        card.getStyleClass().add("habit-rule-card");
        if (rule.getId().equals(selectedRuleId)) {
            card.getStyleClass().add("habit-rule-card-selected");
        }

        Label title = new Label(rule.getDisplayName());
        title.getStyleClass().add("card-title");

        String meta = normalizeTag(rule.getTag())
                + (rule.getLinkedCategory() == null ? " | Any category" : " | " + rule.getLinkedCategory().getLabel())
                + " | Limit " + MoneyUtils.format(rule.getMonthlyLimit(), resolveCurrencyCode());
        Label metaLabel = UiUtils.createMutedLabel(meta);

        BigDecimal spend = evaluation == null ? BigDecimal.ZERO.setScale(2) : evaluation.getActualSpend();
        String statusText = statusText(status);
        Label spendLabel = new Label("Spend " + MoneyUtils.format(spend, resolveCurrencyCode()) + " | " + statusText);
        spendLabel.getStyleClass().add(statusStyleClass(status));

        Button selectButton = new Button("Select");
        selectButton.setOnAction(event -> {
            selectedRuleId = rule.getId();
            refreshAll();
        });
        Button editButton = new Button("Edit");
        editButton.setOnAction(event -> loadRuleForEdit(rule));
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> onDeleteRule(rule));

        HBox actions = new HBox(8, selectButton, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, metaLabel, spendLabel, actions);
        return card;
    }

    private void refreshEvaluations(HabitPageSummary summary) {
        evaluationListBox.getChildren().clear();

        List<HabitSpendSummary> activeEvals = summary.getEvaluations().stream()
                .filter(HabitSpendSummary::isActive)
                .toList();

        if (activeEvals.isEmpty()) {
            evaluationListBox.getChildren().add(new DataEmptyState(
                    "No active habit rules",
                    "Enable or create habit rules to evaluate monthly spending behavior."
            ));
            return;
        }

        for (HabitSpendSummary evaluation : activeEvals) {
            evaluationListBox.getChildren().add(createEvaluationCard(evaluation));
        }
    }

    private Node createEvaluationCard(HabitSpendSummary evaluation) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label title = new Label(evaluation.getDisplayName() + " (" + evaluation.getTag() + ")");
        title.getStyleClass().add("card-title");

        String line = "Spent " + MoneyUtils.format(evaluation.getActualSpend(), resolveCurrencyCode())
                + " | Limit " + MoneyUtils.format(evaluation.getMonthlyLimit(), resolveCurrencyCode())
                + " | Remaining " + MoneyUtils.format(evaluation.getRemainingBeforeLimit(), resolveCurrencyCode())
                + " | " + evaluation.getMatchedExpenseCount() + " matched expenses";
        Label detail = UiUtils.createMutedLabel(line);

        ProgressBar progressBar = new ProgressBar(clampUsage(evaluation.getUsagePercent()));
        progressBar.getStyleClass().add("habit-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label(statusText(evaluation.getStatus()) + " - "
                + evaluation.getUsagePercent().stripTrailingZeros().toPlainString() + "% usage");
        status.getStyleClass().add(statusStyleClass(evaluation.getStatus()));

        card.getChildren().addAll(title, detail, progressBar, status);
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
    private void loadRuleForEdit(HabitRule rule) {
        editingRuleId = rule.getId();
        displayNameField.setText(rule.getDisplayName());
        tagField.setText(normalizeTag(rule.getTag()));
        linkedCategoryCombo.getSelectionModel().select(rule.getLinkedCategory());
        monthlyLimitField.setMoneyValue(rule.getMonthlyLimit());
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
            case ON_TRACK -> "On Track";
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
        comboBox.getStyleClass().add("combo-box");
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
        field.getStyleClass().add("text-input");
        return field;
    }
}
