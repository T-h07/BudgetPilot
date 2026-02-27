package com.budgetpilot.ui.components;

import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.month.MonthRolloverOptions;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.MonthUtils;
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
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.YearMonth;
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
            List<ExpenseTemplate> expenseTemplates,
            List<IncomeTemplate> incomeTemplates,
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
        copyPlannerPlan.setSelected(true);
        copyPlannerPlan.getStyleClass().add("check-box");

        List<ExpenseTemplateRow> expenseRows = buildExpenseRows(expenseTemplates, currencyCode);
        List<IncomeTemplateRow> incomeRows = buildIncomeRows(incomeTemplates, currencyCode);
        VBox expenseSection = buildExpenseSection(expenseRows);
        VBox incomeSection = buildIncomeSection(incomeRows);

        Label helpText = new Label("Only selected templates are generated. Existing template-generated entries are skipped.");
        helpText.getStyleClass().add("muted-text");
        helpText.setWrapText(true);

        VBox body = new VBox(12, copyPlannerPlan, expenseSection, incomeSection, helpText);
        body.setPadding(new Insets(8, 0, 4, 0));
        body.getStyleClass().addAll("page-root", "month-rollover-body");
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getStyleClass().addAll("page-root", "card", "month-rollover-dialog");
        dialog.getDialogPane().setPrefWidth(840);

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
            options.setSelectedExpenseTemplateIds(expenseRows.stream()
                    .filter(ExpenseTemplateRow::isSelected)
                    .map(row -> row.template.getId())
                    .toList());
            options.setSelectedIncomeTemplateIds(incomeRows.stream()
                    .filter(IncomeTemplateRow::isSelected)
                    .map(row -> row.template.getId())
                    .toList());
            return Result.start(options);
        });

        Optional<Result> result = dialog.showAndWait();
        return result.orElseGet(Result::notNow);
    }

    private static VBox buildExpenseSection(List<ExpenseTemplateRow> rows) {
        Label sectionTitle = new Label("Carry forward expense templates");
        sectionTitle.getStyleClass().add("card-title");

        TextField searchField = new TextField();
        searchField.setPromptText("Search templates...");
        searchField.getStyleClass().addAll("text-input", "form-input");

        Button selectAllButton = new Button("Select all");
        selectAllButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        selectAllButton.setOnAction(event -> rows.forEach(row -> row.setSelected(true)));

        Button clearSelectionButton = new Button("Clear selection");
        clearSelectionButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        clearSelectionButton.setOnAction(event -> rows.forEach(row -> row.setSelected(false)));

        HBox actions = new HBox(8, selectAllButton, clearSelectionButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox sectionsContainer = new VBox(10);
        sectionsContainer.getStyleClass().add("month-rollover-sections");
        populateExpenseBucketSections(sectionsContainer, rows);

        ScrollPane scrollPane = new ScrollPane(sectionsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(240);
        scrollPane.getStyleClass().addAll("card", "month-rollover-scroll");

        Label emptyLabel = new Label("No active expense templates found.");
        emptyLabel.getStyleClass().add("muted-text");
        emptyLabel.setManaged(rows.isEmpty());
        emptyLabel.setVisible(rows.isEmpty());

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            for (ExpenseTemplateRow row : rows) {
                row.setVisible(query.isBlank() || row.matches(query));
            }
            refreshSectionVisibility(sectionsContainer);
        });

        return new VBox(8, sectionTitle, searchField, actions, scrollPane, emptyLabel);
    }

    private static VBox buildIncomeSection(List<IncomeTemplateRow> rows) {
        Label sectionTitle = new Label("Carry forward income templates");
        sectionTitle.getStyleClass().add("card-title");

        Button selectAllButton = new Button("Select all");
        selectAllButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        selectAllButton.setOnAction(event -> rows.forEach(row -> row.setSelected(true)));

        Button clearSelectionButton = new Button("Clear selection");
        clearSelectionButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        clearSelectionButton.setOnAction(event -> rows.forEach(row -> row.setSelected(false)));

        HBox actions = new HBox(8, selectAllButton, clearSelectionButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox rowsBox = new VBox(6);
        rowsBox.getStyleClass().add("month-rollover-sections");
        for (IncomeTemplateRow row : rows) {
            rowsBox.getChildren().add(row.node);
        }

        ScrollPane scrollPane = new ScrollPane(rowsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(180);
        scrollPane.getStyleClass().addAll("card", "month-rollover-scroll");

        Label emptyLabel = new Label("No active income templates found.");
        emptyLabel.getStyleClass().add("muted-text");
        emptyLabel.setManaged(rows.isEmpty());
        emptyLabel.setVisible(rows.isEmpty());

        return new VBox(8, sectionTitle, actions, scrollPane, emptyLabel);
    }

    private static List<ExpenseTemplateRow> buildExpenseRows(List<ExpenseTemplate> templates, String currencyCode) {
        List<ExpenseTemplate> safeTemplates = templates == null ? List.of() : templates;
        return safeTemplates.stream()
                .filter(ExpenseTemplate::isActive)
                .sorted(Comparator
                        .comparing((ExpenseTemplate template) -> template.getPlannerBucket().ordinal())
                        .thenComparing(ExpenseTemplate::getName, String.CASE_INSENSITIVE_ORDER))
                .map(template -> new ExpenseTemplateRow(template, currencyCode))
                .toList();
    }

    private static List<IncomeTemplateRow> buildIncomeRows(List<IncomeTemplate> templates, String currencyCode) {
        List<IncomeTemplate> safeTemplates = templates == null ? List.of() : templates;
        return safeTemplates.stream()
                .filter(IncomeTemplate::isActive)
                .sorted(Comparator.comparing(IncomeTemplate::getSourceName, String.CASE_INSENSITIVE_ORDER))
                .map(template -> new IncomeTemplateRow(template, currencyCode))
                .toList();
    }

    private static void populateExpenseBucketSections(VBox sectionsContainer, List<ExpenseTemplateRow> rows) {
        Map<PlannerBucket, VBox> rowsByBucket = new EnumMap<>(PlannerBucket.class);
        for (PlannerBucket bucket : PlannerBucket.values()) {
            Label header = new Label(bucket.getDisplayName());
            header.getStyleClass().add("card-title");

            VBox bucketRows = new VBox(6);
            VBox section = new VBox(6, header, bucketRows);
            section.getStyleClass().add("month-rollover-bucket-section");
            rowsByBucket.put(bucket, bucketRows);
            sectionsContainer.getChildren().add(section);
        }

        for (ExpenseTemplateRow row : rows) {
            VBox bucketRows = rowsByBucket.get(row.template.getPlannerBucket());
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

    private static final class ExpenseTemplateRow {
        private final ExpenseTemplate template;
        private final VBox node;
        private final CheckBox selectedCheck;
        private final String searchableText;

        private ExpenseTemplateRow(ExpenseTemplate template, String currencyCode) {
            this.template = template;
            this.selectedCheck = new CheckBox(template.getName());
            this.selectedCheck.getStyleClass().add("check-box");
            this.selectedCheck.setSelected(true);

            String subcategory = template.getSubcategory() == null || template.getSubcategory().isBlank()
                    ? "-"
                    : template.getSubcategory();
            Label secondary = new Label(
                    template.getCategory().getLabel().toUpperCase(Locale.ROOT)
                            + " \u2022 "
                            + template.getPlannerBucket().getDisplayName()
                            + " \u2022 "
                            + subcategory
                            + " \u2022 "
                            + MoneyUtils.format(template.getDefaultAmount(), currencyCode)
                            + " \u2022 Day "
                            + template.getDayOfMonth()
            );
            secondary.getStyleClass().add("muted-text");

            this.node = new VBox(4, selectedCheck, secondary);
            this.node.setPadding(new Insets(8));
            this.node.getStyleClass().addAll("card", "month-rollover-template-row");

            this.searchableText = (
                    template.getName()
                            + " "
                            + template.getCategory().getLabel()
                            + " "
                            + template.getPlannerBucket().getDisplayName()
                            + " "
                            + template.getSubcategory()
                            + " "
                            + template.getTag()
                            + " "
                            + template.getNote()
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
    }

    private static final class IncomeTemplateRow {
        private final IncomeTemplate template;
        private final VBox node;
        private final CheckBox selectedCheck;

        private IncomeTemplateRow(IncomeTemplate template, String currencyCode) {
            this.template = template;
            this.selectedCheck = new CheckBox(template.getSourceName());
            this.selectedCheck.getStyleClass().add("check-box");
            this.selectedCheck.setSelected(true);

            Label secondary = new Label(
                    template.getIncomeType().getLabel().toUpperCase(Locale.ROOT)
                            + " \u2022 "
                            + MoneyUtils.format(template.getDefaultAmount(), currencyCode)
                            + " \u2022 Day "
                            + template.getDayOfMonth()
            );
            secondary.getStyleClass().add("muted-text");

            this.node = new VBox(4, selectedCheck, secondary);
            this.node.setPadding(new Insets(8));
            this.node.getStyleClass().addAll("card", "month-rollover-template-row");
        }

        private boolean isSelected() {
            return selectedCheck.isSelected();
        }

        private void setSelected(boolean selected) {
            selectedCheck.setSelected(selected);
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
