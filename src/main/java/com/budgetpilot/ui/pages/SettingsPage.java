package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.core.Theme;
import com.budgetpilot.core.ThemeManager;
import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.service.backup.BackupService;
import com.budgetpilot.service.PersistenceStatus;
import com.budgetpilot.service.month.MonthRolloverService;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.service.retention.DataRetentionService;
import com.budgetpilot.service.settings.SettingsService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.DbStore;
import com.budgetpilot.ui.components.MonthRolloverDialog;
import com.budgetpilot.ui.components.SectionCard;
import com.budgetpilot.ui.components.ToggleCard;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.UiUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.stage.FileChooser;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class SettingsPage extends VBox {
    private final AppContext appContext;
    private final SettingsService settingsService;
    private final BackupService backupService;
    private final MonthRolloverService rolloverService;
    private final DataRetentionService dataRetentionService;
    private final Runnable contextListener = this::populateFromContext;

    private final Label bannerLabel = new Label();

    private final TextField firstNameField = textField("First name");
    private final TextField lastNameField = textField("Last name");
    private final TextField emailField = textField("Email");
    private final TextField ageField = textField("Age");
    private final TextField currencyField = textField("Currency");
    private final ComboBox<UserProfileType> profileTypeCombo = new ComboBox<>();
    private final ComboBox<Theme> themeCombo = new ComboBox<>();

    private final ToggleCard familyModuleToggle = new ToggleCard("Family Module", "Enable dependent and family budgeting pages.");
    private final ToggleCard investmentsModuleToggle = new ToggleCard("Investments Module", "Enable investment tracking and allocation workspace.");
    private final ToggleCard achievementsModuleToggle = new ToggleCard("Achievements Module", "Enable habit streaks and achievement milestones.");
    private boolean populatingForm;

    private final Label persistenceStatusLabel = new Label();
    private final Label databasePathLabel = new Label();
    private final Label backupsPathLabel = new Label();

    public SettingsPage(AppContext appContext) {
        this.appContext = appContext;
        this.settingsService = new SettingsService(appContext);
        this.backupService = new BackupService(appContext);
        this.rolloverService = new MonthRolloverService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));
        this.dataRetentionService = new DataRetentionService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(UiUtils.SECTION_SPACING);
        setPadding(UiUtils.PAGE_PADDING);
        getStyleClass().add("page-root");

        getChildren().add(UiUtils.createPageHeader(
                "Settings",
                "Manage profile, modules, month session controls, and developer utilities."
        ));

        bannerLabel.setManaged(false);
        bannerLabel.setVisible(false);
        getChildren().add(bannerLabel);

        SectionCard profileSection = new SectionCard(
                "Profile",
                "Update personal details and profile mode.",
                buildProfileSectionBody()
        );
        profileSection.getStyleClass().add("settings-section");

        SectionCard modulesSection = new SectionCard(
                "Modules",
                "Enable or disable optional sections from the sidebar.",
                buildModuleSectionBody()
        );
        modulesSection.getStyleClass().add("settings-section");

        SectionCard appearanceSection = new SectionCard(
                "Appearance",
                "Switch between dark and light theme palettes. Changes apply instantly.",
                buildAppearanceSectionBody()
        );
        appearanceSection.getStyleClass().add("settings-section");

        SectionCard monthSection = new SectionCard(
                "Month & Session",
                "Move between months and optionally initialize a fresh plan.",
                buildMonthSectionBody()
        );
        monthSection.getStyleClass().add("settings-section");

        SectionCard persistenceSection = new SectionCard(
                "Data & Persistence",
                "Manage local database path, backup exports, imports, and persistence mode.",
                buildPersistenceSectionBody()
        );
        persistenceSection.getStyleClass().add("settings-section");

        SectionCard developerSection = new SectionCard(
                "Developer Tools",
                "Utilities for quickly resetting and reseeding local app state.",
                buildDeveloperSectionBody()
        );
        developerSection.getStyleClass().add("settings-section");

        getChildren().addAll(profileSection, modulesSection, appearanceSection, monthSection, persistenceSection, developerSection);

        appContext.addChangeListener(contextListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                appContext.removeChangeListener(contextListener);
            }
        });
        populateFromContext();
    }

    private Node buildProfileSectionBody() {
        profileTypeCombo.getItems().setAll(UserProfileType.values());
        profileTypeCombo.getStyleClass().addAll("combo-box", "form-combo");

        GridPane form = createFormGrid();
        addFormRow(form, 0, "First Name", firstNameField);
        addFormRow(form, 1, "Last Name", lastNameField);
        addFormRow(form, 2, "Email", emailField);
        addFormRow(form, 3, "Age", ageField);
        addFormRow(form, 4, "Currency", currencyField);
        addFormRow(form, 5, "Profile Type", profileTypeCombo);

        Button saveButton = new Button("Save Profile");
        saveButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveButton.setOnAction(event -> saveProfile());

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        resetButton.setOnAction(event -> populateFromContext());

        Button signOutButton = new Button("Sign Out");
        signOutButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        signOutButton.setOnAction(event -> {
            appContext.signOut();
            appContext.navigate(PageId.LOGIN);
        });

        HBox actions = new HBox(10, saveButton, resetButton, signOutButton);
        return new VBox(12, form, actions);
    }

    private Node buildModuleSectionBody() {
        Button saveModulesButton = new Button("Save Module Preferences");
        saveModulesButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        saveModulesButton.setOnAction(event -> saveModules());

        return new VBox(
                12,
                familyModuleToggle,
                investmentsModuleToggle,
                achievementsModuleToggle,
                saveModulesButton
        );
    }

    private Node buildMonthSectionBody() {
        Label monthLabel = new Label();
        monthLabel.getStyleClass().add("page-subtitle");
        monthLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> "Selected Month: " + appContext.getCurrentMonthDisplayText(),
                        appContext.selectedMonthProperty()
                )
        );

        Button prevButton = new Button("Previous Month");
        prevButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        prevButton.setOnAction(event -> settingsService.shiftSelectedMonth(-1));

        Button nextButton = new Button("Next Month");
        nextButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        nextButton.setOnAction(event -> settingsService.shiftSelectedMonth(1));

        Button newMonthButton = new Button("Start New Month");
        newMonthButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        newMonthButton.setOnAction(event -> startNewMonthFromSettings());

        HBox controls = new HBox(10, prevButton, nextButton, newMonthButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        return new VBox(12, monthLabel, controls);
    }

    private Node buildDeveloperSectionBody() {
        Button clearAllButton = new Button("Clear All Data");
        clearAllButton.getStyleClass().addAll("danger-button", "btn-danger");
        clearAllButton.setOnAction(event -> {
            if (confirm("Clear all BudgetPilot data? This action cannot be undone.")) {
                settingsService.clearAllData();
                showSuccess("All data cleared. Redirecting to onboarding.");
                appContext.navigate(PageId.ONBOARDING);
            }
        });

        Button seedDemoButton = new Button("Seed Demo Data (Selected Month)");
        seedDemoButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        seedDemoButton.setOnAction(event -> {
            settingsService.seedDemoDataForSelectedMonth();
            populateFromContext();
            showSuccess("Demo data seeded for " + appContext.getCurrentMonthDisplayText() + ".");
        });

        Button freshOnboardingButton = new Button("Reset to Fresh Onboarding");
        freshOnboardingButton.getStyleClass().addAll("danger-button", "btn-danger");
        freshOnboardingButton.setOnAction(event -> {
            if (confirm("Reset app to fresh onboarding state? This clears all current data.")) {
                settingsService.resetToFreshOnboarding();
                showSuccess("App reset complete. Redirecting to onboarding.");
                appContext.navigate(PageId.ONBOARDING);
            }
        });

        return new VBox(10, clearAllButton, seedDemoButton, freshOnboardingButton);
    }

    private Node buildPersistenceSectionBody() {
        persistenceStatusLabel.getStyleClass().add("persistence-status-card");
        databasePathLabel.getStyleClass().addAll("muted-text", "settings-data-path");
        backupsPathLabel.getStyleClass().addAll("muted-text", "settings-data-path");

        Button exportBackupButton = new Button("Export Backup");
        exportBackupButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        exportBackupButton.setOnAction(event -> {
            clearBanner();
            try {
                var backupPath = backupService.exportBackup();
                showSuccess("Backup exported to: " + backupPath);
            } catch (Exception ex) {
                showError("Backup export failed: " + ex.getMessage());
            }
        });

        Button importBackupButton = new Button("Import Backup");
        importBackupButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        importBackupButton.setOnAction(event -> {
            clearBanner();
            if (!confirm("Import backup and replace current data?")) {
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select BudgetPilot Backup");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Backup", "*.json"));
            Window owner = getScene() == null ? null : getScene().getWindow();
            File selected = fileChooser.showOpenDialog(owner);
            if (selected == null) {
                return;
            }
            try {
                backupService.importBackup(selected.toPath());
                populateFromContext();
                showSuccess("Backup imported successfully.");
                appContext.navigate(appContext.onboardingCompleted() ? PageId.DASHBOARD : PageId.ONBOARDING);
            } catch (Exception ex) {
                showError("Backup import failed: " + ex.getMessage());
            }
        });

        Button openDataFolderButton = new Button("Open Data Folder");
        openDataFolderButton.getStyleClass().addAll("secondary-button", "btn-secondary");
        openDataFolderButton.setOnAction(event -> {
            clearBanner();
            PersistenceStatus status = appContext.getPersistenceStatus();
            if (status == null || status.getDatabasePath() == null) {
                showError("Data directory is not available.");
                return;
            }
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(status.getDatabasePath().getParent().toFile());
                } else {
                    showError("Desktop integration is not supported on this system.");
                    return;
                }
                showSuccess("Opened data folder.");
            } catch (IOException ex) {
                showError("Unable to open data folder: " + ex.getMessage());
            }
        });

        HBox actions = new HBox(10, exportBackupButton, importBackupButton, openDataFolderButton);
        actions.getStyleClass().add("backup-actions-row");
        actions.setAlignment(Pos.CENTER_LEFT);

        Label retentionInfo = new Label("Old monthly data older than 12 months is automatically removed.");
        retentionInfo.getStyleClass().addAll("banner-info", "muted-text");
        retentionInfo.setWrapText(true);

        VBox box = new VBox(10,
                persistenceStatusLabel,
                new Label("Database Path:"),
                databasePathLabel,
                new Label("Backups Folder:"),
                backupsPathLabel,
                retentionInfo,
                actions
        );
        return box;
    }

    private Node buildAppearanceSectionBody() {
        themeCombo.getItems().setAll(Theme.values());
        themeCombo.getStyleClass().addAll("combo-box", "form-combo");
        themeCombo.valueProperty().addListener((obs, oldTheme, newTheme) -> {
            if (populatingForm || newTheme == null || newTheme == oldTheme) {
                return;
            }
            clearBanner();
            try {
                settingsService.updateTheme(newTheme);
                if (getScene() != null) {
                    ThemeManager.apply(getScene(), newTheme);
                }
                showSuccess("Theme applied: " + newTheme.getLabel() + ".");
            } catch (RuntimeException ex) {
                showError("Unable to apply theme: " + ex.getMessage());
            }
        });

        GridPane form = createFormGrid();
        addFormRow(form, 0, "Theme", themeCombo);

        Label helper = new Label("Applied instantly and remembered for next startup.");
        helper.getStyleClass().add("muted-text");
        return new VBox(10, form, helper);
    }

    private void saveProfile() {
        clearBanner();
        try {
            SettingsService.ProfileSettingsData data = new SettingsService.ProfileSettingsData();
            data.setFirstName(ValidationUtils.requireNonBlank(firstNameField.getText(), "firstName"));
            data.setLastName(ValidationUtils.requireNonBlank(lastNameField.getText(), "lastName"));
            data.setEmail(ValidationUtils.requireValidEmail(emailField.getText(), "email"));
            data.setAgeText(ageField.getText());
            data.setCurrencyCode(ValidationUtils.requireNonBlank(currencyField.getText(), "currencyCode"));
            data.setProfileType(profileTypeCombo.getValue());
            settingsService.updateProfile(data);
            showSuccess("Profile settings saved.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void saveModules() {
        clearBanner();
        try {
            settingsService.updateModuleFlags(
                    familyModuleToggle.getToggle().isSelected(),
                    investmentsModuleToggle.getToggle().isSelected(),
                    achievementsModuleToggle.getToggle().isSelected()
            );
            showSuccess("Module preferences saved.");
            appContext.navigate(PageId.SETTINGS);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void startNewMonthFromSettings() {
        clearBanner();
        YearMonth sourceMonth = appContext.getSelectedMonth();
        YearMonth targetMonth = sourceMonth.plusMonths(1);

        if (sourceMonth.equals(MonthUtils.currentMonth())) {
            int todayDay = LocalDate.now().getDayOfMonth();
            if (todayDay < sourceMonth.lengthOfMonth()) {
                boolean confirmed = confirm("This month is not finished yet. Are you sure you want to start a new month?");
                if (!confirmed) {
                    return;
                }
            }
        }

        String currencyCode = appContext.getCurrentUser() == null ? "EUR" : appContext.getCurrentUser().getCurrencyCode();
        List<ExpenseTemplate> expenseTemplates = rolloverService.listActiveExpenseTemplates();
        List<IncomeTemplate> incomeTemplates = rolloverService.listActiveIncomeTemplates();
        MonthRolloverDialog.Result wizardResult = MonthRolloverDialog.show(
                getScene() == null ? null : getScene().getWindow(),
                targetMonth,
                expenseTemplates,
                incomeTemplates,
                currencyCode
        );
        if (!wizardResult.isStartNewMonth()) {
            return;
        }

        try {
            BudgetStore store = ValidationUtils.requireNonNull(appContext.getStore(), "store");
            if (store instanceof DbStore dbStore) {
                dbStore.runBulkUpdate(() -> {
                    rolloverService.startNewMonth(targetMonth, wizardResult.getOptions());
                    dataRetentionService.purgeOldMonthData();
                });
            } else {
                rolloverService.startNewMonth(targetMonth, wizardResult.getOptions());
                dataRetentionService.purgeOldMonthData();
            }
            appContext.setSelectedMonth(targetMonth);
            appContext.notifyContextChanged();
            showSuccess("Started " + MonthUtils.format(targetMonth) + " successfully.");
        } catch (RuntimeException ex) {
            showError("Unable to start new month: " + ex.getMessage());
        }
    }

    private void populateFromContext() {
        populatingForm = true;
        try {
            updatePersistenceStatus();
            UserProfile profile = appContext.getCurrentUser();
            if (profile == null) {
                firstNameField.setText("");
                lastNameField.setText("");
                emailField.setText("");
                ageField.setText("");
                currencyField.setText("EUR");
                profileTypeCombo.getSelectionModel().select(UserProfileType.PERSONAL_USE);
                familyModuleToggle.getToggle().setSelected(false);
                investmentsModuleToggle.getToggle().setSelected(true);
                achievementsModuleToggle.getToggle().setSelected(true);
                themeCombo.getSelectionModel().select(appContext.getTheme());
                return;
            }

            firstNameField.setText(profile.getFirstName());
            lastNameField.setText(profile.getLastName());
            emailField.setText(profile.getEmail());
            ageField.setText(profile.getAge() == null ? "" : String.valueOf(profile.getAge()));
            currencyField.setText(profile.getCurrencyCode());
            profileTypeCombo.getSelectionModel().select(profile.getProfileType());
            familyModuleToggle.getToggle().setSelected(profile.isFamilyModuleEnabled());
            investmentsModuleToggle.getToggle().setSelected(profile.isInvestmentsModuleEnabled());
            achievementsModuleToggle.getToggle().setSelected(profile.isAchievementsModuleEnabled());
            themeCombo.getSelectionModel().select(appContext.getTheme());
        } finally {
            populatingForm = false;
        }
    }

    private void updatePersistenceStatus() {
        PersistenceStatus status = appContext.getPersistenceStatus();
        if (status == null) {
            persistenceStatusLabel.setText("Persistence status unavailable.");
            persistenceStatusLabel.getStyleClass().removeAll("persistence-status-ok", "persistence-status-fallback");
            persistenceStatusLabel.getStyleClass().add("persistence-status-fallback");
            databasePathLabel.setText("-");
            backupsPathLabel.setText("-");
            return;
        }

        persistenceStatusLabel.setText(
                (status.isPersistent() ? "Connected: " : "Fallback: ") + status.getMessage()
        );
        persistenceStatusLabel.getStyleClass().removeAll("persistence-status-ok", "persistence-status-fallback");
        persistenceStatusLabel.getStyleClass().add(status.isPersistent() ? "persistence-status-ok" : "persistence-status-fallback");

        databasePathLabel.setText(status.getDatabasePath() == null ? "-" : status.getDatabasePath().toString());
        backupsPathLabel.setText(status.getBackupsPath() == null ? "-" : status.getBackupsPath().toString());
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Confirm Action");
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("text-input", "form-input");
        return field;
    }
}
