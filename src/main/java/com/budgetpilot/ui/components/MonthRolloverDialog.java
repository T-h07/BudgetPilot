package com.budgetpilot.ui.components;

import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.month.ExpenseTemplateCandidate;
import com.budgetpilot.service.month.ExpenseTemplateSelection;
import com.budgetpilot.service.month.MonthRolloverOptions;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.MonthUtils;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MonthRolloverDialog {
    private MonthRolloverDialog() {
    }

    public static Result show(
            Window owner,
            YearMonth targetMonth,
            List<ExpenseTemplateCandidate> templateCandidates,
            String currencyCode
    ) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("New month detected: " + MonthUtils.format(targetMonth));
        dialog.setHeaderText("Start a new month and optionally carry forward templates from last month.");

        if (owner != null) {
            dialog.initOwner(owner);
            Scene ownerScene = owner.getScene();
            if (ownerScene != null) {
                dialog.getDialogPane().getStylesheets().addAll(ownerScene.getStylesheets());
            }
        }

        CheckBox copyPlannerPlan = new CheckBox("Copy last month planner plan");
        CheckBox carryRecurringIncome = new CheckBox("Carry forward recurring income");
        CheckBox carryRecurringExpenses = new CheckBox("Carry forward recurring expenses");
        copyPlannerPlan.setSelected(true);
        carryRecurringIncome.setSelected(true);
        carryRecurringExpenses.setSelected(true);
        copyPlannerPlan.getStyleClass().add("check-box");
        carryRecurringIncome.getStyleClass().add("check-box");
        carryRecurringExpenses.getStyleClass().add("check-box");

        List<TemplateRow> templateRows = buildTemplateRows(templateCandidates, currencyCode);
        VBox templateSection = buildTemplateSection(carryRecurringExpenses, templateRows);

        Label helpText = new Label("Only selected templates are copied. Unplanned one-time expenses are copied only if selected.");
        helpText.getStyleClass().add("muted-text");
        helpText.setWrapText(true);

        VBox body = new VBox(10, copyPlannerPlan, carryRecurringIncome, carryRecurringExpenses, templateSection, helpText);
        body.setPadding(new Insets(8, 0, 4, 0));
        body.getStyleClass().addAll("page-root", "month-rollover-body");
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getStyleClass().addAll("page-root", "card", "month-rollover-dialog");
        dialog.getDialogPane().setPrefWidth(780);

        ButtonType notNowType = new ButtonType("Not now", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType startType = new ButtonType("Start New Month", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(notNowType, startType);

        Node startButtonNode = dialog.getDialogPane().lookupButton(startType);
        if (startButtonNode instanceof Button startButton) {
            startButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        }
        Node notNowButtonNode = dialog.getDialogPane().lookupButton(notNowType);
        if (notNowButtonNode instanceof Button notNowButton) {
            notNowButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        }

        dialog.setResultConverter(buttonType -> {
            if (buttonType != startType) {
                return Result.notNow();
            }
            MonthRolloverOptions options = new MonthRolloverOptions();
            options.setCopyPlannerPlan(copyPlannerPlan.isSelected());
            options.setCarryForwardRecurringIncome(carryRecurringIncome.isSelected());
            options.setCarryForwardRecurringExpenses(carryRecurringExpenses.isSelected());
            if (carryRecurringExpenses.isSelected()) {
                List<ExpenseTemplateSelection> selections = templateRows.stream()
                        .filter(TemplateRow::isSelected)
                        .map(TemplateRow::toSelection)
                        .toList();
                options.setSelectedExpenseTemplates(selections);
            } else {
                options.setSelectedExpenseTemplates(List.of());
            }
            return Result.start(options);
        });

        Optional<Result> result = dialog.showAndWait();
        return result.orElseGet(Result::notNow);
    }

    private static VBox buildTemplateSection(CheckBox carryRecurringExpenses, List<TemplateRow> templateRows) {
        VBox section = new VBox(8);

        TextField searchField = new TextField();
        searchField.setPromptText("Search templates...");
        searchField.getStyleClass().addAll("text-input", "form-input");

        Button selectRecurringButton = new Button("Select all recurring");
        selectRecurringButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        selectRecurringButton.setOnAction(event -> templateRows.forEach(row -> row.setSelected(row.candidate.wasRecurring())));

        Button clearSelectionButton = new Button("Clear selection");
        clearSelectionButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        clearSelectionButton.setOnAction(event -> templateRows.forEach(row -> row.setSelected(false)));

        HBox actions = new HBox(8, selectRecurringButton, clearSelectionButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox sectionsContainer = new VBox(10);
        sectionsContainer.setPadding(new Insets(4, 0, 0, 0));
        sectionsContainer.getStyleClass().add("month-rollover-sections");
        populateBucketSections(sectionsContainer, templateRows);

        ScrollPane scrollPane = new ScrollPane(sectionsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(260);
        scrollPane.getStyleClass().addAll("card", "month-rollover-scroll");

        Label emptyLabel = new Label("No expense templates found in last month.");
        emptyLabel.getStyleClass().add("muted-text");
        emptyLabel.setManaged(templateRows.isEmpty());
        emptyLabel.setVisible(templateRows.isEmpty());

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            for (TemplateRow row : templateRows) {
                row.setVisible(query.isBlank() || row.matches(query));
            }
            refreshSectionVisibility(sectionsContainer);
        });

        VBox content = new VBox(8, searchField, actions, scrollPane, emptyLabel);
        content.visibleProperty().bind(carryRecurringExpenses.selectedProperty());
        content.managedProperty().bind(carryRecurringExpenses.selectedProperty());

        section.getChildren().add(content);
        return section;
    }

    private static List<TemplateRow> buildTemplateRows(List<ExpenseTemplateCandidate> templateCandidates, String currencyCode) {
        List<ExpenseTemplateCandidate> safeCandidates = templateCandidates == null ? List.of() : templateCandidates;
        return safeCandidates.stream()
                .sorted(Comparator
                        .comparing((ExpenseTemplateCandidate candidate) -> candidate.getBucket().ordinal())
                        .thenComparing(ExpenseTemplateCandidate::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(candidate -> new TemplateRow(candidate, currencyCode))
                .toList();
    }

    private static void populateBucketSections(VBox sectionsContainer, List<TemplateRow> templateRows) {
        Map<PlannerBucket, VBox> rowsByBucket = new EnumMap<>(PlannerBucket.class);
        Map<PlannerBucket, VBox> sectionByBucket = new EnumMap<>(PlannerBucket.class);
        for (PlannerBucket bucket : PlannerBucket.values()) {
            Label header = new Label(bucket.getDisplayName());
            header.getStyleClass().add("card-title");

            VBox bucketRows = new VBox(6);
            VBox section = new VBox(6, header, bucketRows);
            section.getStyleClass().add("month-rollover-bucket-section");
            sectionByBucket.put(bucket, section);
            rowsByBucket.put(bucket, bucketRows);
            sectionsContainer.getChildren().add(section);
        }

        for (TemplateRow row : templateRows) {
            VBox bucketRows = rowsByBucket.get(row.candidate.getBucket());
            if (bucketRows != null) {
                bucketRows.getChildren().add(row.node);
            }
        }
        refreshSectionVisibility(sectionsContainer);
    }

    private static void refreshSectionVisibility(VBox sectionsContainer) {
        for (Node sectionNode : sectionsContainer.getChildren()) {
            if (!(sectionNode instanceof VBox section) || section.getChildren().size() < 2) {
                continue;
            }
            Node rowsNode = section.getChildren().get(1);
            if (!(rowsNode instanceof VBox rowsBox)) {
                continue;
            }
            boolean hasVisibleRows = rowsBox.getChildren().stream().anyMatch(Node::isVisible);
            section.setManaged(hasVisibleRows);
            section.setVisible(hasVisibleRows);
        }
    }

    private static final class TemplateRow {
        private final ExpenseTemplateCandidate candidate;
        private final VBox node;
        private final CheckBox selectedCheck;
        private final CheckBox recurringCheck;
        private final String searchableText;

        private TemplateRow(ExpenseTemplateCandidate candidate, String currencyCode) {
            this.candidate = candidate;
            this.selectedCheck = new CheckBox(candidate.getDisplayName());
            this.selectedCheck.getStyleClass().add("check-box");
            this.selectedCheck.setSelected(candidate.wasRecurring());

            Label secondary = new Label(
                    candidate.getCategory().getLabel().toUpperCase(Locale.ROOT)
                            + " \u2022 "
                            + candidate.getBucket().getDisplayName()
                            + " \u2022 Last "
                            + MoneyUtils.format(candidate.getLastAmount(), currencyCode)
            );
            secondary.getStyleClass().add("muted-text");

            this.recurringCheck = new CheckBox("Mark as recurring for future months");
            this.recurringCheck.getStyleClass().add("check-box");
            this.recurringCheck.setSelected(candidate.wasRecurring());
            this.recurringCheck.disableProperty().bind(selectedCheck.selectedProperty().not());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox recurringRow = new HBox(8, secondary, spacer, recurringCheck);
            recurringRow.setAlignment(Pos.CENTER_LEFT);

            this.node = new VBox(4, selectedCheck, recurringRow);
            this.node.setPadding(new Insets(8));
            this.node.getStyleClass().addAll("card", "month-rollover-template-row");

            this.searchableText = (
                    candidate.getDisplayName()
                            + " "
                            + candidate.getCategory().getLabel()
                            + " "
                            + candidate.getBucket().getDisplayName()
                            + " "
                            + candidate.getSubcategory()
                            + " "
                            + candidate.getTag()
                            + " "
                            + candidate.getNote()
            ).toLowerCase(Locale.ROOT);
        }

        private boolean matches(String query) {
            return searchableText.contains(query);
        }

        private boolean isSelected() {
            return selectedCheck.isSelected();
        }

        private void setSelected(boolean selected) {
            selectedCheck.setSelected(selected);
        }

        private void setVisible(boolean visible) {
            node.setVisible(visible);
            node.setManaged(visible);
        }

        private ExpenseTemplateSelection toSelection() {
            return new ExpenseTemplateSelection(candidate, recurringCheck.isSelected());
        }
    }

    public static final class Result {
        private final boolean startNewMonth;
        private final MonthRolloverOptions options;

        private Result(boolean startNewMonth, MonthRolloverOptions options) {
            this.startNewMonth = startNewMonth;
            this.options = options;
        }

        public static Result start(MonthRolloverOptions options) {
            return new Result(true, options);
        }

        public static Result notNow() {
            return new Result(false, null);
        }

        public boolean isStartNewMonth() {
            return startNewMonth;
        }

        public MonthRolloverOptions getOptions() {
            return options;
        }
    }
}
