package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.SavingsEntryType;
import com.budgetpilot.service.planner.BudgetSummary;
import com.budgetpilot.service.planner.PlannerService;
import com.budgetpilot.service.savings.SavingsBucketSummary;
import com.budgetpilot.service.savings.SavingsService;
import com.budgetpilot.service.savings.SavingsSummary;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class SavingsPage extends VBox {
    private final AppContext appContext;
    private final SavingsService savingsService;
    private final PlannerService plannerService;
    private final Runnable contextListener = this::refreshAll;

    private final Label bannerLabel = new Label();

    private final SummaryStatCard totalSavingsCard = new SummaryStatCard();
    private final SummaryStatCard monthlyContributionCard = new SummaryStatCard();
    private final SummaryStatCard monthlyWithdrawalCard = new SummaryStatCard();
    private final SummaryStatCard activeBucketsCard = new SummaryStatCard();
    private final SummaryStatCard plannedVsActualCard = new SummaryStatCard();

    private final TextField bucketNameField = textField("Bucket name");
    private final MoneyField bucketTargetField = new MoneyField("Target Amount", "Target amount (optional)");
    private final CheckBox bucketActiveCheck = new CheckBox("Active");
    private final TextArea bucketNotesArea = new TextArea();
    private final Button saveBucketButton = new Button("Create Bucket");
    private final Button clearBucketButton = new Button("Clear Form");

    private final VBox bucketListBox = new VBox(10);

    private final Label selectedBucketTitleLabel = new Label("No bucket selected");
    private final Label selectedBucketBalanceLabel = new Label("-");
    private final Label selectedBucketMetaLabel = new Label("Select a bucket to view details.");
    private final ProgressBar selectedBucketProgress = new ProgressBar(0);

    private final MoneyField transactionAmountField = new MoneyField("Amount", "Amount");
    private final DatePicker transactionDatePicker = new DatePicker();
    private final ComboBox<SavingsEntryType> transactionTypeCombo = new ComboBox<>();
    private final TextField transactionNoteField = textField("Note (optional)");
    private final Button addTransactionButton = new Button("Add Transaction");
    private final Button clearTransactionButton = new Button("Clear");

    private final ObservableList<SavingsEntry> transactionRows = FXCollections.observableArrayList();
    private final TableView<SavingsEntry> transactionTable = new TableView<>(transactionRows);

    private List<SavingsBucket> buckets = List.of();
    private String selectedBucketId;
    private String editingBucketId;

    public SavingsPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.savingsService = new SavingsService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));
        this.plannerService = new PlannerService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().addAll("page-root", "page-savings");

        getChildren().add(UiUtils.createPageHeader(
                "Savings",
                "Build your savings vault with dedicated buckets, monthly contributions, and clear progress tracking."
        ));

        setupBanner();
        setupBucketForm();
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

    private void setupBucketForm() {
        bucketActiveCheck.setSelected(true);
        bucketNotesArea.setPromptText("Optional notes");
        bucketNotesArea.setPrefRowCount(3);
        bucketNotesArea.getStyleClass().addAll("text-area", "form-textarea");

        bucketListBox.getStyleClass().add("savings-bucket-list");
    }

    private void setupTransactionForm() {
        transactionTypeCombo.getItems().setAll(SavingsEntryType.values());
        transactionTypeCombo.getSelectionModel().select(SavingsEntryType.CONTRIBUTION);
        transactionTypeCombo.getStyleClass().addAll("combo-box", "form-combo");
        transactionDatePicker.setValue(defaultDateForSelectedMonth());
        transactionDatePicker.getStyleClass().addAll("date-picker", "form-datepicker");

        selectedBucketTitleLabel.getStyleClass().add("card-title");
        selectedBucketBalanceLabel.getStyleClass().add("kpi-value");
        selectedBucketMetaLabel.getStyleClass().add("muted-text");
        selectedBucketProgress.getStyleClass().add("bucket-progress-bar");
    }

    private void setupTransactionTable() {
        transactionTable.getStyleClass().add("savings-transaction-table");
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.setPlaceholder(new DataEmptyState(
                "No savings transactions",
                "Start by adding a contribution or adjustment for this bucket."
        ));

        TableColumn<SavingsEntry, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEntryDate() == null ? "" : data.getValue().getEntryDate().toString()
        ));

        TableColumn<SavingsEntry, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEntryType().getLabel()));

        TableColumn<SavingsEntry, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(formatSignedMoney(data.getValue().getAmount())));
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
                SavingsEntry entry = getTableView().getItems().get(rowIndex);
                getStyleClass().removeAll("status-good", "status-danger");
                if (entry.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    getStyleClass().add("status-good");
                } else {
                    getStyleClass().add("status-danger");
                }
            }
        });

        TableColumn<SavingsEntry, String> noteColumn = new TableColumn<>("Note");
        noteColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

        TableColumn<SavingsEntry, Void> actionsColumn = new TableColumn<>("Actions");
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
        saveBucketButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveBucketButton.setOnAction(event -> onSaveBucket());

        clearBucketButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearBucketButton.setOnAction(event -> clearBucketForm());

        addTransactionButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        addTransactionButton.setOnAction(event -> onAddTransaction());

        clearTransactionButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        clearTransactionButton.setOnAction(event -> clearTransactionForm());
    }

    private HBox buildSummaryRow() {
        HBox row = new HBox(
                UiUtils.CARD_GAP,
                totalSavingsCard,
                monthlyContributionCard,
                monthlyWithdrawalCard,
                activeBucketsCard,
                plannedVsActualCard
        );
        row.getStyleClass().add("savings-summary-grid");

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
                "Bucket Management",
                "Create and manage savings buckets such as Emergency Fund, Tuition, or Travel.",
                buildBucketManagerBody()
        );
        leftCard.getStyleClass().add("savings-buckets-card");

        SectionCard rightCard = new SectionCard(
                "Bucket Details",
                "View progress, add transactions, and review history for the selected bucket.",
                buildDetailsBody()
        );
        rightCard.getStyleClass().add("savings-details-card");

        HBox row = new HBox(UiUtils.CARD_GAP, leftCard, rightCard);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        leftCard.setMaxWidth(Double.MAX_VALUE);
        rightCard.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node buildBucketManagerBody() {
        VBox section = new VBox(12, buildBucketForm(), new Label("Buckets"), bucketListBox);
        section.setFillWidth(true);
        section.getChildren().get(1).getStyleClass().add("form-label");
        return section;
    }

    private Node buildBucketForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addFormRow(grid, 0, "Bucket Name", bucketNameField);
        addFormRow(grid, 1, "Target Amount", bucketTargetField);

        Label activeLabel = new Label("Status");
        activeLabel.getStyleClass().add("form-label");
        grid.add(activeLabel, 0, 2);
        grid.add(bucketActiveCheck, 1, 2);

        addFormRow(grid, 3, "Notes", bucketNotesArea);

        HBox actions = new HBox(10, saveBucketButton, clearBucketButton);
        actions.setPadding(new Insets(8, 0, 0, 0));
        return new VBox(10, grid, actions);
    }

    private Node buildDetailsBody() {
        VBox details = new VBox(14,
                buildSelectedBucketHeader(),
                buildTransactionForm(),
                buildTransactionTableSection()
        );
        details.setFillWidth(true);
        return details;
    }

    private Node buildSelectedBucketHeader() {
        VBox header = new VBox(8,
                selectedBucketTitleLabel,
                selectedBucketBalanceLabel,
                selectedBucketMetaLabel,
                selectedBucketProgress
        );
        return header;
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

    private Node buildTransactionTableSection() {
        transactionTable.setPrefHeight(360);
        return transactionTable;
    }

    private void onSaveBucket() {
        clearBanner();
        try {
            SavingsBucket target = editingBucketId == null
                    ? new SavingsBucket()
                    : findBucketOrThrow(editingBucketId).copy();

            target.setName(ValidationUtils.requireNonBlank(bucketNameField.getText(), "Bucket name"));
            target.setTargetAmount(parseOptionalTarget(bucketTargetField.getText()));
            target.setActive(bucketActiveCheck.isSelected());
            target.setNotes(bucketNotesArea.getText());

            savingsService.saveBucket(target);
            selectedBucketId = target.getId();
            appContext.notifyContextChanged();
            showSuccess(editingBucketId == null ? "Savings bucket created." : "Savings bucket updated.");
            clearBucketForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onAddTransaction() {
        clearBanner();
        try {
            if (selectedBucketId == null) {
                throw new IllegalArgumentException("Select a savings bucket first.");
            }

            LocalDate entryDate = ValidationUtils.requireNonNull(transactionDatePicker.getValue(), "Date");
            YearMonth entryMonth = YearMonth.from(entryDate);
            if (!entryMonth.equals(appContext.getSelectedMonth())) {
                throw new IllegalArgumentException("Transaction date must be within " + appContext.getCurrentMonthDisplayText() + ".");
            }

            SavingsEntryType type = ValidationUtils.requireNonNull(transactionTypeCombo.getValue(), "Type");
            BigDecimal amount = type == SavingsEntryType.ADJUSTMENT
                    ? parseSignedAmount(transactionAmountField.getText(), "Adjustment amount")
                    : transactionAmountField.parseRequiredPositive();

            switch (type) {
                case CONTRIBUTION -> savingsService.addContribution(selectedBucketId, amount, entryDate, transactionNoteField.getText());
                case WITHDRAWAL -> savingsService.withdraw(selectedBucketId, amount, entryDate, transactionNoteField.getText());
                case ADJUSTMENT -> savingsService.addAdjustment(selectedBucketId, amount, entryDate, transactionNoteField.getText());
            }

            appContext.notifyContextChanged();
            showSuccess("Savings transaction recorded.");
            clearTransactionForm();
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteBucket(SavingsBucket bucket) {
        if (!confirm(
                "Delete Bucket",
                "Delete bucket \"" + bucket.getName() + "\" and all its transactions?"
        )) {
            return;
        }

        savingsService.deleteBucket(bucket.getId());
        if (bucket.getId().equals(selectedBucketId)) {
            selectedBucketId = null;
        }
        if (bucket.getId().equals(editingBucketId)) {
            clearBucketForm();
        }

        appContext.notifyContextChanged();
        showSuccess("Savings bucket deleted.");
        refreshAll();
    }

    private void onDeleteTransaction(SavingsEntry entry) {
        if (selectedBucketId == null) {
            return;
        }

        if (!confirm(
                "Delete Transaction",
                "Delete this savings transaction from " + entry.getEntryDate() + "?"
        )) {
            return;
        }

        try {
            savingsService.deleteEntry(selectedBucketId, entry.getId());
            appContext.notifyContextChanged();
            showSuccess("Savings transaction deleted.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        YearMonth month = appContext.getSelectedMonth();
        buckets = savingsService.listBuckets();

        if (selectedBucketId == null && !buckets.isEmpty()) {
            selectedBucketId = buckets.get(0).getId();
        }

        if (selectedBucketId != null && buckets.stream().noneMatch(bucket -> bucket.getId().equals(selectedBucketId))) {
            selectedBucketId = buckets.isEmpty() ? null : buckets.get(0).getId();
        }

        if (editingBucketId != null && buckets.stream().noneMatch(bucket -> bucket.getId().equals(editingBucketId))) {
            clearBucketForm();
        }

        if (transactionDatePicker.getValue() == null
                || !YearMonth.from(transactionDatePicker.getValue()).equals(month)) {
            transactionDatePicker.setValue(defaultDateForSelectedMonth());
        }

        refreshSummaryCards(month);
        refreshBucketList();
        refreshSelectedBucketDetails(month);
    }

    private void refreshSummaryCards(YearMonth month) {
        String currencyCode = resolveCurrencyCode();
        SavingsSummary summary = savingsService.getSavingsSummary(month);

        totalSavingsCard.setValues(
                "Total Savings",
                MoneyUtils.format(summary.getTotalCurrentSavings(), currencyCode),
                summary.getBucketCount() + " buckets"
        );
        monthlyContributionCard.setValues(
                "Contributed This Month",
                MoneyUtils.format(summary.getMonthlyContributions(), currencyCode),
                month.toString()
        );
        monthlyWithdrawalCard.setValues(
                "Withdrawn This Month",
                MoneyUtils.format(summary.getMonthlyWithdrawals(), currencyCode),
                "Net " + MoneyUtils.format(summary.getMonthlyNetChange(), currencyCode)
        );
        activeBucketsCard.setValues(
                "Active Buckets",
                String.valueOf(summary.getActiveBucketCount()),
                "Target coverage " + summary.getTargetCoveragePercent().stripTrailingZeros().toPlainString() + "%"
        );

        MonthlyPlan existingPlan = appContext.getStore().getMonthlyPlan(month);
        if (existingPlan == null) {
            plannedVsActualCard.setValues(
                    "Planned vs Actual",
                    "No monthly plan",
                    "Set planner savings target for comparison"
            );
        } else {
            BudgetSummary budgetSummary = plannerService.buildBudgetSummary(month, isFamilyModuleEnabled());
            BigDecimal planned = budgetSummary.getSavingsAmountPlanned();
            BigDecimal delta = MoneyUtils.normalize(summary.getMonthlyContributions().subtract(planned));
            String deltaText = (delta.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + MoneyUtils.format(delta, currencyCode);
            plannedVsActualCard.setValues(
                    "Planned vs Actual",
                    MoneyUtils.format(summary.getMonthlyContributions(), currencyCode),
                    "Plan " + MoneyUtils.format(planned, currencyCode) + " | Delta " + deltaText
            );
        }
    }

    private void refreshBucketList() {
        bucketListBox.getChildren().clear();
        if (buckets.isEmpty()) {
            bucketListBox.getChildren().add(new DataEmptyState(
                    "No savings buckets",
                    "Create your first savings bucket (e.g., Emergency Fund)."
            ));
            return;
        }

        for (SavingsBucket bucket : buckets) {
            bucketListBox.getChildren().add(createBucketCard(bucket));
        }
    }

    private Node createBucketCard(SavingsBucket bucket) {
        VBox card = new VBox(8);
        card.getStyleClass().add("bucket-card");
        if (bucket.getId().equals(selectedBucketId)) {
            card.getStyleClass().add("bucket-card-selected");
        }

        Label title = new Label(bucket.getName());
        title.getStyleClass().add("card-title");

        BigDecimal target = bucket.getTargetAmount();
        String targetText = target == null || target.compareTo(BigDecimal.ZERO) <= 0
                ? "No target"
                : MoneyUtils.format(target, resolveCurrencyCode());

        Label summary = UiUtils.createMutedLabel(
                MoneyUtils.format(bucket.getCurrentAmount(), resolveCurrencyCode()) + " / " + targetText
                        + " | " + (bucket.isActive() ? "Active" : "Inactive")
        );

        ProgressBar progressBar = new ProgressBar(calculateProgress(bucket.getCurrentAmount(), bucket.getTargetAmount()));
        progressBar.getStyleClass().add("bucket-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Button selectButton = new Button("Select");
        selectButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        selectButton.setOnAction(event -> {
            selectedBucketId = bucket.getId();
            refreshAll();
        });

        Button editButton = new Button("Edit");
        editButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
        editButton.setOnAction(event -> loadBucketForEdit(bucket));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("danger-button", "btn-danger", "btn-small");
        deleteButton.setOnAction(event -> onDeleteBucket(bucket));

        HBox actions = new HBox(8, selectButton, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, summary, progressBar, actions);
        return card;
    }

    private void refreshSelectedBucketDetails(YearMonth month) {
        if (selectedBucketId == null) {
            selectedBucketTitleLabel.setText("No bucket selected");
            selectedBucketBalanceLabel.setText("-");
            selectedBucketMetaLabel.setText("Select a savings bucket to view details.");
            selectedBucketProgress.setProgress(0);
            transactionRows.clear();
            setTransactionControlsEnabled(false);
            return;
        }

        SavingsBucket bucket = findBucketOrThrow(selectedBucketId);
        SavingsBucketSummary summary = savingsService.getBucketSummary(selectedBucketId, month);

        selectedBucketTitleLabel.setText(bucket.getName());
        selectedBucketBalanceLabel.setText(MoneyUtils.format(summary.getCurrentAmount(), resolveCurrencyCode()));
        selectedBucketMetaLabel.setText(
                "Month: +" + MoneyUtils.format(summary.getMonthlyContributions(), resolveCurrencyCode())
                        + " / -" + MoneyUtils.format(summary.getMonthlyWithdrawals(), resolveCurrencyCode())
                        + " | Entries: " + summary.getEntryCount()
        );
        selectedBucketProgress.setProgress(calculateProgress(summary.getCurrentAmount(), summary.getTargetAmount()));

        transactionRows.setAll(savingsService.listBucketEntries(selectedBucketId));
        setTransactionControlsEnabled(true);
    }

    private void loadBucketForEdit(SavingsBucket bucket) {
        editingBucketId = bucket.getId();
        bucketNameField.setText(bucket.getName());
        bucketTargetField.setText(bucket.getTargetAmount() == null ? "" : bucket.getTargetAmount().toPlainString());
        bucketActiveCheck.setSelected(bucket.isActive());
        bucketNotesArea.setText(bucket.getNotes());

        saveBucketButton.setText("Save Changes");
        clearBucketButton.setText("Cancel Edit");
    }

    private void clearBucketForm() {
        editingBucketId = null;
        bucketNameField.clear();
        bucketTargetField.clear();
        bucketActiveCheck.setSelected(true);
        bucketNotesArea.clear();

        saveBucketButton.setText("Create Bucket");
        clearBucketButton.setText("Clear Form");
    }

    private void clearTransactionForm() {
        transactionAmountField.clear();
        transactionDatePicker.setValue(defaultDateForSelectedMonth());
        transactionTypeCombo.getSelectionModel().select(SavingsEntryType.CONTRIBUTION);
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

    private BigDecimal parseOptionalTarget(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return MoneyUtils.parse(rawValue, "Target amount", true);
    }

    private BigDecimal parseSignedAmount(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String normalizedInput = rawValue.trim().replace(',', '.');
        try {
            BigDecimal amount = MoneyUtils.normalize(new BigDecimal(normalizedInput));
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException(fieldName + " must not be zero.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid amount.");
        }
    }

    private double calculateProgress(BigDecimal currentAmount, BigDecimal targetAmount) {
        BigDecimal current = MoneyUtils.zeroIfNull(currentAmount);
        BigDecimal target = MoneyUtils.zeroIfNull(targetAmount);
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        double progress = current.divide(target, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.max(0, Math.min(1, progress));
    }

    private String formatSignedMoney(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.zeroIfNull(amount);
        String prefix = normalized.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-";
        return prefix + MoneyUtils.format(normalized.abs(), resolveCurrencyCode());
    }

    private SavingsBucket findBucketOrThrow(String bucketId) {
        return buckets.stream()
                .filter(bucket -> bucket.getId().equals(bucketId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Savings bucket not found."));
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

    private boolean isFamilyModuleEnabled() {
        UserProfile profile = appContext.getCurrentUser();
        return profile != null && profile.isFamilyModuleEnabled();
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
