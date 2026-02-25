package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.service.OnboardingService;
import com.budgetpilot.ui.components.ChipToggle;
import com.budgetpilot.ui.components.WizardStepHeader;
import com.budgetpilot.util.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OnboardingPage extends VBox {
    private static final List<String> DEFAULT_HABIT_TAGS = List.of(
            "#snacks",
            "#clothes",
            "#coffee",
            "#gaming",
            "#fuel",
            "#subscriptions",
            "#food-delivery",
            "#other"
    );

    private final AppContext appContext;
    private final OnboardingService onboardingService = new OnboardingService();
    private final OnboardingService.OnboardingData onboardingData = new OnboardingService.OnboardingData();

    private final WizardStepHeader stepHeader = new WizardStepHeader();
    private final Label bannerLabel = new Label();
    private final Label persistenceWarningLabel = new Label();
    private final VBox contentHost = new VBox(14);
    private final Button backButton = new Button("Back");
    private final Button nextButton = new Button("Next");

    private int stepIndex = 0;

    // Profile step controls
    private final TextField firstNameField = textField("First name");
    private final TextField lastNameField = textField("Last name");
    private final TextField emailField = textField("Email");
    private final TextField ageField = textField("Age (optional)");
    private final TextField currencyField = textField("Currency (e.g. EUR)");

    // Profile type step controls
    private final ToggleGroup profileTypeGroup = new ToggleGroup();
    private final VBox profileTypeOptionsBox = new VBox(10);

    // Habit step controls
    private final Map<String, ChipToggle> habitChipMap = new LinkedHashMap<>();
    private final TextField customHabitField = textField("Custom tag (optional, e.g. #books)");

    // Income step controls
    private final TextField incomeSourceField = textField("Income source");
    private final ComboBox<IncomeType> incomeTypeCombo = new ComboBox<>();
    private final TextField incomeAmountField = textField("Amount");
    private final CheckBox recurringIncomeCheck = new CheckBox("Recurring");
    private final CheckBox receivedIncomeCheck = new CheckBox("Received");
    private final ObservableList<String> incomeLineItems = FXCollections.observableArrayList();
    private final ListView<String> incomeListView = new ListView<>(incomeLineItems);

    // Plan step controls
    private final TextField fixedBudgetField = textField("Fixed costs budget");
    private final TextField foodBudgetField = textField("Food budget");
    private final TextField transportBudgetField = textField("Transport budget");
    private final TextField familyBudgetField = textField("Family budget");
    private final TextField discretionaryBudgetField = textField("Discretionary budget");
    private final TextField savingsPercentField = textField("Savings percent");
    private final TextField goalsPercentField = textField("Goals percent");
    private final TextField safetyBufferField = textField("Safety buffer amount");
    private final TextArea planNotesArea = new TextArea();

    public OnboardingPage(AppContext appContext) {
        this.appContext = appContext;
        initializeDefaults();

        setSpacing(14);
        setPadding(new Insets(28, 34, 30, 34));
        getStyleClass().addAll("page-root", "wizard-container");

        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.getStyleClass().add("error-banner");

        persistenceWarningLabel.getStyleClass().add("banner-warning");
        persistenceWarningLabel.setWrapText(true);
        persistenceWarningLabel.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> appContext.isPersistenceAvailable()
                                ? ""
                                : appContext.getPersistenceStatus() == null
                                ? ""
                                : appContext.getPersistenceStatus().getMessage(),
                        appContext.persistenceStatusProperty()
                )
        );
        persistenceWarningLabel.visibleProperty().bind(
                javafx.beans.binding.Bindings.createBooleanBinding(
                        () -> !appContext.isPersistenceAvailable(),
                        appContext.persistenceStatusProperty()
                )
        );
        persistenceWarningLabel.managedProperty().bind(persistenceWarningLabel.visibleProperty());

        HBox navBar = new HBox(10, backButton, nextButton);
        navBar.setAlignment(Pos.CENTER_RIGHT);
        nextButton.getStyleClass().add("quick-add-button");

        backButton.setOnAction(event -> {
            stepIndex = Math.max(0, stepIndex - 1);
            renderStep();
        });
        nextButton.setOnAction(event -> handleNext());

        getChildren().addAll(stepHeader, persistenceWarningLabel, bannerLabel, contentHost, navBar);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        bindValidationListeners();
        renderStep();
    }

    private void initializeDefaults() {
        currencyField.setText("EUR");
        incomeTypeCombo.getItems().setAll(IncomeType.values());
        incomeTypeCombo.getSelectionModel().select(IncomeType.SALARY);
        receivedIncomeCheck.setSelected(true);
        incomeListView.setPrefHeight(160);

        RadioButton studentOption = profileTypeOption(
                UserProfileType.STUDENT,
                "Student - lightweight budgeting with study-focused priorities."
        );
        RadioButton personalOption = profileTypeOption(
                UserProfileType.PERSONAL_USE,
                "Personal Use - flexible everyday planning and tracking."
        );
        RadioButton familyOption = profileTypeOption(
                UserProfileType.MAIN_FAMILY_SUPPORTER,
                "Main Family Supporter - includes family planning by default."
        );
        personalOption.setSelected(true);

        planNotesArea.setPromptText("Optional notes about your first month plan");
        planNotesArea.getStyleClass().add("text-input");
        planNotesArea.setPrefRowCount(3);

        for (String tag : DEFAULT_HABIT_TAGS) {
            ChipToggle chip = new ChipToggle(tag);
            habitChipMap.put(tag, chip);
        }

        setupStarterPlanDefaults();
    }

    private void setupStarterPlanDefaults() {
        fixedBudgetField.setText("0");
        foodBudgetField.setText("0");
        transportBudgetField.setText("0");
        familyBudgetField.setText("0");
        discretionaryBudgetField.setText("0");
        savingsPercentField.setText("20");
        goalsPercentField.setText("10");
        safetyBufferField.setText("0");
    }

    private RadioButton profileTypeOption(UserProfileType type, String description) {
        RadioButton option = new RadioButton(type.getLabel());
        option.setUserData(type);
        option.setToggleGroup(profileTypeGroup);
        option.getStyleClass().add("muted-text");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("muted-text");
        descriptionLabel.setWrapText(true);

        VBox box = new VBox(4, option, descriptionLabel);
        box.getStyleClass().add("form-card");
        box.setPadding(new Insets(12));

        option.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (selected) {
                onboardingData.setProfileType(type);
                updateNavigationState();
            }
        });

        profileTypeBox().getChildren().add(box);
        return option;
    }

    private VBox profileTypeBox() {
        return profileTypeOptionsBox;
    }

    private void bindValidationListeners() {
        List<TextField> profileFields = List.of(firstNameField, lastNameField, emailField, ageField, currencyField);
        for (TextField field : profileFields) {
            field.textProperty().addListener((obs, oldText, newText) -> updateNavigationState());
        }

        for (ChipToggle chip : habitChipMap.values()) {
            chip.selectedProperty().addListener((obs, wasSelected, isSelected) -> updateNavigationState());
        }
        customHabitField.textProperty().addListener((obs, oldText, newText) -> updateNavigationState());

        incomeSourceField.textProperty().addListener((obs, oldText, newText) -> updateNavigationState());
        incomeAmountField.textProperty().addListener((obs, oldText, newText) -> updateNavigationState());
        incomeTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateNavigationState());

        List<TextField> planFields = List.of(
                fixedBudgetField,
                foodBudgetField,
                transportBudgetField,
                familyBudgetField,
                discretionaryBudgetField,
                savingsPercentField,
                goalsPercentField,
                safetyBufferField
        );
        for (TextField field : planFields) {
            field.textProperty().addListener((obs, oldText, newText) -> updateNavigationState());
        }
    }

    private void handleNext() {
        clearBanner();
        if (!isCurrentStepValid()) {
            showError(stepValidationMessage());
            return;
        }

        if (stepIndex == 5) {
            finishOnboarding();
            return;
        }

        stepIndex = Math.min(5, stepIndex + 1);
        renderStep();
    }

    private void finishOnboarding() {
        try {
            collectProfileData();
            collectHabitData();
            collectPlanData();
            onboardingService.completeOnboarding(appContext, onboardingData);
            appContext.navigate(PageId.DASHBOARD);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void collectProfileData() {
        onboardingData.setFirstName(firstNameField.getText());
        onboardingData.setLastName(lastNameField.getText());
        onboardingData.setEmail(emailField.getText());
        onboardingData.setAgeText(ageField.getText());
        onboardingData.setCurrencyCode(currencyField.getText());
        onboardingData.setProfileType(selectedProfileType());
    }

    private void collectHabitData() {
        onboardingData.getSelectedHabitTags().clear();
        for (Map.Entry<String, ChipToggle> entry : habitChipMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                onboardingData.getSelectedHabitTags().add(entry.getKey());
            }
        }

        String customTag = customHabitField.getText() == null ? "" : customHabitField.getText().trim();
        if (!customTag.isBlank()) {
            onboardingData.getSelectedHabitTags().add(customTag.startsWith("#") ? customTag.toLowerCase() : "#" + customTag.toLowerCase());
        }
    }

    private void collectPlanData() {
        OnboardingService.PlanInput planInput = new OnboardingService.PlanInput();
        planInput.setFixedCostsBudget(parseMoney(fixedBudgetField.getText(), "fixed costs budget"));
        planInput.setFoodBudget(parseMoney(foodBudgetField.getText(), "food budget"));
        planInput.setTransportBudget(parseMoney(transportBudgetField.getText(), "transport budget"));
        planInput.setFamilyBudget(parseMoney(familyBudgetField.getText(), "family budget"));
        planInput.setDiscretionaryBudget(parseMoney(discretionaryBudgetField.getText(), "discretionary budget"));
        planInput.setSavingsPercent(parsePercent(savingsPercentField.getText(), "savings percent"));
        planInput.setGoalsPercent(parsePercent(goalsPercentField.getText(), "goals percent"));
        planInput.setSafetyBufferAmount(parseMoney(safetyBufferField.getText(), "safety buffer amount"));
        planInput.setNotes(planNotesArea.getText());
        onboardingData.setPlanInput(planInput);
    }

    private void renderStep() {
        contentHost.getChildren().setAll(stepContentFor(stepIndex));
        stepHeader.update(stepIndex + 1, 6, stepTitle(stepIndex), stepSubtitle(stepIndex));
        backButton.setDisable(stepIndex == 0);
        nextButton.setText(stepIndex == 5 ? "Finish Setup" : (stepIndex == 0 ? "Get Started" : "Next"));
        updateNavigationState();
    }

    private Node stepContentFor(int step) {
        return switch (step) {
            case 0 -> buildWelcomeStep();
            case 1 -> buildProfileStep();
            case 2 -> buildProfileTypeStep();
            case 3 -> buildHabitStep();
            case 4 -> buildIncomeStep();
            case 5 -> buildPlanStep();
            default -> buildWelcomeStep();
        };
    }

    private Node buildWelcomeStep() {
        Label intro = new Label("BudgetPilot helps you plan, track, and improve your monthly finances.");
        intro.getStyleClass().add("wizard-step-subtitle");
        intro.setWrapText(true);

        Button startButton = new Button("Get Started");
        startButton.getStyleClass().add("quick-add-button");
        startButton.setOnAction(event -> {
            stepIndex = 1;
            renderStep();
        });

        VBox box = createFormCard();
        box.getChildren().addAll(intro, new Separator(), startButton);
        return box;
    }

    private Node buildProfileStep() {
        GridPane grid = createFormGrid();
        addFormRow(grid, 0, "First Name", firstNameField);
        addFormRow(grid, 1, "Last Name", lastNameField);
        addFormRow(grid, 2, "Email", emailField);
        addFormRow(grid, 3, "Age", ageField);
        addFormRow(grid, 4, "Currency", currencyField);

        VBox box = createFormCard();
        box.getChildren().add(grid);
        return box;
    }

    private Node buildProfileTypeStep() {
        VBox box = createFormCard();
        box.getChildren().add(profileTypeBox());
        return box;
    }

    private Node buildHabitStep() {
        FlowPane chips = new FlowPane(8, 10);
        habitChipMap.values().forEach(chips.getChildren()::add);

        VBox box = createFormCard();
        box.getChildren().addAll(
                new Label("Select tags you want BudgetPilot to monitor from day one."),
                chips,
                customHabitField
        );
        return box;
    }

    private Node buildIncomeStep() {
        GridPane form = createFormGrid();
        addFormRow(form, 0, "Source", incomeSourceField);
        addFormRow(form, 1, "Type", incomeTypeCombo);
        addFormRow(form, 2, "Amount", incomeAmountField);

        HBox options = new HBox(12, recurringIncomeCheck, receivedIncomeCheck);
        options.setAlignment(Pos.CENTER_LEFT);

        Button addIncomeButton = new Button("Add Income");
        addIncomeButton.getStyleClass().add("quick-add-button");
        addIncomeButton.setOnAction(event -> handleAddIncome());

        Button removeIncomeButton = new Button("Remove Selected");
        removeIncomeButton.setOnAction(event -> {
            int selectedIndex = incomeListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < onboardingData.getIncomeInputs().size()) {
                onboardingData.getIncomeInputs().remove(selectedIndex);
                incomeLineItems.remove(selectedIndex);
                updateNavigationState();
            }
        });

        HBox actions = new HBox(10, addIncomeButton, removeIncomeButton);

        VBox box = createFormCard();
        box.getChildren().addAll(form, options, actions, incomeListView);
        return box;
    }

    private Node buildPlanStep() {
        GridPane grid = createFormGrid();
        addFormRow(grid, 0, "Fixed Costs", fixedBudgetField);
        addFormRow(grid, 1, "Food", foodBudgetField);
        addFormRow(grid, 2, "Transport", transportBudgetField);
        addFormRow(grid, 3, "Family", familyBudgetField);
        addFormRow(grid, 4, "Discretionary", discretionaryBudgetField);
        addFormRow(grid, 5, "Savings %", savingsPercentField);
        addFormRow(grid, 6, "Goals %", goalsPercentField);
        addFormRow(grid, 7, "Safety Buffer", safetyBufferField);

        familyBudgetField.setDisable(selectedProfileType() != UserProfileType.MAIN_FAMILY_SUPPORTER);
        if (familyBudgetField.isDisable()) {
            familyBudgetField.setText("0");
        }

        VBox box = createFormCard();
        box.getChildren().addAll(grid, new Label("Notes"), planNotesArea);
        return box;
    }

    private void handleAddIncome() {
        clearBanner();
        try {
            String source = ValidationUtils.requireNonBlank(incomeSourceField.getText(), "income source");
            IncomeType type = ValidationUtils.requireNonNull(incomeTypeCombo.getValue(), "income type");
            BigDecimal amount = parsePositiveAmount(incomeAmountField.getText(), "income amount");

            OnboardingService.IncomeInput incomeInput = new OnboardingService.IncomeInput(
                    source,
                    type,
                    amount,
                    recurringIncomeCheck.isSelected(),
                    receivedIncomeCheck.isSelected()
            );

            onboardingData.getIncomeInputs().add(incomeInput);
            incomeLineItems.add(type.getLabel() + " | " + source + " | " + amount);

            incomeSourceField.clear();
            incomeAmountField.clear();
            recurringIncomeCheck.setSelected(false);
            receivedIncomeCheck.setSelected(true);
            updateNavigationState();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private boolean isCurrentStepValid() {
        try {
            return switch (stepIndex) {
                case 0 -> true;
                case 1 -> isProfileStepValid();
                case 2 -> selectedProfileType() != null;
                case 3 -> true;
                case 4 -> !onboardingData.getIncomeInputs().isEmpty();
                case 5 -> isPlanStepValid();
                default -> false;
            };
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean isProfileStepValid() {
        ValidationUtils.requireNonBlank(firstNameField.getText(), "first name");
        ValidationUtils.requireNonBlank(lastNameField.getText(), "last name");
        ValidationUtils.requireValidEmail(emailField.getText(), "email");
        ValidationUtils.parseOptionalNonNegativeInteger(ageField.getText(), "age");
        ValidationUtils.requireNonBlank(currencyField.getText(), "currency");
        return true;
    }

    private boolean isPlanStepValid() {
        parseMoney(fixedBudgetField.getText(), "fixed costs");
        parseMoney(foodBudgetField.getText(), "food budget");
        parseMoney(transportBudgetField.getText(), "transport budget");
        parseMoney(familyBudgetField.getText(), "family budget");
        parseMoney(discretionaryBudgetField.getText(), "discretionary budget");
        parsePercent(savingsPercentField.getText(), "savings percent");
        parsePercent(goalsPercentField.getText(), "goals percent");
        parseMoney(safetyBufferField.getText(), "safety buffer");
        return true;
    }

    private String stepValidationMessage() {
        return switch (stepIndex) {
            case 1 -> "Please complete your profile with a valid email.";
            case 2 -> "Please choose a profile type.";
            case 4 -> "Please add at least one income entry.";
            case 5 -> "Please provide valid non-negative budget values and percentages between 0 and 100.";
            default -> "Please complete required fields before continuing.";
        };
    }

    private void updateNavigationState() {
        nextButton.setDisable(!isCurrentStepValid());
    }

    private void showError(String message) {
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
        bannerLabel.setText(message);
    }

    private void clearBanner() {
        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        bannerLabel.setText("");
    }

    private UserProfileType selectedProfileType() {
        Toggle selected = profileTypeGroup.getSelectedToggle();
        return selected == null ? null : (UserProfileType) selected.getUserData();
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        return grid;
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

    private VBox createFormCard() {
        VBox box = new VBox(12);
        box.getStyleClass().addAll("card", "form-card");
        box.setPadding(new Insets(18));
        return box;
    }

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("text-input");
        return field;
    }

    private BigDecimal parseMoney(String raw, String fieldName) {
        try {
            BigDecimal value = new BigDecimal(raw == null || raw.isBlank() ? "0" : raw.trim());
            return ValidationUtils.requireNonNegative(value, fieldName);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }

    private BigDecimal parsePercent(String raw, String fieldName) {
        try {
            BigDecimal value = new BigDecimal(raw == null || raw.isBlank() ? "0" : raw.trim());
            return ValidationUtils.requirePercent(value, fieldName);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid percentage");
        }
    }

    private BigDecimal parsePositiveAmount(String raw, String fieldName) {
        try {
            BigDecimal value = new BigDecimal(raw == null || raw.isBlank() ? "0" : raw.trim());
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than 0");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }

    private String stepTitle(int step) {
        return switch (step) {
            case 0 -> "Welcome";
            case 1 -> "Profile Setup";
            case 2 -> "Profile Type";
            case 3 -> "Habit Templates";
            case 4 -> "Initial Income Setup";
            case 5 -> "Basic Monthly Plan";
            default -> "Onboarding";
        };
    }

    private String stepSubtitle(int step) {
        return switch (step) {
            case 0 -> "Set up your financial cockpit in a few guided steps.";
            case 1 -> "Tell BudgetPilot who you are and which currency you use.";
            case 2 -> "Pick the profile mode that best matches your financial context.";
            case 3 -> "Choose tags for spending habits you want to control.";
            case 4 -> "Add one or more income entries for the selected month.";
            case 5 -> "Define starter budgets and percentages for your first month.";
            default -> "";
        };
    }
}
