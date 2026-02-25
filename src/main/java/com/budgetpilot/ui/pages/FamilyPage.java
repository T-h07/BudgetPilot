package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.FamilyExpenseType;
import com.budgetpilot.model.enums.RelationshipType;
import com.budgetpilot.service.FamilyExpenseSummary;
import com.budgetpilot.service.FamilyMemberSummary;
import com.budgetpilot.service.FamilyService;
import com.budgetpilot.service.FamilySummary;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FamilyPage extends VBox {
    private final AppContext appContext;
    private final FamilyService familyService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final SummaryStatCard totalFamilyCostsCard = new SummaryStatCard();
    private final SummaryStatCard allowancesCard = new SummaryStatCard();
    private final SummaryStatCard medicalCard = new SummaryStatCard();
    private final SummaryStatCard activeMembersCard = new SummaryStatCard();
    private final SummaryStatCard plannedVsActualCard = new SummaryStatCard();

    private final TextField memberNameField = textField("Member name");
    private final ComboBox<RelationshipType> relationshipCombo = new ComboBox<>();
    private final MoneyField weeklyAllowanceField = new MoneyField("Weekly Allowance", "Weekly allowance");
    private final MoneyField monthlyMedicalField = new MoneyField("Monthly Medical Budget", "Monthly medical budget");
    private final MoneyField monthlySupportField = new MoneyField("Monthly Support Budget", "Monthly support budget");
    private final CheckBox memberActiveCheck = new CheckBox("Active");
    private final TextArea memberNotesArea = new TextArea();
    private final Button saveMemberButton = new Button("Add Member");
    private final Button clearMemberButton = new Button("Clear Form");

    private final VBox memberListBox = new VBox(10);

    private final Label selectedMemberTitle = new Label("No member selected");
    private final Label selectedMemberMeta = new Label("Select a family member to manage expenses.");
    private final Label selectedMemberTotals = new Label("-");

    private final MoneyField expenseAmountField = new MoneyField("Amount", "Amount");
    private final DatePicker expenseDatePicker = new DatePicker();
    private final ComboBox<FamilyExpenseType> expenseTypeCombo = new ComboBox<>();
    private final TextField expenseNoteField = textField("Note");
    private final Button saveExpenseButton = new Button("Add Family Expense");
    private final Button clearExpenseButton = new Button("Clear");

    private final ObservableList<FamilyExpenseEntry> expenseRows = FXCollections.observableArrayList();
    private final TableView<FamilyExpenseEntry> expenseTable = new TableView<>(expenseRows);

    private final VBox breakdownBox = new VBox(8);
    private final VBox budgetCompareBox = new VBox(8);

    private List<FamilyMember> members = List.of();
    private Map<String, FamilyMemberSummary> memberSummaryMap = new HashMap<>();
    private String selectedMemberId;
    private String editingMemberId;
    private String editingExpenseId;

    public FamilyPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.familyService = new FamilyService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-family");

        getChildren().add(UiUtils.createPageHeader(
                "Family",
                "Manage dependents, support costs, and monthly family budget control."
        ));

        setupBanner();
        setupMemberForm();
        setupExpenseForm();
        setupExpenseTable();
        setupActions();

        HBox summaryRow = buildSummaryRow();
        HBox mainRow = buildMainRow();
        HBox bottomRow = buildBottomRow();

        getChildren().addAll(summaryRow, bannerLabel, mainRow, bottomRow);

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

    private void setupMemberForm() {
        relationshipCombo.getItems().setAll(RelationshipType.values());
        relationshipCombo.getSelectionModel().select(RelationshipType.OTHER);
        relationshipCombo.getStyleClass().addAll("combo-box", "form-combo");

        memberActiveCheck.setSelected(true);
        memberNotesArea.setPromptText("Optional notes");
        memberNotesArea.setPrefRowCount(3);
        memberNotesArea.getStyleClass().addAll("text-area", "form-textarea");

        memberListBox.getStyleClass().add("family-member-list");
    }

    private void setupExpenseForm() {
        selectedMemberTitle.getStyleClass().add("card-title");
        selectedMemberMeta.getStyleClass().add("muted-text");
        selectedMemberTotals.getStyleClass().add("kpi-value");

        expenseDatePicker.setValue(defaultDateForSelectedMonth());
        expenseDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");

        expenseTypeCombo.getItems().setAll(FamilyExpenseType.values());
        expenseTypeCombo.getSelectionModel().select(FamilyExpenseType.SUPPORT);
        expenseTypeCombo.getStyleClass().addAll("combo-box", "form-combo");
    }

    private void setupExpenseTable() {
        expenseTable.getStyleClass().add("family-expense-table");
        expenseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        expenseTable.setPlaceholder(new DataEmptyState(
                "No family expenses yet",
                "Add family expenses to track real support costs."
        ));

        TableColumn<FamilyExpenseEntry, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getExpenseDate() == null ? "" : data.getValue().getExpenseDate().toString()
        ));

        TableColumn<FamilyExpenseEntry, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExpenseType().getLabel()));

        TableColumn<FamilyExpenseEntry, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyUtils.format(data.getValue().getAmount(), resolveCurrencyCode())
        ));

        TableColumn<FamilyExpenseEntry, String> noteColumn = new TableColumn<>("Note");
        noteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

        TableColumn<FamilyExpenseEntry, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actions = new HBox(6, editButton, deleteButton);

            {
                editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
                deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
                actions.setAlignment(Pos.CENTER_LEFT);
                editButton.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    loadExpenseForEdit(getTableView().getItems().get(rowIndex));
                });
                deleteButton.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    onDeleteExpense(getTableView().getItems().get(rowIndex));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });

        expenseTable.getColumns().setAll(dateColumn, typeColumn, amountColumn, noteColumn, actionsColumn);
    }

    private void setupActions() {
        saveMemberButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveMemberButton.setOnAction(event -> onSaveMember());
        clearMemberButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearMemberButton.setOnAction(event -> clearMemberForm());

        saveExpenseButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveExpenseButton.setOnAction(event -> onSaveExpense());
        clearExpenseButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearExpenseButton.setOnAction(event -> clearExpenseForm());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                totalFamilyCostsCard,
                allowancesCard,
                medicalCard,
                activeMembersCard,
                plannedVsActualCard
        );
        row.getStyleClass().add("family-summary-grid");
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
                "Family Members",
                "Create and manage family profiles with recurring support expectations.",
                buildMembersBody()
        );

        SectionCard rightCard = new SectionCard(
                "Selected Member",
                "Track family expenses by member and keep support costs transparent.",
                buildSelectedMemberBody()
        );

        leftCard.getStyleClass().add("family-members-card");
        rightCard.getStyleClass().add("family-detail-card");

        HBox row = new HBox(UiUtils.CARD_GAP, leftCard, rightCard);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);
        rightCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private HBox buildBottomRow() {
        SectionCard breakdownCard = new SectionCard(
                "Monthly Family Breakdown",
                "Family costs grouped by allowance, medical, support, and emergency types.",
                breakdownBox
        );

        SectionCard compareCard = new SectionCard(
                "Planner Family Budget Comparison",
                "Compare actual family spending to planned monthly family budget.",
                budgetCompareBox
        );
        compareCard.getStyleClass().add("family-budget-compare-card");

        HBox row = new HBox(UiUtils.CARD_GAP, breakdownCard, compareCard);
        HBox.setHgrow(breakdownCard, Priority.ALWAYS);
        HBox.setHgrow(compareCard, Priority.ALWAYS);
        breakdownCard.setMaxWidth(Double.MAX_VALUE);
        compareCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildMembersBody() {
        VBox section = new VBox(12, buildMemberForm(), new Label("Members"), memberListBox);
        section.getChildren().get(1).getStyleClass().add("form-label");
        return section;
    }

    private Node buildMemberForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Name", memberNameField);
        addFormRow(grid, 1, "Relationship", relationshipCombo);
        addFormRow(grid, 2, "Weekly Allowance", weeklyAllowanceField);
        addFormRow(grid, 3, "Monthly Medical Budget", monthlyMedicalField);
        addFormRow(grid, 4, "Monthly Support Budget", monthlySupportField);

        Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("form-label");
        grid.add(statusLabel, 0, 5);
        grid.add(memberActiveCheck, 1, 5);

        addFormRow(grid, 6, "Notes", memberNotesArea);

        HBox actions = new HBox(10, saveMemberButton, clearMemberButton);
        actions.setPadding(new Insets(8, 0, 0, 0));
        return new VBox(10, grid, actions);
    }

    private Node buildSelectedMemberBody() {
        VBox section = new VBox(14,
                new VBox(6, selectedMemberTitle, selectedMemberMeta, selectedMemberTotals),
                buildExpenseForm(),
                expenseTable
        );
        expenseTable.setPrefHeight(300);
        return section;
    }

    private Node buildExpenseForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Amount", expenseAmountField);
        addFormRow(grid, 1, "Date", expenseDatePicker);
        addFormRow(grid, 2, "Expense Type", expenseTypeCombo);
        addFormRow(grid, 3, "Note", expenseNoteField);

        HBox actions = new HBox(10, saveExpenseButton, clearExpenseButton);
        actions.setPadding(new Insets(6, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(10, new Label("Family Expense"), grid, actions);
        form.getChildren().get(0).getStyleClass().add("form-label");
        form.getStyleClass().add("form-card");
        return form;
    }
    private void onSaveMember() {
        clearBanner();
        try {
            FamilyMember target = editingMemberId == null
                    ? new FamilyMember()
                    : findMemberOrThrow(editingMemberId).copy();
            target.setName(ValidationUtils.requireNonBlank(memberNameField.getText(), "Member name"));
            target.setRelationshipType(ValidationUtils.requireNonNull(relationshipCombo.getValue(), "Relationship"));
            target.setWeeklyAllowance(weeklyAllowanceField.parseNonNegativeOrZero());
            target.setMonthlyMedicalBudget(monthlyMedicalField.parseNonNegativeOrZero());
            target.setMonthlySupportBudget(monthlySupportField.parseNonNegativeOrZero());
            target.setActive(memberActiveCheck.isSelected());
            target.setNotes(memberNotesArea.getText());

            familyService.saveMember(target);
            selectedMemberId = target.getId();
            appContext.notifyContextChanged();
            showSuccess(editingMemberId == null ? "Family member added." : "Family member updated.");
            clearMemberForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onSaveExpense() {
        clearBanner();
        try {
            if (selectedMemberId == null) {
                throw new IllegalArgumentException("Select a family member first.");
            }
            LocalDate date = ValidationUtils.requireNonNull(expenseDatePicker.getValue(), "Date");
            if (!YearMonth.from(date).equals(appContext.getSelectedMonth())) {
                throw new IllegalArgumentException("Expense date must be within " + appContext.getCurrentMonthDisplayText() + ".");
            }

            FamilyExpenseEntry target = editingExpenseId == null
                    ? new FamilyExpenseEntry()
                    : findCurrentMemberExpenseOrThrow(editingExpenseId).copy();
            target.setFamilyMemberId(selectedMemberId);
            target.setMonth(appContext.getSelectedMonth());
            target.setExpenseDate(date);
            target.setAmount(expenseAmountField.parseRequiredPositive());
            target.setExpenseType(ValidationUtils.requireNonNull(expenseTypeCombo.getValue(), "Expense type"));
            target.setNote(expenseNoteField.getText());

            familyService.saveFamilyExpense(target);
            appContext.notifyContextChanged();
            showSuccess(editingExpenseId == null ? "Family expense added." : "Family expense updated.");
            clearExpenseForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteMember(FamilyMember member) {
        if (!confirm("Delete Family Member",
                "Delete " + member.getName() + " and all related family expenses?")) {
            return;
        }
        familyService.deleteMember(member.getId());
        if (member.getId().equals(selectedMemberId)) {
            selectedMemberId = null;
        }
        if (member.getId().equals(editingMemberId)) {
            clearMemberForm();
        }
        appContext.notifyContextChanged();
        showSuccess("Family member deleted.");
        refreshAll();
    }

    private void onDeleteExpense(FamilyExpenseEntry entry) {
        if (!confirm("Delete Family Expense",
                "Delete " + MoneyUtils.format(entry.getAmount(), resolveCurrencyCode())
                        + " from " + entry.getExpenseDate() + "?")) {
            return;
        }
        familyService.deleteFamilyExpense(entry.getId());
        if (entry.getId().equals(editingExpenseId)) {
            clearExpenseForm();
        }
        appContext.notifyContextChanged();
        showSuccess("Family expense deleted.");
        refreshAll();
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        FamilySummary familySummary = familyService.getFamilySummary(month);
        members = familyService.listMembers();
        memberSummaryMap = new HashMap<>();
        for (FamilyMemberSummary summary : familyService.getMemberSummaries(month)) {
            memberSummaryMap.put(summary.getMemberId(), summary);
        }

        if (selectedMemberId == null && !members.isEmpty()) {
            selectedMemberId = members.get(0).getId();
        }
        if (selectedMemberId != null && members.stream().noneMatch(member -> member.getId().equals(selectedMemberId))) {
            selectedMemberId = members.isEmpty() ? null : members.get(0).getId();
        }
        if (editingMemberId != null && members.stream().noneMatch(member -> member.getId().equals(editingMemberId))) {
            clearMemberForm();
        }
        if (expenseDatePicker.getValue() == null
                || !YearMonth.from(expenseDatePicker.getValue()).equals(month)) {
            expenseDatePicker.setValue(defaultDateForSelectedMonth());
        }

        updateSummaryCards(familySummary);
        refreshMemberList();
        refreshSelectedMemberSection(month);
        refreshBreakdownPanel(familySummary);
        refreshBudgetComparisonPanel(familySummary);
    }

    private void updateSummaryCards(FamilySummary summary) {
        String currencyCode = resolveCurrencyCode();
        totalFamilyCostsCard.setValues(
                "Total Family Costs",
                MoneyUtils.format(summary.getTotalFamilyCosts(), currencyCode),
                appContext.getCurrentMonthDisplayText()
        );
        allowancesCard.setValues(
                "Allowances",
                MoneyUtils.format(summary.getTotalAllowances(), currencyCode),
                "Allowance transfers this month"
        );
        medicalCard.setValues(
                "Medical Costs",
                MoneyUtils.format(summary.getTotalMedicalCosts(), currencyCode),
                "Health-related family spending"
        );
        activeMembersCard.setValues(
                "Active Family Members",
                String.valueOf(summary.getActiveMembersCount()),
                summary.getMemberCount() + " total members"
        );

        BigDecimal variance = summary.getFamilyBudgetVariance();
        String varianceText = (variance.compareTo(BigDecimal.ZERO) > 0 ? "+" : "")
                + MoneyUtils.format(variance, currencyCode);
        plannedVsActualCard.setValues(
                "Planned vs Actual",
                MoneyUtils.format(summary.getPlannedFamilyBudget(), currencyCode),
                "Actual " + MoneyUtils.format(summary.getTotalFamilyCosts(), currencyCode) + " | Variance " + varianceText
        );
    }

    private void refreshMemberList() {
        memberListBox.getChildren().clear();
        if (members.isEmpty()) {
            memberListBox.getChildren().add(new DataEmptyState(
                    "No family members",
                    "Add your first family member to start tracking support and costs."
            ));
            return;
        }
        for (FamilyMember member : members) {
            memberListBox.getChildren().add(createMemberCard(member));
        }
    }

    private Node createMemberCard(FamilyMember member) {
        FamilyMemberSummary summary = memberSummaryMap.get(member.getId());

        VBox card = new VBox(8);
        card.getStyleClass().add("family-member-card");
        if (member.getId().equals(selectedMemberId)) {
            card.getStyleClass().add("family-member-card-selected");
        }

        Label name = new Label(member.getName());
        name.getStyleClass().add("card-title");

        String metaText = member.getRelationshipType().getLabel()
                + " | Weekly " + MoneyUtils.format(member.getWeeklyAllowance(), resolveCurrencyCode())
                + " | Support " + MoneyUtils.format(member.getMonthlySupportBudget(), resolveCurrencyCode());
        Label meta = UiUtils.createMutedLabel(metaText);

        BigDecimal spent = summary == null ? BigDecimal.ZERO.setScale(2) : summary.getMonthlyTotal();
        Label spentLabel = UiUtils.createMutedLabel("Spent this month: " + MoneyUtils.format(spent, resolveCurrencyCode()));

        Button selectButton = new Button("Select");
        selectButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        selectButton.setOnAction(event -> {
            selectedMemberId = member.getId();
            refreshAll();
        });
        Button editButton = new Button("Edit");
        editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        editButton.setOnAction(event -> loadMemberForEdit(member));
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
        deleteButton.setOnAction(event -> onDeleteMember(member));

        HBox actions = new HBox(8, selectButton, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(name, meta, spentLabel, actions);
        return card;
    }

    private void refreshSelectedMemberSection(YearMonth month) {
        if (selectedMemberId == null) {
            selectedMemberTitle.setText("No member selected");
            selectedMemberMeta.setText("Select a family member to manage expenses.");
            selectedMemberTotals.setText("-");
            expenseRows.clear();
            setExpenseControlsEnabled(false);
            return;
        }

        FamilyMember member = findMemberOrThrow(selectedMemberId);
        FamilyMemberSummary summary = memberSummaryMap.get(selectedMemberId);

        selectedMemberTitle.setText(member.getName() + " (" + member.getRelationshipType().getLabel() + ")");
        selectedMemberMeta.setText(
                "Weekly allowance " + MoneyUtils.format(member.getWeeklyAllowance(), resolveCurrencyCode())
                        + " | Medical budget " + MoneyUtils.format(member.getMonthlyMedicalBudget(), resolveCurrencyCode())
                        + " | Support budget " + MoneyUtils.format(member.getMonthlySupportBudget(), resolveCurrencyCode())
        );

        if (summary == null) {
            selectedMemberTotals.setText(MoneyUtils.format(BigDecimal.ZERO, resolveCurrencyCode()));
        } else {
            selectedMemberTotals.setText(
                    "This month " + MoneyUtils.format(summary.getMonthlyTotal(), resolveCurrencyCode())
                            + " (" + summary.getExpenseCount() + " entries)"
            );
        }

        expenseRows.setAll(familyService.listFamilyExpensesForMember(selectedMemberId, month));
        setExpenseControlsEnabled(true);
    }

    private void refreshBreakdownPanel(FamilySummary summary) {
        breakdownBox.getChildren().clear();
        if (summary.getExpenseBreakdown().isEmpty()) {
            breakdownBox.getChildren().add(UiUtils.createMutedLabel("No family expense data for this month."));
            return;
        }
        for (FamilyExpenseSummary typeSummary : summary.getExpenseBreakdown()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label title = UiUtils.createMutedLabel(typeSummary.getExpenseType().getLabel()
                    + " (" + typeSummary.getEntryCount() + ")");
            Label value = new Label(MoneyUtils.format(typeSummary.getTotal(), resolveCurrencyCode()));
            value.getStyleClass().add("info-row-value");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(title, spacer, value);
            breakdownBox.getChildren().add(row);
        }
    }

    private void refreshBudgetComparisonPanel(FamilySummary summary) {
        budgetCompareBox.getChildren().clear();

        budgetCompareBox.getChildren().addAll(
                metricRow("Planned Family Budget", MoneyUtils.format(summary.getPlannedFamilyBudget(), resolveCurrencyCode())),
                metricRow("Actual Family Costs", MoneyUtils.format(summary.getTotalFamilyCosts(), resolveCurrencyCode())),
                metricRow("Variance (Actual - Planned)", MoneyUtils.format(summary.getFamilyBudgetVariance(), resolveCurrencyCode()))
        );

        Label status = new Label(summary.getBudgetStatusMessage());
        status.getStyleClass().add("muted-text");
        status.getStyleClass().removeAll("variance-good", "variance-warn", "variance-danger");
        if (summary.getPlannedFamilyBudget().compareTo(BigDecimal.ZERO) <= 0) {
            status.getStyleClass().add("variance-warn");
        } else if (summary.getFamilyBudgetVariance().compareTo(BigDecimal.ZERO) > 0) {
            status.getStyleClass().add("variance-danger");
        } else {
            status.getStyleClass().add("variance-good");
        }
        budgetCompareBox.getChildren().add(status);
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
    private void loadMemberForEdit(FamilyMember member) {
        editingMemberId = member.getId();
        memberNameField.setText(member.getName());
        relationshipCombo.getSelectionModel().select(member.getRelationshipType());
        weeklyAllowanceField.setMoneyValue(member.getWeeklyAllowance());
        monthlyMedicalField.setMoneyValue(member.getMonthlyMedicalBudget());
        monthlySupportField.setMoneyValue(member.getMonthlySupportBudget());
        memberActiveCheck.setSelected(member.isActive());
        memberNotesArea.setText(member.getNotes());

        saveMemberButton.setText("Save Changes");
        clearMemberButton.setText("Cancel Edit");
    }

    private void loadExpenseForEdit(FamilyExpenseEntry entry) {
        editingExpenseId = entry.getId();
        expenseAmountField.setMoneyValue(entry.getAmount());
        expenseDatePicker.setValue(entry.getExpenseDate());
        expenseTypeCombo.getSelectionModel().select(entry.getExpenseType());
        expenseNoteField.setText(entry.getNote());
        saveExpenseButton.setText("Save Changes");
        clearExpenseButton.setText("Cancel Edit");
    }

    private void clearMemberForm() {
        editingMemberId = null;
        memberNameField.clear();
        relationshipCombo.getSelectionModel().select(RelationshipType.OTHER);
        weeklyAllowanceField.clear();
        monthlyMedicalField.clear();
        monthlySupportField.clear();
        memberActiveCheck.setSelected(true);
        memberNotesArea.clear();

        saveMemberButton.setText("Add Member");
        clearMemberButton.setText("Clear Form");
    }

    private void clearExpenseForm() {
        editingExpenseId = null;
        expenseAmountField.clear();
        expenseDatePicker.setValue(defaultDateForSelectedMonth());
        expenseTypeCombo.getSelectionModel().select(FamilyExpenseType.SUPPORT);
        expenseNoteField.clear();
        saveExpenseButton.setText("Add Family Expense");
        clearExpenseButton.setText("Clear");
    }

    private void setExpenseControlsEnabled(boolean enabled) {
        expenseAmountField.setDisable(!enabled);
        expenseDatePicker.setDisable(!enabled);
        expenseTypeCombo.setDisable(!enabled);
        expenseNoteField.setDisable(!enabled);
        saveExpenseButton.setDisable(!enabled);
        clearExpenseButton.setDisable(!enabled);
        expenseTable.setDisable(!enabled);
    }

    private FamilyMember findMemberOrThrow(String memberId) {
        return members.stream()
                .filter(member -> member.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Family member not found."));
    }

    private FamilyExpenseEntry findCurrentMemberExpenseOrThrow(String expenseId) {
        return expenseRows.stream()
                .filter(entry -> entry.getId().equals(expenseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Family expense entry not found."));
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
