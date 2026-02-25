package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.service.IncomeService;
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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IncomePage extends VBox {
    private final AppContext appContext;
    private final IncomeService incomeService;
    private final ObservableList<IncomeEntry> incomeRows = FXCollections.observableArrayList();
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final TextField sourceField = textField("Source name");
    private final ComboBox<IncomeType> typeCombo = new ComboBox<>();
    private final MoneyField amountField = new MoneyField("Amount", "Amount");
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final CheckBox recurringCheck = new CheckBox("Recurring");
    private final CheckBox receivedCheck = new CheckBox("Received");
    private final TextArea notesArea = new TextArea();

    private final Button saveButton = new Button("Add Income");
    private final Button clearButton = new Button("Clear Form");
    private final TableView<IncomeEntry> incomeTable = new TableView<>(incomeRows);

    private final SummaryStatCard plannedIncomeCard = new SummaryStatCard();
    private final SummaryStatCard receivedIncomeCard = new SummaryStatCard();
    private final SummaryStatCard recurringIncomeCard = new SummaryStatCard();
    private final SummaryStatCard sourceCountCard = new SummaryStatCard();

    private IncomeEntry editingEntry;

    public IncomePage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.incomeService = new IncomeService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-income");

        getChildren().add(UiUtils.createPageHeader(
                "Income",
                "Add, edit, and review monthly income sources for " + appContext.getCurrentMonthDisplayText() + "."
        ));

        setupBanner();
        setupFormDefaults();
        setupTable();
        setupActions();

        HBox summaryRow = buildSummaryRow();
        HBox contentRow = buildMainContentRow();

        getChildren().addAll(summaryRow, bannerLabel, contentRow);

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
        typeCombo.getItems().setAll(IncomeType.values());
        typeCombo.getSelectionModel().select(IncomeType.SALARY);
        typeCombo.getStyleClass().addAll("combo-box", "form-combo");
        datePicker.getStyleClass().addAll("date-picker", "form-datepicker");

        notesArea.setPromptText("Optional notes");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().addAll("text-area", "form-textarea");

        receivedCheck.setSelected(true);
    }

    private void setupActions() {
        saveButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveButton.setOnAction(event -> onSave());
        clearButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearButton.setOnAction(event -> clearForm());
    }

    private void setupTable() {
        incomeTable.getStyleClass().add("income-table");
        incomeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        incomeTable.setPlaceholder(new DataEmptyState(
                "No income entries yet",
                "Add your first income source for this month to start planning."
        ));

        TableColumn<IncomeEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getReceivedDate() == null ? "" : data.getValue().getReceivedDate().toString()
        ));

        TableColumn<IncomeEntry, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSourceName()));

        TableColumn<IncomeEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIncomeType().getLabel()));

        TableColumn<IncomeEntry, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(data.getValue().getAmount(), resolveCurrencyCode())
        ));

        TableColumn<IncomeEntry, String> recurringCol = new TableColumn<>("Recurring");
        recurringCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isRecurring() ? "Yes" : "No"));

        TableColumn<IncomeEntry, String> receivedCol = new TableColumn<>("Received");
        receivedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isReceived() ? "Yes" : "No"));

        TableColumn<IncomeEntry, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(6, editBtn, deleteBtn);

            {
                actionBox.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
                editBtn.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    IncomeEntry rowEntry = getTableView().getItems().get(rowIndex);
                    loadForEdit(rowEntry);
                });
                deleteBtn.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
                deleteBtn.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    IncomeEntry rowEntry = getTableView().getItems().get(rowIndex);
                    incomeService.deleteIncome(rowEntry.getId());
                    if (editingEntry != null && editingEntry.getId().equals(rowEntry.getId())) {
                        clearForm();
                    }
                    refreshAll();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });

        incomeTable.getColumns().setAll(dateCol, sourceCol, typeCol, amountCol, recurringCol, receivedCol, actionsCol);
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(UiUtils.CARD_GAP, plannedIncomeCard, receivedIncomeCard, recurringIncomeCard, sourceCountCard);
        row.getStyleClass().add("income-summary-grid");
        HBox.setHgrow(plannedIncomeCard, Priority.ALWAYS);
        HBox.setHgrow(receivedIncomeCard, Priority.ALWAYS);
        HBox.setHgrow(recurringIncomeCard, Priority.ALWAYS);
        HBox.setHgrow(sourceCountCard, Priority.ALWAYS);

        plannedIncomeCard.setMaxWidth(Double.MAX_VALUE);
        receivedIncomeCard.setMaxWidth(Double.MAX_VALUE);
        recurringIncomeCard.setMaxWidth(Double.MAX_VALUE);
        sourceCountCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private HBox buildMainContentRow() {
        SectionCard formCard = new SectionCard(
                "Income Form",
                "Create or update monthly income records.",
                buildFormBody()
        );

        SectionCard listCard = new SectionCard(
                "Income Entries",
                "Newest entries appear first.",
                incomeTable
        );

        formCard.getStyleClass().add("income-form-card");
        listCard.getStyleClass().add("income-list-card");
        incomeTable.setPrefHeight(420);
        HBox row = new HBox(UiUtils.CARD_GAP, formCard, listCard);
        HBox.setHgrow(formCard, Priority.ALWAYS);
        HBox.setHgrow(listCard, Priority.ALWAYS);
        formCard.setMaxWidth(Double.MAX_VALUE);
        listCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildFormBody() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Source", sourceField);
        addFormRow(grid, 1, "Type", typeCombo);
        addFormRow(grid, 2, "Amount", amountField);
        addFormRow(grid, 3, "Received Date", datePicker);

        HBox toggles = new HBox(10, recurringCheck, receivedCheck);
        toggles.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("Flags"), 0, 4);
        grid.add(toggles, 1, 4);

        grid.add(new Label("Notes"), 0, 5);
        grid.add(notesArea, 1, 5);

        HBox actions = new HBox(10, saveButton, clearButton);
        actions.setPadding(new Insets(8, 0, 0, 0));
        return new VBox(10, grid, actions);
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

    private void onSave() {
        clearBanner();
        try {
            IncomeEntry target = editingEntry == null ? new IncomeEntry() : editingEntry.copy();
            target.setMonth(appContext.getSelectedMonth());
            target.setSourceName(ValidationUtils.requireNonBlank(sourceField.getText(), "Source"));
            target.setIncomeType(ValidationUtils.requireNonNull(typeCombo.getValue(), "Income type"));
            target.setAmount(amountField.parseRequiredPositive());
            target.setReceivedDate(ValidationUtils.requireNonNull(datePicker.getValue(), "Received date"));
            target.setRecurring(recurringCheck.isSelected());
            target.setReceived(receivedCheck.isSelected());
            target.setNotes(notesArea.getText());

            incomeService.saveIncome(target);
            showSuccess(editingEntry == null ? "Income entry added." : "Income entry updated.");
            clearForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadForEdit(IncomeEntry entry) {
        editingEntry = entry;
        sourceField.setText(entry.getSourceName());
        typeCombo.getSelectionModel().select(entry.getIncomeType());
        amountField.setMoneyValue(entry.getAmount());
        datePicker.setValue(entry.getReceivedDate());
        recurringCheck.setSelected(entry.isRecurring());
        receivedCheck.setSelected(entry.isReceived());
        notesArea.setText(entry.getNotes());
        saveButton.setText("Save Changes");
    }

    private void clearForm() {
        editingEntry = null;
        sourceField.clear();
        typeCombo.getSelectionModel().select(IncomeType.SALARY);
        amountField.clear();
        datePicker.setValue(LocalDate.now());
        recurringCheck.setSelected(false);
        receivedCheck.setSelected(true);
        notesArea.clear();
        saveButton.setText("Add Income");
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        List<IncomeEntry> entries = incomeService.listForMonth(month);
        incomeRows.setAll(entries);
        updateSummary(entries, month);

        if (editingEntry != null && entries.stream().noneMatch(entry -> entry.getId().equals(editingEntry.getId()))) {
            clearForm();
        }
    }

    private void updateSummary(List<IncomeEntry> entries, YearMonth month) {
        String currencyCode = resolveCurrencyCode();
        BigDecimal planned = incomeService.getPlannedIncomeTotal(month);
        BigDecimal received = incomeService.getReceivedIncomeTotal(month);
        BigDecimal recurring = incomeService.getRecurringIncomeTotal(month);
        Set<String> sourceNames = new HashSet<>();
        for (IncomeEntry entry : entries) {
            sourceNames.add(entry.getSourceName());
        }

        plannedIncomeCard.setValues("Planned Income", MoneyUtils.format(planned, currencyCode), entries.size() + " entries");
        receivedIncomeCard.setValues("Received Income", MoneyUtils.format(received, currencyCode), "Marked as received");
        recurringIncomeCard.setValues("Recurring Income", MoneyUtils.format(recurring, currencyCode), "Recurring sources only");
        sourceCountCard.setValues("Sources Count", String.valueOf(sourceNames.size()), "Unique source names");
    }

    private String resolveCurrencyCode() {
        UserProfile profile = appContext.getCurrentUser();
        return (profile == null || profile.getCurrencyCode() == null || profile.getCurrencyCode().isBlank())
                ? "EUR"
                : profile.getCurrencyCode();
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
