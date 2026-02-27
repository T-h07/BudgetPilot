package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.PlannerBucket;
import com.budgetpilot.model.enums.RecurrenceCadence;
import com.budgetpilot.service.templates.TemplateService;
import com.budgetpilot.ui.components.DataEmptyState;
import com.budgetpilot.ui.components.MoneyField;
import com.budgetpilot.ui.components.SectionCard;
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
import java.util.List;

public class TemplatesPage extends VBox {
    private final AppContext appContext;
    private final TemplateService templateService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final TextField expenseNameField = textField("Template name");
    private final ComboBox<PlannerBucket> expenseBucketCombo = combo();
    private final ComboBox<ExpenseCategory> expenseCategoryCombo = combo();
    private final TextField expenseSubcategoryField = textField("Subcategory (e.g. Therapist)");
    private final ComboBox<PaymentMethod> expensePaymentMethodCombo = combo();
    private final MoneyField expenseAmountField = new MoneyField("Default Amount", "Default amount");
    private final ComboBox<RecurrenceCadence> expenseCadenceCombo = combo();
    private final TextField expenseDayOfMonthField = textField("Day of month");
    private final CheckBox expenseActiveCheck = new CheckBox("Active");
    private final TextField expenseTagField = textField("Tag (optional)");
    private final TextArea expenseNoteArea = new TextArea();
    private final Button saveExpenseTemplateButton = new Button("Create Expense Template");
    private final Button clearExpenseTemplateButton = new Button("Clear Form");

    private final ObservableList<ExpenseTemplate> expenseTemplateRows = FXCollections.observableArrayList();
    private final TableView<ExpenseTemplate> expenseTemplateTable = new TableView<>(expenseTemplateRows);
    private String editingExpenseTemplateId;

    private final TextField incomeSourceField = textField("Source name");
    private final ComboBox<IncomeType> incomeTypeCombo = combo();
    private final MoneyField incomeAmountField = new MoneyField("Default Amount", "Default amount");
    private final ComboBox<RecurrenceCadence> incomeCadenceCombo = combo();
    private final TextField incomeDayOfMonthField = textField("Day of month");
    private final CheckBox incomeActiveCheck = new CheckBox("Active");
    private final TextArea incomeNoteArea = new TextArea();
    private final Button saveIncomeTemplateButton = new Button("Create Income Template");
    private final Button clearIncomeTemplateButton = new Button("Clear Form");

    private final ObservableList<IncomeTemplate> incomeTemplateRows = FXCollections.observableArrayList();
    private final TableView<IncomeTemplate> incomeTemplateTable = new TableView<>(incomeTemplateRows);
    private String editingIncomeTemplateId;

    public TemplatesPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.templateService = new TemplateService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-templates");

        getChildren().add(UiUtils.createPageHeader(
                "Templates",
                "Manage recurring expense and income templates for clean month rollover and automatic generation."
        ));

        setupBanner();
        setupExpenseForm();
        setupIncomeForm();
        setupExpenseTable();
        setupIncomeTable();
        setupActions();

        SectionCard expenseSection = new SectionCard(
                "Expense Templates",
                "Create recurring spending templates by bucket, category, and subcategory.",
                buildExpenseSectionBody()
        );
        expenseSection.getStyleClass().add("templates-section");

        SectionCard incomeSection = new SectionCard(
                "Income Templates",
                "Create recurring income templates used for month rollover generation.",
                buildIncomeSectionBody()
        );
        incomeSection.getStyleClass().add("templates-section");

        getChildren().addAll(bannerLabel, expenseSection, incomeSection);

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

    private void setupExpenseForm() {
        expenseBucketCombo.getItems().setAll(PlannerBucket.values());
        expenseCategoryCombo.getItems().setAll(ExpenseCategory.values());
        expensePaymentMethodCombo.getItems().setAll(PaymentMethod.values());
        expenseCadenceCombo.getItems().setAll(RecurrenceCadence.values());

        expenseBucketCombo.getSelectionModel().select(PlannerBucket.DISCRETIONARY);
        expenseCategoryCombo.getSelectionModel().select(ExpenseCategory.OTHER);
        expensePaymentMethodCombo.getSelectionModel().select(PaymentMethod.CARD);
        expenseCadenceCombo.getSelectionModel().select(RecurrenceCadence.MONTHLY);
        expenseDayOfMonthField.setText("1");
        expenseActiveCheck.setSelected(true);

        expenseNoteArea.setPromptText("Optional notes");
        expenseNoteArea.setPrefRowCount(3);
        expenseNoteArea.getStyleClass().addAll("text-area", "form-textarea");
    }

    private void setupIncomeForm() {
        incomeTypeCombo.getItems().setAll(IncomeType.values());
        incomeCadenceCombo.getItems().setAll(RecurrenceCadence.values());
        incomeTypeCombo.getSelectionModel().select(IncomeType.OTHER);
        incomeCadenceCombo.getSelectionModel().select(RecurrenceCadence.MONTHLY);
        incomeDayOfMonthField.setText("1");
        incomeActiveCheck.setSelected(true);

        incomeNoteArea.setPromptText("Optional notes");
        incomeNoteArea.setPrefRowCount(3);
        incomeNoteArea.getStyleClass().addAll("text-area", "form-textarea");
    }

    private void setupExpenseTable() {
        expenseTemplateTable.getStyleClass().add("templates-table");
        expenseTemplateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        expenseTemplateTable.setPlaceholder(new DataEmptyState(
                "No expense templates",
                "Create templates such as Rent, Internet, and Therapist."
        ));

        TableColumn<ExpenseTemplate, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<ExpenseTemplate, String> bucketColumn = new TableColumn<>("Bucket");
        bucketColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPlannerBucket().getDisplayName()));

        TableColumn<ExpenseTemplate, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().getLabel()));

        TableColumn<ExpenseTemplate, String> subcategoryColumn = new TableColumn<>("Subcategory");
        subcategoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubcategory()));

        TableColumn<ExpenseTemplate, String> amountColumn = new TableColumn<>("Default");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(data.getValue().getDefaultAmount(), resolveCurrencyCode())
        ));

        TableColumn<ExpenseTemplate, String> scheduleColumn = new TableColumn<>("Cadence");
        scheduleColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCadence().getLabel() + " • Day " + data.getValue().getDayOfMonth()
        ));

        TableColumn<ExpenseTemplate, String> activeColumn = new TableColumn<>("Status");
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));

        TableColumn<ExpenseTemplate, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actions = new HBox(8, editButton, deleteButton);

            {
                editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
                deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
                editButton.setOnAction(event -> {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) {
                        return;
                    }
                    loadExpenseTemplateForEdit(getTableView().getItems().get(index));
                });
                deleteButton.setOnAction(event -> {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) {
                        return;
                    }
                    deleteExpenseTemplate(getTableView().getItems().get(index));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });

        expenseTemplateTable.getColumns().setAll(
                nameColumn, bucketColumn, categoryColumn, subcategoryColumn, amountColumn, scheduleColumn, activeColumn, actionsColumn
        );
    }

    private void setupIncomeTable() {
        incomeTemplateTable.getStyleClass().add("templates-table");
        incomeTemplateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        incomeTemplateTable.setPlaceholder(new DataEmptyState(
                "No income templates",
                "Create recurring templates such as Salary and Side Income."
        ));

        TableColumn<IncomeTemplate, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSourceName()));

        TableColumn<IncomeTemplate, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIncomeType().getLabel()));

        TableColumn<IncomeTemplate, String> amountColumn = new TableColumn<>("Default");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(data.getValue().getDefaultAmount(), resolveCurrencyCode())
        ));

        TableColumn<IncomeTemplate, String> cadenceColumn = new TableColumn<>("Cadence");
        cadenceColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCadence().getLabel() + " • Day " + data.getValue().getDayOfMonth()
        ));

        TableColumn<IncomeTemplate, String> activeColumn = new TableColumn<>("Status");
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));

        TableColumn<IncomeTemplate, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actions = new HBox(8, editButton, deleteButton);

            {
                editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
                deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
                editButton.setOnAction(event -> {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) {
                        return;
                    }
                    loadIncomeTemplateForEdit(getTableView().getItems().get(index));
                });
                deleteButton.setOnAction(event -> {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) {
                        return;
                    }
                    deleteIncomeTemplate(getTableView().getItems().get(index));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });

        incomeTemplateTable.getColumns().setAll(
                sourceColumn, typeColumn, amountColumn, cadenceColumn, activeColumn, actionsColumn
        );
    }

    private void setupActions() {
        saveExpenseTemplateButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveExpenseTemplateButton.setOnAction(event -> onSaveExpenseTemplate());

        clearExpenseTemplateButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearExpenseTemplateButton.setOnAction(event -> clearExpenseForm());

        saveIncomeTemplateButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveIncomeTemplateButton.setOnAction(event -> onSaveIncomeTemplate());

        clearIncomeTemplateButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearIncomeTemplateButton.setOnAction(event -> clearIncomeForm());
    }

    private Node buildExpenseSectionBody() {
        GridPane form = createFormGrid();
        addFormRow(form, 0, "Template Name", expenseNameField);
        addFormRow(form, 1, "Budget Bucket", expenseBucketCombo);
        addFormRow(form, 2, "Category", expenseCategoryCombo);
        addFormRow(form, 3, "Subcategory", expenseSubcategoryField);
        addFormRow(form, 4, "Payment Method", expensePaymentMethodCombo);
        addFormRow(form, 5, "Default Amount", expenseAmountField);
        addFormRow(form, 6, "Cadence", expenseCadenceCombo);
        addFormRow(form, 7, "Day of Month", expenseDayOfMonthField);
        addFormRow(form, 8, "Tag", expenseTagField);
        addFormRow(form, 9, "Notes", expenseNoteArea);

        Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("form-label");
        form.add(statusLabel, 0, 10);
        form.add(expenseActiveCheck, 1, 10);

        HBox actions = new HBox(10, saveExpenseTemplateButton, clearExpenseTemplateButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        expenseTemplateTable.setPrefHeight(260);
        return new VBox(12, form, actions, expenseTemplateTable);
    }

    private Node buildIncomeSectionBody() {
        GridPane form = createFormGrid();
        addFormRow(form, 0, "Source Name", incomeSourceField);
        addFormRow(form, 1, "Income Type", incomeTypeCombo);
        addFormRow(form, 2, "Default Amount", incomeAmountField);
        addFormRow(form, 3, "Cadence", incomeCadenceCombo);
        addFormRow(form, 4, "Day of Month", incomeDayOfMonthField);
        addFormRow(form, 5, "Notes", incomeNoteArea);

        Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("form-label");
        form.add(statusLabel, 0, 6);
        form.add(incomeActiveCheck, 1, 6);

        HBox actions = new HBox(10, saveIncomeTemplateButton, clearIncomeTemplateButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        incomeTemplateTable.setPrefHeight(240);
        return new VBox(12, form, actions, incomeTemplateTable);
    }

    private void onSaveExpenseTemplate() {
        clearBanner();
        try {
            ExpenseTemplate template = editingExpenseTemplateId == null
                    ? new ExpenseTemplate()
                    : findExpenseTemplateOrThrow(editingExpenseTemplateId).copy();
            template.setName(ValidationUtils.requireNonBlank(expenseNameField.getText(), "template name"));
            template.setPlannerBucket(ValidationUtils.requireNonNull(expenseBucketCombo.getValue(), "budget bucket"));
            template.setCategory(ValidationUtils.requireNonNull(expenseCategoryCombo.getValue(), "category"));
            template.setSubcategory(expenseSubcategoryField.getText());
            template.setPaymentMethod(expensePaymentMethodCombo.getValue());
            template.setDefaultAmount(parseNonNegativeAmount(expenseAmountField.getText(), "Default amount"));
            template.setCadence(ValidationUtils.requireNonNull(expenseCadenceCombo.getValue(), "cadence"));
            template.setDayOfMonth(parseDayOfMonth(expenseDayOfMonthField.getText(), "Day of month"));
            template.setTag(expenseTagField.getText());
            template.setNote(expenseNoteArea.getText());
            template.setActive(expenseActiveCheck.isSelected());

            templateService.saveExpenseTemplate(template);
            appContext.notifyContextChanged();
            String successMessage = editingExpenseTemplateId == null ? "Expense template created." : "Expense template updated.";
            if (template.getDefaultAmount().compareTo(BigDecimal.ZERO) == 0) {
                successMessage += " Default amount is 0, so rollover generation will skip this template.";
            }
            showSuccess(successMessage);
            clearExpenseForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onSaveIncomeTemplate() {
        clearBanner();
        try {
            IncomeTemplate template = editingIncomeTemplateId == null
                    ? new IncomeTemplate()
                    : findIncomeTemplateOrThrow(editingIncomeTemplateId).copy();
            template.setSourceName(ValidationUtils.requireNonBlank(incomeSourceField.getText(), "source name"));
            template.setIncomeType(ValidationUtils.requireNonNull(incomeTypeCombo.getValue(), "income type"));
            template.setDefaultAmount(parseNonNegativeAmount(incomeAmountField.getText(), "Default amount"));
            template.setCadence(ValidationUtils.requireNonNull(incomeCadenceCombo.getValue(), "cadence"));
            template.setDayOfMonth(parseDayOfMonth(incomeDayOfMonthField.getText(), "Day of month"));
            template.setNote(incomeNoteArea.getText());
            template.setActive(incomeActiveCheck.isSelected());

            templateService.saveIncomeTemplate(template);
            appContext.notifyContextChanged();
            String successMessage = editingIncomeTemplateId == null ? "Income template created." : "Income template updated.";
            if (template.getDefaultAmount().compareTo(BigDecimal.ZERO) == 0) {
                successMessage += " Default amount is 0, so rollover generation will skip this template.";
            }
            showSuccess(successMessage);
            clearIncomeForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadExpenseTemplateForEdit(ExpenseTemplate template) {
        editingExpenseTemplateId = template.getId();
        expenseNameField.setText(template.getName());
        expenseBucketCombo.getSelectionModel().select(template.getPlannerBucket());
        expenseCategoryCombo.getSelectionModel().select(template.getCategory());
        expenseSubcategoryField.setText(template.getSubcategory());
        expensePaymentMethodCombo.getSelectionModel().select(template.getPaymentMethod());
        expenseAmountField.setMoneyValue(template.getDefaultAmount());
        expenseCadenceCombo.getSelectionModel().select(template.getCadence());
        expenseDayOfMonthField.setText(String.valueOf(template.getDayOfMonth()));
        expenseTagField.setText(template.getTag());
        expenseNoteArea.setText(template.getNote());
        expenseActiveCheck.setSelected(template.isActive());

        saveExpenseTemplateButton.setText("Save Changes");
        clearExpenseTemplateButton.setText("Cancel Edit");
    }

    private void loadIncomeTemplateForEdit(IncomeTemplate template) {
        editingIncomeTemplateId = template.getId();
        incomeSourceField.setText(template.getSourceName());
        incomeTypeCombo.getSelectionModel().select(template.getIncomeType());
        incomeAmountField.setMoneyValue(template.getDefaultAmount());
        incomeCadenceCombo.getSelectionModel().select(template.getCadence());
        incomeDayOfMonthField.setText(String.valueOf(template.getDayOfMonth()));
        incomeNoteArea.setText(template.getNote());
        incomeActiveCheck.setSelected(template.isActive());

        saveIncomeTemplateButton.setText("Save Changes");
        clearIncomeTemplateButton.setText("Cancel Edit");
    }

    private void deleteExpenseTemplate(ExpenseTemplate template) {
        try {
            templateService.deleteExpenseTemplate(template.getId());
            if (template.getId().equals(editingExpenseTemplateId)) {
                clearExpenseForm();
            }
            appContext.notifyContextChanged();
            showSuccess("Expense template deleted.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteIncomeTemplate(IncomeTemplate template) {
        try {
            templateService.deleteIncomeTemplate(template.getId());
            if (template.getId().equals(editingIncomeTemplateId)) {
                clearIncomeForm();
            }
            appContext.notifyContextChanged();
            showSuccess("Income template deleted.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void clearExpenseForm() {
        editingExpenseTemplateId = null;
        expenseNameField.clear();
        expenseBucketCombo.getSelectionModel().select(PlannerBucket.DISCRETIONARY);
        expenseCategoryCombo.getSelectionModel().select(ExpenseCategory.OTHER);
        expenseSubcategoryField.clear();
        expensePaymentMethodCombo.getSelectionModel().select(PaymentMethod.CARD);
        expenseAmountField.clear();
        expenseCadenceCombo.getSelectionModel().select(RecurrenceCadence.MONTHLY);
        expenseDayOfMonthField.setText("1");
        expenseActiveCheck.setSelected(true);
        expenseTagField.clear();
        expenseNoteArea.clear();

        saveExpenseTemplateButton.setText("Create Expense Template");
        clearExpenseTemplateButton.setText("Clear Form");
    }

    private void clearIncomeForm() {
        editingIncomeTemplateId = null;
        incomeSourceField.clear();
        incomeTypeCombo.getSelectionModel().select(IncomeType.OTHER);
        incomeAmountField.clear();
        incomeCadenceCombo.getSelectionModel().select(RecurrenceCadence.MONTHLY);
        incomeDayOfMonthField.setText("1");
        incomeActiveCheck.setSelected(true);
        incomeNoteArea.clear();

        saveIncomeTemplateButton.setText("Create Income Template");
        clearIncomeTemplateButton.setText("Clear Form");
    }

    private void refreshAll() {
        List<ExpenseTemplate> expenseTemplates = templateService.listExpenseTemplates();
        List<IncomeTemplate> incomeTemplates = templateService.listIncomeTemplates();
        expenseTemplateRows.setAll(expenseTemplates);
        incomeTemplateRows.setAll(incomeTemplates);

        if (editingExpenseTemplateId != null && expenseTemplates.stream().noneMatch(t -> editingExpenseTemplateId.equals(t.getId()))) {
            clearExpenseForm();
        }
        if (editingIncomeTemplateId != null && incomeTemplates.stream().noneMatch(t -> editingIncomeTemplateId.equals(t.getId()))) {
            clearIncomeForm();
        }
    }

    private ExpenseTemplate findExpenseTemplateOrThrow(String id) {
        return templateService.listExpenseTemplates().stream()
                .filter(template -> template.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Expense template not found."));
    }

    private IncomeTemplate findIncomeTemplateOrThrow(String id) {
        return templateService.listIncomeTemplates().stream()
                .filter(template -> template.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Income template not found."));
    }

    private BigDecimal parseNonNegativeAmount(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return MoneyUtils.parse(rawValue, fieldName, true);
    }

    private int parseDayOfMonth(String rawValue, String fieldName) {
        String value = ValidationUtils.requireNonBlank(rawValue, fieldName);
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1 || parsed > 31) {
                throw new IllegalArgumentException(fieldName + " must be between 1 and 31.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number between 1 and 31.");
        }
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        return grid;
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

    private <T> ComboBox<T> combo() {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.getStyleClass().addAll("combo-box", "form-combo");
        return comboBox;
    }

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("text-input", "form-input");
        return field;
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
}
