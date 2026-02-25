package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.service.expenses.ExpenseCategorySummary;
import com.budgetpilot.service.expenses.ExpenseFilter;
import com.budgetpilot.service.expenses.ExpenseService;
import com.budgetpilot.service.expenses.ExpenseSummary;
import com.budgetpilot.service.forecast.ForecastService;
import com.budgetpilot.service.forecast.ForecastSummary;
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
import javafx.scene.control.ListCell;
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
import java.util.Optional;

public class ExpensesPage extends VBox {
    private final AppContext appContext;
    private final ExpenseService expenseService;
    private final ForecastService forecastService;
    private final ObservableList<ExpenseEntry> expenseRows = FXCollections.observableArrayList();
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final MoneyField amountField = new MoneyField("Amount", "Amount");
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<ExpenseCategory> categoryCombo = new ComboBox<>();
    private final TextField subcategoryField = textField("Subcategory");
    private final ComboBox<PaymentMethod> paymentMethodCombo = new ComboBox<>();
    private final TextField tagField = textField("Tag (e.g. #snacks)");
    private final TextArea noteArea = new TextArea();

    private final Button saveButton = new Button("Add Expense");
    private final Button clearButton = new Button("Clear Form");
    private final Button clearFiltersButton = new Button("Clear Filters");

    private final TextField searchFilterField = textField("Search note, subcategory, category...");
    private final ComboBox<ExpenseCategory> categoryFilterCombo = new ComboBox<>();
    private final ComboBox<PaymentMethod> paymentMethodFilterCombo = new ComboBox<>();
    private final TextField tagFilterField = textField("Tag filter");
    private final CheckBox onlyTaggedCheck = new CheckBox("Only tagged");

    private final TableView<ExpenseEntry> expensesTable = new TableView<>(expenseRows);
    private final SummaryStatCard totalSpentCard = new SummaryStatCard();
    private final SummaryStatCard averageDailyCard = new SummaryStatCard();
    private final SummaryStatCard countCard = new SummaryStatCard();
    private final SummaryStatCard topCategoryCard = new SummaryStatCard();

    private final VBox categoryBreakdownBox = new VBox(10);
    private final VBox forecastBox = new VBox(8);
    private final Label forecastStatusLabel = new Label();

    private ExpenseEntry editingEntry;
    private List<ExpenseEntry> monthEntries = List.of();

    public ExpensesPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.expenseService = new ExpenseService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));
        this.forecastService = new ForecastService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-expenses");

        getChildren().add(UiUtils.createPageHeader(
                "Expenses",
                "Track daily spending, filter transactions, and forecast month-end outcomes for "
                        + appContext.getCurrentMonthDisplayText() + "."
        ));

        setupBanner();
        setupFormDefaults();
        setupFilterDefaults();
        setupTable();
        setupActions();

        HBox summaryRow = buildSummaryRow();
        SectionCard filterCard = buildFilterCard();
        HBox mainRow = buildMainRow();
        HBox insightsRow = buildInsightsRow();

        getChildren().addAll(summaryRow, bannerLabel, filterCard, mainRow, insightsRow);

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

    private void setupFormDefaults() {
        datePicker.getStyleClass().addAll("date-picker", "form-datepicker");
        categoryCombo.getItems().setAll(ExpenseCategory.values());
        categoryCombo.getSelectionModel().select(ExpenseCategory.OTHER);
        categoryCombo.getStyleClass().addAll("combo-box", "form-combo");

        paymentMethodCombo.getItems().setAll(PaymentMethod.values());
        paymentMethodCombo.getSelectionModel().select(PaymentMethod.CARD);
        paymentMethodCombo.getStyleClass().addAll("combo-box", "form-combo");

        noteArea.setPromptText("Note or merchant");
        noteArea.getStyleClass().addAll("text-area", "form-textarea");
        noteArea.setPrefRowCount(3);
    }

    private void setupFilterDefaults() {
        configureFilterCombo(categoryFilterCombo, "All Categories", ExpenseCategory::getLabel);
        categoryFilterCombo.getItems().add(null);
        categoryFilterCombo.getItems().addAll(ExpenseCategory.values());
        categoryFilterCombo.getSelectionModel().selectFirst();

        configureFilterCombo(paymentMethodFilterCombo, "All Payment Methods", PaymentMethod::getLabel);
        paymentMethodFilterCombo.getItems().add(null);
        paymentMethodFilterCombo.getItems().addAll(PaymentMethod.values());
        paymentMethodFilterCombo.getSelectionModel().selectFirst();

        searchFilterField.textProperty().addListener((obs, oldText, newText) -> refreshFilteredTable());
        tagFilterField.textProperty().addListener((obs, oldText, newText) -> refreshFilteredTable());
        categoryFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshFilteredTable());
        paymentMethodFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshFilteredTable());
        onlyTaggedCheck.selectedProperty().addListener((obs, oldValue, newValue) -> refreshFilteredTable());
    }

    private void setupActions() {
        saveButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveButton.setOnAction(event -> onSaveExpense());

        clearButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearButton.setOnAction(event -> clearForm());
        clearFiltersButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearFiltersButton.setOnAction(event -> clearFilters());
    }

    private void setupTable() {
        expensesTable.getStyleClass().add("expenses-table");
        expensesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        expensesTable.setPlaceholder(new DataEmptyState(
                "No expenses yet",
                "Add your first expense entry for this month to start tracking spending."
        ));

        TableColumn<ExpenseEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getExpenseDate() == null ? "" : data.getValue().getExpenseDate().toString()
        ));

        TableColumn<ExpenseEntry, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(data.getValue().getAmount(), resolveCurrencyCode())
        ));

        TableColumn<ExpenseEntry, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().getLabel()));

        TableColumn<ExpenseEntry, String> subcategoryCol = new TableColumn<>("Subcategory");
        subcategoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubcategory()));

        TableColumn<ExpenseEntry, String> paymentCol = new TableColumn<>("Payment");
        paymentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMethod().getLabel()));

        TableColumn<ExpenseEntry, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTag()));
        tagCol.setCellFactory(col -> new TableCell<>() {
            private final Label tagLabel = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                String value = item == null ? "" : item.trim();
                if (value.isBlank()) {
                    tagLabel.getStyleClass().setAll("muted-text");
                    tagLabel.setText("-");
                } else {
                    tagLabel.getStyleClass().setAll("tag-badge");
                    tagLabel.setText(value);
                }
                setGraphic(tagLabel);
            }
        });

        TableColumn<ExpenseEntry, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

        TableColumn<ExpenseEntry, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actions = new HBox(6, editBtn, deleteBtn);

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
                deleteBtn.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");

                editBtn.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    ExpenseEntry entry = getTableView().getItems().get(rowIndex);
                    loadForEdit(entry);
                });

                deleteBtn.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    ExpenseEntry entry = getTableView().getItems().get(rowIndex);
                    if (confirmDelete(entry)) {
                        expenseService.deleteExpense(entry.getId());
                        if (editingEntry != null && editingEntry.getId().equals(entry.getId())) {
                            clearForm();
                        }
                        appContext.notifyContextChanged();
                        showSuccess("Expense deleted.");
                        refreshAll();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });

        expensesTable.getColumns().setAll(
                dateCol,
                amountCol,
                categoryCol,
                subcategoryCol,
                paymentCol,
                tagCol,
                noteCol,
                actionsCol
        );
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(UiUtils.CARD_GAP, totalSpentCard, averageDailyCard, countCard, topCategoryCard);
        row.getStyleClass().add("expenses-toolbar");
        for (Node node : row.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(region, Priority.ALWAYS);
            }
        }
        return row;
    }

    private SectionCard buildFilterCard() {
        HBox bar = new HBox(10,
                searchFilterField,
                categoryFilterCombo,
                paymentMethodFilterCombo,
                tagFilterField,
                onlyTaggedCheck,
                clearFiltersButton
        );
        bar.getStyleClass().add("expenses-filter-bar");
        HBox.setHgrow(searchFilterField, Priority.ALWAYS);
        HBox.setHgrow(tagFilterField, Priority.SOMETIMES);

        Label scopeLabel = UiUtils.createMutedLabel("Filters update the table only. Summary cards remain month-wide.");
        VBox body = new VBox(8, bar, scopeLabel);
        SectionCard card = new SectionCard("Filters", "Search and narrow your expense list quickly.", body);
        card.getStyleClass().add("expenses-toolbar");
        return card;
    }

    private HBox buildMainRow() {
        SectionCard formCard = new SectionCard(
                "Expense Form",
                "Add or update spending records for the selected month.",
                buildFormBody()
        );
        formCard.getStyleClass().add("expenses-form-card");

        SectionCard tableCard = new SectionCard(
                "Expense Entries",
                "Newest transactions appear first.",
                expensesTable
        );
        tableCard.getStyleClass().add("expenses-table-card");
        expensesTable.setPrefHeight(420);

        HBox row = new HBox(UiUtils.CARD_GAP, formCard, tableCard);
        HBox.setHgrow(formCard, Priority.ALWAYS);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        formCard.setMaxWidth(Double.MAX_VALUE);
        tableCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private HBox buildInsightsRow() {
        forecastStatusLabel.getStyleClass().add("muted-text");
        forecastBox.getChildren().add(forecastStatusLabel);

        SectionCard categoryCard = new SectionCard(
                "Category Breakdown",
                "How this month is split across expense categories.",
                categoryBreakdownBox
        );
        categoryCard.getStyleClass().add("category-breakdown-card");

        SectionCard forecastCard = new SectionCard(
                "Forecast",
                "Projected month-end spend and remaining balance based on current pace.",
                forecastBox
        );
        forecastCard.getStyleClass().add("forecast-card");

        HBox row = new HBox(UiUtils.CARD_GAP, categoryCard, forecastCard);
        HBox.setHgrow(categoryCard, Priority.ALWAYS);
        HBox.setHgrow(forecastCard, Priority.ALWAYS);
        categoryCard.setMaxWidth(Double.MAX_VALUE);
        forecastCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildFormBody() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Amount", amountField);
        addFormRow(grid, 1, "Date", datePicker);
        addFormRow(grid, 2, "Category", categoryCombo);
        addFormRow(grid, 3, "Subcategory", subcategoryField);
        addFormRow(grid, 4, "Payment Method", paymentMethodCombo);
        addFormRow(grid, 5, "Tag", tagField);
        addFormRow(grid, 6, "Note", noteArea);

        HBox actions = new HBox(10, saveButton, clearButton);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, grid, actions);
    }

    private void onSaveExpense() {
        clearBanner();
        try {
            LocalDate expenseDate = ValidationUtils.requireNonNull(datePicker.getValue(), "Date");
            YearMonth dateMonth = YearMonth.from(expenseDate);
            if (!dateMonth.equals(appContext.getSelectedMonth())) {
                throw new IllegalArgumentException("Expense date must be within " + appContext.getCurrentMonthDisplayText() + ".");
            }

            ExpenseEntry target = editingEntry == null ? new ExpenseEntry() : editingEntry.copy();
            target.setMonth(appContext.getSelectedMonth());
            target.setExpenseDate(expenseDate);
            target.setAmount(amountField.parseRequiredPositive());
            target.setCategory(ValidationUtils.requireNonNull(categoryCombo.getValue(), "Category"));
            target.setSubcategory(subcategoryField.getText());
            target.setPaymentMethod(ValidationUtils.requireNonNull(paymentMethodCombo.getValue(), "Payment method"));
            target.setTag(tagField.getText());
            target.setNote(noteArea.getText());

            expenseService.saveExpense(target);
            appContext.notifyContextChanged();
            showSuccess(editingEntry == null ? "Expense added." : "Expense updated.");
            clearForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadForEdit(ExpenseEntry entry) {
        editingEntry = entry;
        amountField.setMoneyValue(entry.getAmount());
        datePicker.setValue(entry.getExpenseDate());
        categoryCombo.getSelectionModel().select(entry.getCategory());
        subcategoryField.setText(entry.getSubcategory());
        paymentMethodCombo.getSelectionModel().select(entry.getPaymentMethod());
        tagField.setText(entry.getTag());
        noteArea.setText(entry.getNote());

        saveButton.setText("Save Changes");
        clearButton.setText("Cancel Edit");
    }

    private void clearForm() {
        editingEntry = null;
        amountField.clear();
        datePicker.setValue(defaultDateForSelectedMonth());
        categoryCombo.getSelectionModel().select(ExpenseCategory.OTHER);
        subcategoryField.clear();
        paymentMethodCombo.getSelectionModel().select(PaymentMethod.CARD);
        tagField.clear();
        noteArea.clear();
        saveButton.setText("Add Expense");
        clearButton.setText("Clear Form");
    }

    private void clearFilters() {
        searchFilterField.clear();
        categoryFilterCombo.getSelectionModel().selectFirst();
        paymentMethodFilterCombo.getSelectionModel().selectFirst();
        tagFilterField.clear();
        onlyTaggedCheck.setSelected(false);
        refreshFilteredTable();
    }

    private void refreshAll() {
        monthEntries = expenseService.listForMonth(appContext.getSelectedMonth());
        refreshFilteredTable();
        updateSummaryCards();
        updateCategoryBreakdown();
        updateForecastPanel();

        if (editingEntry != null && monthEntries.stream().noneMatch(entry -> entry.getId().equals(editingEntry.getId()))) {
            clearForm();
        } else if (editingEntry == null && datePicker.getValue() != null
                && !YearMonth.from(datePicker.getValue()).equals(appContext.getSelectedMonth())) {
            datePicker.setValue(defaultDateForSelectedMonth());
        }
    }

    private void refreshFilteredTable() {
        ExpenseFilter filter = new ExpenseFilter();
        filter.setSearchText(searchFilterField.getText());
        filter.setCategory(categoryFilterCombo.getValue());
        filter.setPaymentMethod(paymentMethodFilterCombo.getValue());
        filter.setTagText(tagFilterField.getText());
        filter.setOnlyTagged(onlyTaggedCheck.isSelected());

        List<ExpenseEntry> filtered = expenseService.listForMonth(appContext.getSelectedMonth(), filter);
        expenseRows.setAll(filtered);
    }

    private void updateSummaryCards() {
        ExpenseSummary summary = expenseService.getExpenseSummary(appContext.getSelectedMonth());
        String currencyCode = resolveCurrencyCode();

        totalSpentCard.setValues(
                "Total Spent",
                MoneyUtils.format(summary.getTotalExpenses(), currencyCode),
                appContext.getCurrentMonthDisplayText()
        );
        averageDailyCard.setValues(
                "Average Daily Spend",
                MoneyUtils.format(summary.getAverageDailySpend(), currencyCode),
                "Daily pace based on selected month context"
        );
        countCard.setValues(
                "Expense Count",
                String.valueOf(summary.getExpenseCount()),
                summary.getDistinctCategoriesCount() + " categories"
        );

        if (summary.getTopCategory() == null) {
            topCategoryCard.setValues("Top Category", "None yet", "Add entries to calculate");
        } else {
            topCategoryCard.setValues(
                    "Top Category",
                    summary.getTopCategory().getLabel(),
                    MoneyUtils.format(summary.getTopCategoryAmount(), currencyCode)
            );
        }
    }

    private void updateCategoryBreakdown() {
        categoryBreakdownBox.getChildren().clear();
        String currencyCode = resolveCurrencyCode();
        List<ExpenseCategorySummary> summaries = expenseService.getCategorySummaries(appContext.getSelectedMonth());
        if (summaries.isEmpty()) {
            categoryBreakdownBox.getChildren().add(UiUtils.createMutedLabel("No category data yet for this month."));
            return;
        }

        for (ExpenseCategorySummary summary : summaries) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label categoryLabel = new Label(summary.getCategory().getLabel() + " (" + summary.getEntryCount() + ")");
            categoryLabel.getStyleClass().add("muted-text");

            Label valueLabel = new Label(
                    MoneyUtils.format(summary.getTotal(), currencyCode)
                            + " | "
                            + summary.getPercentOfTotal().toPlainString() + "%"
            );
            valueLabel.getStyleClass().add("info-row-value");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(categoryLabel, spacer, valueLabel);

            categoryBreakdownBox.getChildren().add(row);
        }
    }

    private void updateForecastPanel() {
        forecastBox.getChildren().clear();
        String currencyCode = resolveCurrencyCode();
        ForecastSummary forecastSummary = forecastService.buildForecast(
                appContext.getSelectedMonth(),
                isFamilyModuleEnabled()
        );

        forecastBox.getChildren().addAll(
                metricRow("Actual Spent", MoneyUtils.format(forecastSummary.getActualSpentSoFar(), currencyCode)),
                metricRow("Avg Daily Spend", MoneyUtils.format(forecastSummary.getAverageDailySpend(), currencyCode)),
                metricRow("Projected Month-End Spend", MoneyUtils.format(forecastSummary.getProjectedExpensesByMonthEnd(), currencyCode)),
                metricRow("Projected Remaining (After Expenses)", MoneyUtils.format(forecastSummary.getProjectedRemainingAfterExpenses(), currencyCode)),
                metricRow("Projected Remaining (After Plan)", MoneyUtils.format(forecastSummary.getProjectedRemainingAfterPlan(), currencyCode)),
                metricRow("Days Elapsed", forecastSummary.getDaysElapsed() + " / " + forecastSummary.getDaysInMonth())
        );

        forecastStatusLabel.setText(forecastSummary.getStatusMessage());
        forecastStatusLabel.getStyleClass().removeAll("status-good", "status-warn", "status-danger");
        if (forecastSummary.isOverspendingRisk()) {
            forecastStatusLabel.getStyleClass().add("status-danger");
        } else if (forecastSummary.isHasNoData() || forecastSummary.isPlanMissing()) {
            forecastStatusLabel.getStyleClass().add("status-warn");
        } else {
            forecastStatusLabel.getStyleClass().add("status-good");
        }
        forecastBox.getChildren().add(forecastStatusLabel);
    }

    private HBox metricRow(String title, String value) {
        Label titleLabel = UiUtils.createMutedLabel(title);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-row-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, titleLabel, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private boolean confirmDelete(ExpenseEntry entry) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete Expense");
        alert.setContentText("Delete " + MoneyUtils.format(entry.getAmount(), resolveCurrencyCode())
                + " from " + entry.getExpenseDate() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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

    private LocalDate defaultDateForSelectedMonth() {
        YearMonth selectedMonth = appContext.getSelectedMonth();
        YearMonth currentMonth = YearMonth.now();
        if (selectedMonth.equals(currentMonth)) {
            return LocalDate.now();
        }
        return selectedMonth.atDay(1);
    }

    private void addFormRow(GridPane grid, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    private <T> void configureFilterCombo(
            ComboBox<T> comboBox,
            String allText,
            java.util.function.Function<T, String> itemLabelProvider
    ) {
        comboBox.getStyleClass().addAll("combo-box", "form-combo");
        comboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText(allText);
                } else {
                    setText(itemLabelProvider.apply(item));
                }
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText(allText);
                } else {
                    setText(itemLabelProvider.apply(item));
                }
            }
        });
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
