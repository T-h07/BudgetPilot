package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.service.export.ExportFormat;
import com.budgetpilot.service.export.ExportRequest;
import com.budgetpilot.service.export.ExportResult;
import com.budgetpilot.service.export.ExportService;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.util.AppPaths;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ExportPage extends VBox {
    private final AppContext appContext;
    private final ExportService exportService;
    private final Runnable contextListener = this::refreshFromContext;

    private final Label bannerLabel = new Label();
    private final ToggleGroup scopeGroup = new ToggleGroup();
    private final RadioButton selectedMonthRadio = new RadioButton("Selected Month");
    private final RadioButton monthRangeRadio = new RadioButton("Month Range");
    private final ComboBox<YearMonth> startMonthCombo = new ComboBox<>();
    private final ComboBox<YearMonth> endMonthCombo = new ComboBox<>();
    private final Label selectedMonthHint = UiUtils.createMutedLabel("");

    private final CheckBox expensesCheck = new CheckBox("Expenses");
    private final CheckBox incomeCheck = new CheckBox("Income");
    private final CheckBox savingsCheck = new CheckBox("Savings transactions");
    private final CheckBox goalsCheck = new CheckBox("Goal contributions");
    private final CheckBox plannerCheck = new CheckBox("Planner plan");
    private final CheckBox habitsCheck = new CheckBox("Habits summary");
    private final CheckBox familyCheck = new CheckBox("Family expenses");
    private final CheckBox investmentsCheck = new CheckBox("Investment transactions");

    private final ComboBox<ExportFormat> formatCombo = new ComboBox<>();
    private final ComboBox<PlannerBucket> bucketFilterCombo = new ComboBox<>();
    private final ComboBox<ExpenseCategory> categoryFilterCombo = new ComboBox<>();
    private final TextField tagFilterField = textField("Tag contains...");
    private final TextField subcategoryFilterField = textField("Subcategory contains...");

    private final Label outputDirLabel = UiUtils.createMutedLabel("");
    private final Button chooseFolderButton = new Button("Choose folder...");
    private final Button exportButton = new Button("Export");
    private final Button openFolderButton = new Button("Open Folder");
    private final VBox exportResultBox = new VBox(8);

    private Path selectedOutputDir;
    private Path lastExportOutputDir;

    public ExportPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.exportService = new ExportService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-export");

        getChildren().add(UiUtils.createPageHeader(
                "Export",
                "Create user-controlled data exports for selected months and datasets."
        ));

        setupBanner();
        setupScopeControls();
        setupDatasetControls();
        setupFormatAndFilters();
        setupDestinationControls();
        setupActionControls();

        HBox topRow = UiUtils.createTwoColumn(buildScopeCard(), buildDatasetsCard());
        HBox middleRow = UiUtils.createTwoColumn(buildFormatFiltersCard(), buildDestinationCard());
        SectionCard actionCard = buildActionCard();

        getChildren().addAll(topRow, middleRow, bannerLabel, actionCard);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });

        refreshFromContext();
    }

    private void setupBanner() {
        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.getStyleClass().add("error-banner");
    }

    private void setupScopeControls() {
        selectedMonthRadio.setToggleGroup(scopeGroup);
        monthRangeRadio.setToggleGroup(scopeGroup);
        selectedMonthRadio.setSelected(true);

        selectedMonthRadio.getStyleClass().add("export-scope-radio");
        monthRangeRadio.getStyleClass().add("export-scope-radio");

        setupYearMonthCombo(startMonthCombo, "Start month");
        setupYearMonthCombo(endMonthCombo, "End month");
        updateScopeState();
        scopeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> updateScopeState());
    }

    private void setupDatasetControls() {
        expensesCheck.setSelected(true);
        incomeCheck.setSelected(true);
    }

    private void setupFormatAndFilters() {
        formatCombo.getStyleClass().addAll("combo-box", "form-combo");
        formatCombo.getItems().setAll(ExportFormat.values());
        formatCombo.getSelectionModel().select(ExportFormat.CSV);
        formatCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ExportFormat item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        formatCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ExportFormat item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });

        setupPlannerBucketFilter();
        setupCategoryFilter();
    }

    private void setupDestinationControls() {
        chooseFolderButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        chooseFolderButton.setOnAction(event -> onChooseFolder());
        outputDirLabel.getStyleClass().add("export-path-label");
    }

    private void setupActionControls() {
        exportButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        exportButton.setOnAction(event -> onExport());

        openFolderButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        openFolderButton.setDisable(true);
        openFolderButton.setOnAction(event -> onOpenFolder());

        exportResultBox.getStyleClass().add("export-result-box");
        exportResultBox.getChildren().setAll(new DataEmptyState(
                "No exports yet",
                "Run an export to see generated files and row counts."
        ));
    }

    private SectionCard buildScopeCard() {
        HBox monthRangeRow = new HBox(10, startMonthCombo, endMonthCombo);
        HBox.setHgrow(startMonthCombo, Priority.ALWAYS);
        HBox.setHgrow(endMonthCombo, Priority.ALWAYS);
        monthRangeRow.getStyleClass().add("export-range-row");

        VBox body = new VBox(10,
                selectedMonthRadio,
                selectedMonthHint,
                monthRangeRadio,
                monthRangeRow
        );
        body.getStyleClass().add("export-section-body");
        return new SectionCard("Scope", "Choose selected month or custom month range.", body);
    }

    private SectionCard buildDatasetsCard() {
        VBox body = new VBox(8,
                expensesCheck,
                incomeCheck,
                savingsCheck,
                goalsCheck,
                plannerCheck,
                habitsCheck,
                familyCheck,
                investmentsCheck
        );
        body.getStyleClass().add("export-section-body");
        return new SectionCard("Datasets", "Select which datasets to include in the export.", body);
    }

    private SectionCard buildFormatFiltersCard() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Format", formatCombo);
        addFormRow(grid, 1, "Bucket filter (Expenses)", bucketFilterCombo);
        addFormRow(grid, 2, "Category filter (Expenses)", categoryFilterCombo);
        addFormRow(grid, 3, "Tag contains (Expenses)", tagFilterField);
        addFormRow(grid, 4, "Subcategory contains (Expenses)", subcategoryFilterField);

        VBox body = new VBox(10, grid);
        body.getStyleClass().add("export-section-body");
        return new SectionCard("Format & Expense Filters", "Filters apply only to Expenses export.", body);
    }

    private SectionCard buildDestinationCard() {
        HBox chooseRow = new HBox(10, chooseFolderButton);
        chooseRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(10,
                chooseRow,
                outputDirLabel
        );
        body.getStyleClass().add("export-section-body");
        return new SectionCard("Destination", "Export files are written to the selected folder.", body);
    }

    private SectionCard buildActionCard() {
        HBox actions = new HBox(10, exportButton, openFolderButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(2, 0, 0, 0));

        VBox body = new VBox(12, actions, exportResultBox);
        body.getStyleClass().add("export-section-body");
        return new SectionCard("Export Action", "Run export and review generated files.", body);
    }

    private void onChooseFolder() {
        clearBanner();
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Export Folder");

        Path initialPath = selectedOutputDir == null ? resolveDefaultExportDir() : selectedOutputDir;
        File initialDir = initialPath.toFile();
        if (initialDir.exists() && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }

        Window owner = getScene() == null ? null : getScene().getWindow();
        File selected = chooser.showDialog(owner);
        if (selected == null) {
            return;
        }

        selectedOutputDir = selected.toPath();
        updateOutputDirLabel();
    }

    private void onExport() {
        clearBanner();
        try {
            ExportRequest request = buildRequest();
            ExportResult result = exportService.export(request);
            lastExportOutputDir = request.getOutputDir();
            openFolderButton.setDisable(!Desktop.isDesktopSupported() || lastExportOutputDir == null);
            showExportResult(result);
            showSuccess(result.getMessage());
        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private void onOpenFolder() {
        clearBanner();
        if (lastExportOutputDir == null) {
            showError("No export folder available yet.");
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            showError("Desktop integration is not supported on this system.");
            return;
        }

        try {
            Desktop.getDesktop().open(lastExportOutputDir.toFile());
            showSuccess("Opened export folder.");
        } catch (IOException ex) {
            showError("Unable to open folder: " + ex.getMessage());
        }
    }

    private ExportRequest buildRequest() {
        YearMonth startMonth;
        YearMonth endMonth;
        if (selectedMonthRadio.isSelected()) {
            startMonth = appContext.getSelectedMonth();
            endMonth = appContext.getSelectedMonth();
        } else {
            startMonth = ValidationUtils.requireNonNull(startMonthCombo.getValue(), "startMonth");
            endMonth = ValidationUtils.requireNonNull(endMonthCombo.getValue(), "endMonth");
        }

        Path outputDir = selectedOutputDir == null ? resolveDefaultExportDir() : selectedOutputDir;

        return new ExportRequest(
                startMonth,
                endMonth,
                ValidationUtils.requireNonNull(formatCombo.getValue(), "format"),
                expensesCheck.isSelected(),
                incomeCheck.isSelected(),
                savingsCheck.isSelected(),
                goalsCheck.isSelected(),
                plannerCheck.isSelected(),
                habitsCheck.isSelected(),
                familyCheck.isSelected() && !familyCheck.isDisable(),
                investmentsCheck.isSelected() && !investmentsCheck.isDisable(),
                bucketFilterCombo.getValue(),
                categoryFilterCombo.getValue(),
                tagFilterField.getText(),
                subcategoryFilterField.getText(),
                outputDir
        );
    }

    private void showExportResult(ExportResult result) {
        exportResultBox.getChildren().clear();
        Label filesLabel = new Label("Files created");
        filesLabel.getStyleClass().add("form-label");
        exportResultBox.getChildren().add(filesLabel);

        for (Path path : result.getFilesWritten()) {
            Label fileLine = UiUtils.createMutedLabel(path.toAbsolutePath().toString());
            fileLine.getStyleClass().add("export-result-path");
            exportResultBox.getChildren().add(fileLine);
        }

        Label countsLabel = new Label("Row counts");
        countsLabel.getStyleClass().add("form-label");
        exportResultBox.getChildren().add(countsLabel);

        for (Map.Entry<String, Integer> entry : result.getRowCounts().entrySet()) {
            Label row = UiUtils.createMutedLabel(datasetLabel(entry.getKey()) + ": " + entry.getValue());
            row.getStyleClass().add("export-result-count");
            exportResultBox.getChildren().add(row);
        }
    }

    private String datasetLabel(String datasetKey) {
        return switch (datasetKey) {
            case "expenses" -> "Expenses";
            case "income" -> "Income";
            case "savingsEntries" -> "Savings transactions";
            case "goalContributions" -> "Goal contributions";
            case "plannerPlans" -> "Planner plan";
            case "habitsSummary" -> "Habits summary";
            case "familyExpenses" -> "Family expenses";
            case "investmentTransactions" -> "Investment transactions";
            default -> datasetKey;
        };
    }

    private void refreshFromContext() {
        refreshModuleGating();
        refreshMonthOptions();
        updateScopeState();
        updateOutputDirLabel();
    }

    private void refreshModuleGating() {
        UserProfile profile = appContext.getCurrentUser();
        boolean familyEnabled = profile != null && profile.isFamilyModuleEnabled();
        boolean investmentsEnabled = profile != null && profile.isInvestmentsModuleEnabled();

        familyCheck.setDisable(!familyEnabled);
        if (!familyEnabled) {
            familyCheck.setSelected(false);
            familyCheck.setText("Family expenses (module disabled)");
        } else {
            familyCheck.setText("Family expenses");
        }

        investmentsCheck.setDisable(!investmentsEnabled);
        if (!investmentsEnabled) {
            investmentsCheck.setSelected(false);
            investmentsCheck.setText("Investment transactions (module disabled)");
        } else {
            investmentsCheck.setText("Investment transactions");
        }
    }

    private void refreshMonthOptions() {
        List<YearMonth> options = buildMonthOptions(appContext.getSelectedMonth());
        YearMonth currentStart = startMonthCombo.getValue();
        YearMonth currentEnd = endMonthCombo.getValue();

        startMonthCombo.getItems().setAll(options);
        endMonthCombo.getItems().setAll(options);

        YearMonth selectedMonth = appContext.getSelectedMonth();
        if (currentStart != null && options.contains(currentStart)) {
            startMonthCombo.getSelectionModel().select(currentStart);
        } else {
            startMonthCombo.getSelectionModel().select(selectedMonth.minusMonths(2));
            if (startMonthCombo.getValue() == null && !options.isEmpty()) {
                startMonthCombo.getSelectionModel().select(options.get(0));
            }
        }

        if (currentEnd != null && options.contains(currentEnd)) {
            endMonthCombo.getSelectionModel().select(currentEnd);
        } else {
            endMonthCombo.getSelectionModel().select(selectedMonth);
            if (endMonthCombo.getValue() == null && !options.isEmpty()) {
                endMonthCombo.getSelectionModel().select(options.get(options.size() - 1));
            }
        }
    }

    private List<YearMonth> buildMonthOptions(YearMonth anchor) {
        List<YearMonth> months = new ArrayList<>();
        for (int i = -24; i <= 12; i++) {
            months.add(anchor.plusMonths(i));
        }
        months.sort(Comparator.naturalOrder());
        return List.copyOf(months);
    }

    private void updateScopeState() {
        boolean monthRange = monthRangeRadio.isSelected();
        startMonthCombo.setDisable(!monthRange);
        endMonthCombo.setDisable(!monthRange);
        selectedMonthHint.setText("Using selected month from top bar: " + MonthUtils.format(appContext.getSelectedMonth()));
    }

    private Path resolveDefaultExportDir() {
        return AppPaths.getExportsDir();
    }

    private void updateOutputDirLabel() {
        Path outputDir = selectedOutputDir == null ? resolveDefaultExportDir() : selectedOutputDir;
        outputDirLabel.setText("Current folder: " + outputDir.toAbsolutePath());
    }

    private void setupYearMonthCombo(ComboBox<YearMonth> combo, String prompt) {
        combo.getStyleClass().addAll("combo-box", "form-combo");
        combo.setPromptText(prompt);
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(YearMonth item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : MonthUtils.format(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(YearMonth item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : MonthUtils.format(item));
            }
        });
    }

    private void setupPlannerBucketFilter() {
        bucketFilterCombo.getStyleClass().addAll("combo-box", "form-combo");
        bucketFilterCombo.getItems().setAll((PlannerBucket) null);
        bucketFilterCombo.getItems().addAll(PlannerBucket.values());
        bucketFilterCombo.getSelectionModel().selectFirst();
        bucketFilterCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PlannerBucket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("All Buckets");
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        bucketFilterCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PlannerBucket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("All Buckets");
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
    }

    private void setupCategoryFilter() {
        categoryFilterCombo.getStyleClass().addAll("combo-box", "form-combo");
        categoryFilterCombo.getItems().setAll((ExpenseCategory) null);
        categoryFilterCombo.getItems().addAll(ExpenseCategory.values());
        categoryFilterCombo.getSelectionModel().selectFirst();
        categoryFilterCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("All Categories");
                } else {
                    setText(item.getLabel());
                }
            }
        });
        categoryFilterCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("All Categories");
                } else {
                    setText(item.getLabel());
                }
            }
        });
    }

    private void addFormRow(GridPane grid, int row, String labelText, Node node) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        grid.add(label, 0, row);
        grid.add(node, 1, row);
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("text-input", "form-input");
        return field;
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
}
