package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.Investment;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.InvestmentKind;
import com.budgetpilot.model.enums.InvestmentStatus;
import com.budgetpilot.model.enums.InvestmentTransactionType;
import com.budgetpilot.model.enums.InvestmentType;
import com.budgetpilot.service.investments.InvestmentPageSummary;
import com.budgetpilot.service.investments.InvestmentPositionSummary;
import com.budgetpilot.service.investments.InvestmentService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InvestmentsPage extends VBox {
    private final AppContext appContext;
    private final InvestmentService investmentService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final SummaryStatCard totalInvestedCard = new SummaryStatCard();
    private final SummaryStatCard estimatedValueCard = new SummaryStatCard();
    private final SummaryStatCard netProfitCard = new SummaryStatCard();
    private final SummaryStatCard monthlyContributionCard = new SummaryStatCard();
    private final SummaryStatCard activeCountCard = new SummaryStatCard();
    private final SummaryStatCard avgRoiCard = new SummaryStatCard();

    private final TextField nameField = textField("Investment name");
    private final ComboBox<InvestmentType> typeCombo = new ComboBox<>();
    private final ComboBox<InvestmentKind> kindCombo = new ComboBox<>();
    private final ComboBox<InvestmentStatus> statusCombo = new ComboBox<>();
    private final MoneyField targetAmountField = new MoneyField("Target Amount", "Target amount (optional)");
    private final MoneyField estimatedValueField = new MoneyField("Estimated Value", "Current estimated value");
    private final MoneyField expectedProfitAmountField = new MoneyField("Expected Profit Amount", "Optional");
    private final TextField expectedProfitPercentField = textField("Expected profit % (optional)");
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker expectedReturnDatePicker = new DatePicker();
    private final ComboBox<Integer> priorityCombo = new ComboBox<>();
    private final CheckBox activeCheck = new CheckBox("Active");
    private final TextArea notesArea = new TextArea();
    private final Button saveInvestmentButton = new Button("Add Investment");
    private final Button clearInvestmentButton = new Button("Clear Form");

    private final VBox investmentsListBox = new VBox(10);

    private final Label selectedInvestmentTitle = new Label("No investment selected");
    private final Label selectedInvestmentStats = new Label("-");
    private final Label selectedInvestmentMeta = new Label("Select an investment to view details.");
    private final ProgressBar selectedInvestmentProgress = new ProgressBar(0);

    private final MoneyField transactionAmountField = new MoneyField("Amount", "Amount");
    private final DatePicker transactionDatePicker = new DatePicker();
    private final ComboBox<InvestmentTransactionType> transactionTypeCombo = new ComboBox<>();
    private final TextField transactionNoteField = textField("Note");
    private final Button addTransactionButton = new Button("Add Transaction");
    private final Button clearTransactionButton = new Button("Clear");

    private final ObservableList<InvestmentTransaction> transactionRows = FXCollections.observableArrayList();
    private final TableView<InvestmentTransaction> transactionTable = new TableView<>(transactionRows);

    private List<Investment> investments = List.of();
    private Map<String, InvestmentPositionSummary> positionById = new HashMap<>();
    private String selectedInvestmentId;
    private String editingInvestmentId;

    public InvestmentsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.investmentService = new InvestmentService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-investments");

        getChildren().add(UiUtils.createPageHeader(
                "Investments",
                "Track capital allocations, returns, and portfolio growth with ROI and monthly activity visibility."
        ));

        setupBanner();
        setupInvestmentForm();
        setupTransactionForm();
        setupTransactionTable();
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

    private void setupInvestmentForm() {
        typeCombo.getItems().setAll(InvestmentType.values());
        typeCombo.getSelectionModel().select(InvestmentType.OTHER);
        typeCombo.getStyleClass().addAll("combo-box", "form-combo");

        kindCombo.getItems().setAll(InvestmentKind.values());
        kindCombo.getSelectionModel().select(InvestmentKind.MONEY);
        kindCombo.getStyleClass().addAll("combo-box", "form-combo");

        statusCombo.getItems().setAll(InvestmentStatus.values());
        statusCombo.getSelectionModel().select(InvestmentStatus.PLANNED);
        statusCombo.getStyleClass().addAll("combo-box", "form-combo");

        priorityCombo.getItems().setAll(1, 2, 3, 4, 5);
        priorityCombo.getSelectionModel().select(Integer.valueOf(3));
        priorityCombo.getStyleClass().addAll("combo-box", "form-combo");

        startDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");
        startDatePicker.setValue(defaultDateForSelectedMonth());

        expectedReturnDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");
        expectedReturnDatePicker.setPromptText("Optional");

        activeCheck.setSelected(true);

        notesArea.setPromptText("Optional notes");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().addAll("text-area", "form-textarea");

        investmentsListBox.getStyleClass().add("investment-list");
    }

    private void setupTransactionForm() {
        selectedInvestmentTitle.getStyleClass().add("card-title");
        selectedInvestmentStats.getStyleClass().add("kpi-value");
        selectedInvestmentMeta.getStyleClass().add("muted-text");
        selectedInvestmentProgress.getStyleClass().add("investment-progress-bar");

        transactionDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");
        transactionDatePicker.setValue(defaultDateForSelectedMonth());

        transactionTypeCombo.getItems().setAll(InvestmentTransactionType.values());
        transactionTypeCombo.getSelectionModel().select(InvestmentTransactionType.CONTRIBUTION);
        transactionTypeCombo.getStyleClass().addAll("combo-box", "form-combo");
    }

    private void setupTransactionTable() {
        transactionTable.getStyleClass().add("investment-transaction-table");
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.setPlaceholder(new DataEmptyState(
                "No transactions yet",
                "Add contributions, returns, or withdrawals to track this investment."
        ));

        TableColumn<InvestmentTransaction, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTransactionDate() == null ? "" : data.getValue().getTransactionDate().toString()
        ));

        TableColumn<InvestmentTransaction, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().getLabel()));

        TableColumn<InvestmentTransaction, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(formatSignedTransaction(data.getValue())));
        amountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (empty) {
                    getStyleClass().removeAll("status-good", "status-danger");
                    return;
                }
                int rowIndex = getIndex();
                if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                    getStyleClass().removeAll("status-good", "status-danger");
                    return;
                }
                InvestmentTransaction tx = getTableView().getItems().get(rowIndex);
                getStyleClass().removeAll("status-good", "status-danger");
                if (isPositiveTransaction(tx)) {
                    getStyleClass().add("status-good");
                } else {
                    getStyleClass().add("status-danger");
                }
            }
        });

        TableColumn<InvestmentTransaction, String> noteColumn = new TableColumn<>("Note");
        noteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

        TableColumn<InvestmentTransaction, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
                deleteButton.setOnAction(event -> {
                    int rowIndex = getIndex();
                    if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                        return;
                    }
                    onDeleteTransaction(getTableView().getItems().get(rowIndex));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });

        transactionTable.getColumns().setAll(dateColumn, typeColumn, amountColumn, noteColumn, actionsColumn);
    }

    private void setupActions() {
        saveInvestmentButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveInvestmentButton.setOnAction(event -> onSaveInvestment());
        clearInvestmentButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearInvestmentButton.setOnAction(event -> clearInvestmentForm());

        addTransactionButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        addTransactionButton.setOnAction(event -> onAddTransaction());
        clearTransactionButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearTransactionButton.setOnAction(event -> clearTransactionForm());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                totalInvestedCard,
                estimatedValueCard,
                netProfitCard,
                monthlyContributionCard,
                activeCountCard,
                avgRoiCard
        );
        row.getStyleClass().add("investment-summary-grid");
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
                "Investments",
                "Create and manage portfolio positions with status, targets, and expected returns.",
                buildInvestmentsBody()
        );

        SectionCard rightCard = new SectionCard(
                "Position Details",
                "Review ROI, add transactions, and inspect activity history for the selected investment.",
                buildDetailsBody()
        );

        leftCard.getStyleClass().add("investment-list-card");
        rightCard.getStyleClass().add("investment-details-card");

        HBox row = new HBox(UiUtils.CARD_GAP, leftCard, rightCard);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);
        rightCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildInvestmentsBody() {
        VBox section = new VBox(12, buildInvestmentForm(), new Label("Portfolio"), investmentsListBox);
        section.getChildren().get(1).getStyleClass().add("form-label");
        return section;
    }

    private Node buildInvestmentForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Name", nameField);
        addFormRow(grid, 1, "Type", typeCombo);
        addFormRow(grid, 2, "Kind", kindCombo);
        addFormRow(grid, 3, "Status", statusCombo);
        addFormRow(grid, 4, "Target Amount", targetAmountField);
        addFormRow(grid, 5, "Estimated Value", estimatedValueField);
        addFormRow(grid, 6, "Expected Profit Amount", expectedProfitAmountField);
        addFormRow(grid, 7, "Expected Profit %", expectedProfitPercentField);
        addFormRow(grid, 8, "Start Date", startDatePicker);
        addFormRow(grid, 9, "Expected Return Date", expectedReturnDatePicker);
        addFormRow(grid, 10, "Priority", priorityCombo);

        Label statusLabel = new Label("Active");
        statusLabel.getStyleClass().add("form-label");
        grid.add(statusLabel, 0, 11);
        grid.add(activeCheck, 1, 11);

        addFormRow(grid, 12, "Notes", notesArea);

        HBox actions = new HBox(10, saveInvestmentButton, clearInvestmentButton);
        actions.setPadding(new Insets(8, 0, 0, 0));

        return new VBox(10, grid, actions);
    }

    private Node buildDetailsBody() {
        VBox section = new VBox(
                14,
                new VBox(8, selectedInvestmentTitle, selectedInvestmentStats, selectedInvestmentMeta, selectedInvestmentProgress),
                buildTransactionForm(),
                transactionTable
        );
        transactionTable.setPrefHeight(360);
        return section;
    }

    private Node buildTransactionForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Amount", transactionAmountField);
        addFormRow(grid, 1, "Date", transactionDatePicker);
        addFormRow(grid, 2, "Type", transactionTypeCombo);
        addFormRow(grid, 3, "Note", transactionNoteField);

        HBox actions = new HBox(10, addTransactionButton, clearTransactionButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(6, 0, 0, 0));

        VBox card = new VBox(10, new Label("Transaction"), grid, actions);
        card.getStyleClass().add("form-card");
        card.getChildren().get(0).getStyleClass().add("form-label");
        return card;
    }
    private void onSaveInvestment() {
        clearBanner();
        try {
            Investment target = editingInvestmentId == null
                    ? new Investment()
                    : findInvestmentOrThrow(editingInvestmentId).copy();

            target.setName(ValidationUtils.requireNonBlank(nameField.getText(), "Investment name"));
            target.setType(ValidationUtils.requireNonNull(typeCombo.getValue(), "Investment type"));
            target.setKind(ValidationUtils.requireNonNull(kindCombo.getValue(), "Investment kind"));
            target.setStatus(ValidationUtils.requireNonNull(statusCombo.getValue(), "Investment status"));
            target.setTargetAmount(parseOptionalAmount(targetAmountField.getText(), "Target amount"));
            target.setCurrentEstimatedValue(estimatedValueField.parseNonNegativeOrZero());
            target.setExpectedProfitAmount(parseOptionalAmount(expectedProfitAmountField.getText(), "Expected profit amount"));
            target.setExpectedProfitPercent(parseOptionalAmount(expectedProfitPercentField.getText(), "Expected profit percent"));
            target.setStartDate(ValidationUtils.requireNonNull(startDatePicker.getValue(), "Start date"));
            target.setExpectedReturnDate(expectedReturnDatePicker.getValue());
            target.setPriority(ValidationUtils.requireNonNull(priorityCombo.getValue(), "Priority"));
            target.setActive(activeCheck.isSelected());
            target.setNotes(notesArea.getText());

            investmentService.saveInvestment(target);
            selectedInvestmentId = target.getId();
            appContext.notifyContextChanged();
            showSuccess(editingInvestmentId == null ? "Investment created." : "Investment updated.");
            clearInvestmentForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onAddTransaction() {
        clearBanner();
        try {
            if (selectedInvestmentId == null) {
                throw new IllegalArgumentException("Select an investment first.");
            }

            LocalDate txDate = ValidationUtils.requireNonNull(transactionDatePicker.getValue(), "Date");
            if (!YearMonth.from(txDate).equals(appContext.getSelectedMonth())) {
                throw new IllegalArgumentException("Transaction date must be within " + appContext.getCurrentMonthDisplayText() + ".");
            }

            InvestmentTransactionType txType = ValidationUtils.requireNonNull(transactionTypeCombo.getValue(), "Transaction type");
            BigDecimal amount = txType == InvestmentTransactionType.ADJUSTMENT
                    ? parseSignedAmount(transactionAmountField.getText(), "Adjustment amount")
                    : transactionAmountField.parseRequiredPositive();

            switch (txType) {
                case CONTRIBUTION -> investmentService.addContribution(selectedInvestmentId, amount, txDate, transactionNoteField.getText());
                case RETURN -> investmentService.addReturn(selectedInvestmentId, amount, txDate, transactionNoteField.getText());
                case WITHDRAWAL -> investmentService.addWithdrawal(selectedInvestmentId, amount, txDate, transactionNoteField.getText());
                case FEE -> investmentService.addFee(selectedInvestmentId, amount, txDate, transactionNoteField.getText());
                case ADJUSTMENT -> investmentService.addAdjustment(selectedInvestmentId, amount, txDate, transactionNoteField.getText());
            }

            appContext.notifyContextChanged();
            showSuccess("Investment transaction recorded.");
            clearTransactionForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteInvestment(Investment investment) {
        if (!confirm("Delete Investment", "Delete investment \"" + investment.getName() + "\" and all its transactions?")) {
            return;
        }

        investmentService.deleteInvestment(investment.getId());
        if (investment.getId().equals(selectedInvestmentId)) {
            selectedInvestmentId = null;
        }
        if (investment.getId().equals(editingInvestmentId)) {
            clearInvestmentForm();
        }

        appContext.notifyContextChanged();
        showSuccess("Investment deleted.");
        refreshAll();
    }

    private void onDeleteTransaction(InvestmentTransaction tx) {
        if (!confirm("Delete Transaction", "Delete this " + tx.getType().getLabel().toLowerCase() + " transaction?")) {
            return;
        }

        try {
            investmentService.deleteTransaction(tx.getId());
            appContext.notifyContextChanged();
            showSuccess("Transaction deleted.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        investments = investmentService.listInvestments();
        positionById = investmentService.listPositionSummaries(month).stream()
                .collect(HashMap::new, (map, position) -> map.put(position.getInvestmentId(), position), HashMap::putAll);

        if (selectedInvestmentId == null && !investments.isEmpty()) {
            selectedInvestmentId = investments.get(0).getId();
        }
        if (selectedInvestmentId != null && investments.stream().noneMatch(item -> item.getId().equals(selectedInvestmentId))) {
            selectedInvestmentId = investments.isEmpty() ? null : investments.get(0).getId();
        }
        if (editingInvestmentId != null && investments.stream().noneMatch(item -> item.getId().equals(editingInvestmentId))) {
            clearInvestmentForm();
        }

        if (startDatePicker.getValue() == null || !YearMonth.from(startDatePicker.getValue()).equals(month)) {
            startDatePicker.setValue(defaultDateForSelectedMonth());
        }
        if (transactionDatePicker.getValue() == null || !YearMonth.from(transactionDatePicker.getValue()).equals(month)) {
            transactionDatePicker.setValue(defaultDateForSelectedMonth());
        }

        refreshSummaryCards(month);
        refreshInvestmentList();
        refreshSelectedInvestmentDetails(month);
    }

    private void refreshSummaryCards(YearMonth month) {
        String currencyCode = resolveCurrencyCode();
        InvestmentPageSummary summary = investmentService.getInvestmentPageSummary(month);

        totalInvestedCard.setValues(
                "Total Invested",
                MoneyUtils.format(summary.getTotalInvestedAllTime(), currencyCode),
                "All-time invested principal"
        );
        estimatedValueCard.setValues(
                "Portfolio Value",
                MoneyUtils.format(summary.getTotalEstimatedValue(), currencyCode),
                "Estimated current value"
        );
        netProfitCard.setValues(
                "Net Profit",
                MoneyUtils.format(summary.getTotalNetProfit(), currencyCode),
                summary.getTotalNetProfit().compareTo(BigDecimal.ZERO) >= 0 ? "Positive ROI trend" : "Currently below invested"
        );
        monthlyContributionCard.setValues(
                "Monthly Contributions",
                MoneyUtils.format(summary.getTotalMonthlyContributions(), currencyCode),
                "Returns " + MoneyUtils.format(summary.getTotalMonthlyReturns(), currencyCode)
        );
        activeCountCard.setValues(
                "Active Investments",
                String.valueOf(summary.getActiveInvestmentCount()),
                summary.getCompletedInvestmentCount() + " completed"
        );
        avgRoiCard.setValues(
                "Avg ROI",
                summary.getAvgRoiPercent().stripTrailingZeros().toPlainString() + "%",
                appContext.getCurrentMonthDisplayText()
        );
    }

    private void refreshInvestmentList() {
        investmentsListBox.getChildren().clear();
        if (investments.isEmpty()) {
            investmentsListBox.getChildren().add(new DataEmptyState(
                    "No investments yet",
                    "Create your first investment (e.g. Course, Business, Stocks)."
            ));
            return;
        }

        for (Investment investment : investments) {
            investmentsListBox.getChildren().add(createInvestmentCard(investment));
        }
    }

    private Node createInvestmentCard(Investment investment) {
        InvestmentPositionSummary position = positionById.get(investment.getId());

        VBox card = new VBox(8);
        card.getStyleClass().add("investment-card");
        if (investment.getId().equals(selectedInvestmentId)) {
            card.getStyleClass().add("investment-card-selected");
        }

        Label title = new Label(investment.getName());
        title.getStyleClass().add("card-title");

        Label meta = UiUtils.createMutedLabel(
                investment.getType().getLabel() + " | " + investment.getStatus().getLabel() + " | Priority " + investment.getPriority()
        );
        meta.getStyleClass().add("investment-status-badge");

        String line = position == null
                ? "No position data"
                : "Invested " + MoneyUtils.format(position.getInvestedAmountTotal(), resolveCurrencyCode())
                + " | Value " + MoneyUtils.format(position.getCurrentEstimatedValue(), resolveCurrencyCode())
                + " | ROI " + position.getRoiPercent().stripTrailingZeros().toPlainString() + "%";
        Label stats = UiUtils.createMutedLabel(line);
        if (position != null && position.getRoiPercent().compareTo(BigDecimal.ZERO) >= 0) {
            stats.getStyleClass().add("investment-roi-positive");
        } else {
            stats.getStyleClass().add("investment-roi-negative");
        }

        double progressValue = 0;
        if (position != null) {
            progressValue = clamp(position.getProgressPercent().doubleValue() / 100.0);
        }
        ProgressBar progressBar = new ProgressBar(progressValue);
        progressBar.getStyleClass().add("investment-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Button selectButton = new Button("Select");
        selectButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        selectButton.setOnAction(event -> {
            selectedInvestmentId = investment.getId();
            refreshAll();
        });
        Button editButton = new Button("Edit");
        editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        editButton.setOnAction(event -> loadInvestmentForEdit(investment));
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
        deleteButton.setOnAction(event -> onDeleteInvestment(investment));

        HBox actions = new HBox(8, selectButton, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, meta, stats, progressBar, actions);
        return card;
    }
    private void refreshSelectedInvestmentDetails(YearMonth month) {
        if (selectedInvestmentId == null) {
            selectedInvestmentTitle.setText("No investment selected");
            selectedInvestmentStats.setText("-");
            selectedInvestmentMeta.setText("Select an investment to view details.");
            selectedInvestmentProgress.setProgress(0);
            transactionRows.clear();
            setTransactionControlsEnabled(false);
            return;
        }

        Investment investment = findInvestmentOrThrow(selectedInvestmentId);
        InvestmentPositionSummary position = positionById.get(selectedInvestmentId);
        if (position == null) {
            position = investmentService.getInvestmentSummary(selectedInvestmentId, month).getPosition();
        }

        selectedInvestmentTitle.setText(investment.getName() + " (" + investment.getType().getLabel() + ")");
        selectedInvestmentStats.setText(
                MoneyUtils.format(position.getInvestedAmountTotal(), resolveCurrencyCode())
                        + " -> " + MoneyUtils.format(position.getCurrentEstimatedValue(), resolveCurrencyCode())
        );
        selectedInvestmentMeta.setText(
                "Net " + MoneyUtils.format(position.getNetProfitAmount(), resolveCurrencyCode())
                        + " | ROI " + position.getRoiPercent().stripTrailingZeros().toPlainString() + "%"
                        + " | " + position.getDaysRemainingText()
                        + " | Month +" + MoneyUtils.format(position.getMonthlyContributionTotal(), resolveCurrencyCode())
                        + " / +" + MoneyUtils.format(position.getMonthlyReturnTotal(), resolveCurrencyCode())
        );
        selectedInvestmentProgress.setProgress(clamp(position.getProgressPercent().doubleValue() / 100.0));

        transactionRows.setAll(investmentService.listTransactions(selectedInvestmentId));
        setTransactionControlsEnabled(true);
    }

    private void loadInvestmentForEdit(Investment investment) {
        editingInvestmentId = investment.getId();

        nameField.setText(investment.getName());
        typeCombo.getSelectionModel().select(investment.getType());
        kindCombo.getSelectionModel().select(investment.getKind());
        statusCombo.getSelectionModel().select(investment.getStatus());
        targetAmountField.setMoneyValue(investment.getTargetAmount());
        estimatedValueField.setMoneyValue(investment.getCurrentEstimatedValue());
        expectedProfitAmountField.setMoneyValue(investment.getExpectedProfitAmount());
        expectedProfitPercentField.setText(
                investment.getExpectedProfitPercent() == null ? "" : investment.getExpectedProfitPercent().stripTrailingZeros().toPlainString()
        );
        startDatePicker.setValue(investment.getStartDate());
        expectedReturnDatePicker.setValue(investment.getExpectedReturnDate());
        priorityCombo.getSelectionModel().select(Integer.valueOf(investment.getPriority()));
        activeCheck.setSelected(investment.isActive());
        notesArea.setText(investment.getNotes());

        saveInvestmentButton.setText("Save Changes");
        clearInvestmentButton.setText("Cancel Edit");
    }

    private void clearInvestmentForm() {
        editingInvestmentId = null;

        nameField.clear();
        typeCombo.getSelectionModel().select(InvestmentType.OTHER);
        kindCombo.getSelectionModel().select(InvestmentKind.MONEY);
        statusCombo.getSelectionModel().select(InvestmentStatus.PLANNED);
        targetAmountField.clear();
        estimatedValueField.clear();
        expectedProfitAmountField.clear();
        expectedProfitPercentField.clear();
        startDatePicker.setValue(defaultDateForSelectedMonth());
        expectedReturnDatePicker.setValue(null);
        priorityCombo.getSelectionModel().select(Integer.valueOf(3));
        activeCheck.setSelected(true);
        notesArea.clear();

        saveInvestmentButton.setText("Add Investment");
        clearInvestmentButton.setText("Clear Form");
    }

    private void clearTransactionForm() {
        transactionAmountField.clear();
        transactionDatePicker.setValue(defaultDateForSelectedMonth());
        transactionTypeCombo.getSelectionModel().select(InvestmentTransactionType.CONTRIBUTION);
        transactionNoteField.clear();
    }

    private void setTransactionControlsEnabled(boolean enabled) {
        transactionAmountField.setDisable(!enabled);
        transactionDatePicker.setDisable(!enabled);
        transactionTypeCombo.setDisable(!enabled);
        transactionNoteField.setDisable(!enabled);
        addTransactionButton.setDisable(!enabled);
        clearTransactionButton.setDisable(!enabled);
        transactionTable.setDisable(!enabled);
    }

    private Investment findInvestmentOrThrow(String id) {
        return investments.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Investment not found."));
    }

    private BigDecimal parseOptionalAmount(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return MoneyUtils.parse(rawValue, fieldName, true);
    }

    private BigDecimal parseSignedAmount(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            BigDecimal amount = MoneyUtils.normalize(new BigDecimal(rawValue.trim().replace(',', '.')));
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException(fieldName + " must not be zero.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid amount.");
        }
    }

    private String formatSignedTransaction(InvestmentTransaction tx) {
        if (tx == null) {
            return MoneyUtils.format(BigDecimal.ZERO, resolveCurrencyCode());
        }
        BigDecimal amount = MoneyUtils.zeroIfNull(tx.getAmount());
        BigDecimal magnitude = amount.abs();
        String prefix = isPositiveTransaction(tx) ? "+" : "-";
        return prefix + MoneyUtils.format(magnitude, resolveCurrencyCode());
    }

    private boolean isPositiveTransaction(InvestmentTransaction tx) {
        if (tx == null || tx.getType() == null) {
            return true;
        }
        return switch (tx.getType()) {
            case CONTRIBUTION, RETURN -> true;
            case WITHDRAWAL, FEE -> false;
            case ADJUSTMENT -> MoneyUtils.zeroIfNull(tx.getAmount()).compareTo(BigDecimal.ZERO) >= 0;
        };
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

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
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
