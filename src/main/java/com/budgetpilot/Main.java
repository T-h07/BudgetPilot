package com.budgetpilot;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.core.Theme;
import com.budgetpilot.core.ThemeManager;
import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.persistence.DbManager;
import com.budgetpilot.service.PersistenceStatus;
import com.budgetpilot.service.month.MonthRolloverService;
import com.budgetpilot.service.retention.DataRetentionService;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.DbStore;
import com.budgetpilot.store.DemoDataSeeder;
import com.budgetpilot.store.FullDataStore;
import com.budgetpilot.store.InMemoryStore;
import com.budgetpilot.ui.MainLayout;
import com.budgetpilot.ui.components.MonthRolloverDialog;
import com.budgetpilot.util.AppPaths;
import com.budgetpilot.util.MonthUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

public class Main extends Application {
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final boolean DEV_MODE_SEED_DEMO = false;
    private static final String SELECTED_MONTH_SETTING_KEY = "selected_month";
    private static final String THEME_SETTING_KEY = "theme";

    private AutoCloseable closeableStore;

    @Override
    public void start(Stage stage) {
        Path databasePath = null;
        Path backupsPath = null;
        try {
            databasePath = AppPaths.getDatabasePath();
            backupsPath = AppPaths.getBackupsDir();
        } catch (RuntimeException ex) {
            System.err.println("Failed to resolve app data paths. Persistence will run in fallback mode.");
            ex.printStackTrace(System.err);
        }

        YearMonth selectedMonth = MonthUtils.currentMonth();
        Theme selectedTheme = Theme.DARK;
        BudgetStore store;
        PersistenceStatus persistenceStatus;

        try {
            if (databasePath == null) {
                throw new IllegalStateException("Database path is unavailable.");
            }
            DbStore dbStore = new DbStore(DbManager.open(databasePath));
            store = dbStore;
            closeableStore = dbStore;

            String savedMonth = dbStore.getAppSetting(SELECTED_MONTH_SETTING_KEY);
            if (savedMonth != null && !savedMonth.isBlank()) {
                try {
                    selectedMonth = YearMonth.parse(savedMonth);
                } catch (Exception ex) {
                    selectedMonth = MonthUtils.currentMonth();
                }
            }
            selectedTheme = Theme.fromSettingValue(dbStore.getAppSetting(THEME_SETTING_KEY));

            if (DEV_MODE_SEED_DEMO && store.getUserProfile() == null) {
                YearMonth seedMonth = selectedMonth;
                dbStore.runBulkUpdate(() -> DemoDataSeeder.seed(dbStore, seedMonth));
            }

            persistenceStatus = new PersistenceStatus(
                    true,
                    "SQLite persistence connected.",
                    databasePath,
                    backupsPath
            );
        } catch (Exception ex) {
            System.err.println("Failed to initialize SQLite persistence. Falling back to in-memory mode.");
            ex.printStackTrace(System.err);

            store = new InMemoryStore();
            closeableStore = null;

            if (DEV_MODE_SEED_DEMO) {
                DemoDataSeeder.seed(store, selectedMonth);
            }

            persistenceStatus = new PersistenceStatus(
                    false,
                    "Persistence unavailable - running in temporary in-memory mode.",
                    databasePath,
                    backupsPath
            );
        }

        AppContext appContext = new AppContext(store, selectedMonth);
        appContext.setTheme(selectedTheme);
        appContext.setPersistenceStatus(persistenceStatus);
        appContext.reloadCurrentUserFromStore();
        DataRetentionService dataRetentionService = new DataRetentionService(store);
        try {
            dataRetentionService.purgeOldMonthData();
        } catch (RuntimeException ex) {
            System.err.println("Failed running 12-month data retention purge: " + ex.getMessage());
        }

        if (store instanceof FullDataStore fullDataStore) {
            try {
                fullDataStore.saveAppSetting(SELECTED_MONTH_SETTING_KEY, appContext.getSelectedMonth().toString());
            } catch (Exception ex) {
                System.err.println("Failed persisting initial selected month: " + ex.getMessage());
            }
            appContext.selectedMonthProperty().addListener((obs, oldMonth, newMonth) -> {
                if (newMonth == null) {
                    return;
                }
                try {
                    fullDataStore.saveAppSetting(SELECTED_MONTH_SETTING_KEY, newMonth.toString());
                } catch (Exception ex) {
                    System.err.println("Failed persisting selected month setting: " + ex.getMessage());
                }
            });
            appContext.themeProperty().addListener((obs, oldTheme, newTheme) -> {
                if (newTheme == null) {
                    return;
                }
                try {
                    fullDataStore.saveAppSetting(THEME_SETTING_KEY, newTheme.getSettingValue());
                } catch (Exception ex) {
                    System.err.println("Failed persisting theme setting: " + ex.getMessage());
                }
            });
        }

        PageId startupPage;
        if (!appContext.onboardingCompleted()) {
            startupPage = PageId.ONBOARDING;
        } else {
            startupPage = PageId.LOGIN;
        }

        MainLayout mainLayout = new MainLayout(appContext, startupPage);

        Scene scene = new Scene(mainLayout, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        List<String> stylesheets = List.of(
                "/css/base.css",
                "/css/layout.css",
                "/css/components.css",
                "/css/forms.css",
                "/css/date-picker.css",
                "/css/pages/dashboard.css",
                "/css/pages/login.css",
                "/css/pages/onboarding.css",
                "/css/pages/settings.css",
                "/css/pages/income.css",
                "/css/pages/planner.css",
                "/css/pages/expenses.css",
                "/css/pages/templates.css",
                "/css/pages/insights.css",
                "/css/pages/savings.css",
                "/css/pages/goals.css",
                "/css/pages/family.css",
                "/css/pages/habits.css",
                "/css/pages/investments.css",
                "/css/pages/achievements.css"
        );
        for (String stylesheetPath : stylesheets) {
            URL stylesheet = Main.class.getResource(stylesheetPath);
            if (stylesheet == null) {
                throw new IllegalStateException("Missing stylesheet: " + stylesheetPath);
            }
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        ThemeManager.apply(scene, appContext.getTheme());

        stage.setTitle("BudgetPilot");
        applyAppIcon(stage);
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.show();

        MonthRolloverService rolloverService = new MonthRolloverService(store);
        Platform.runLater(() -> handleMonthRollover(stage, appContext, rolloverService, dataRetentionService));
    }

    @Override
    public void stop() throws Exception {
        if (closeableStore != null) {
            closeableStore.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void applyAppIcon(Stage stage) {
        if (stage == null) {
            return;
        }
        for (String iconPath : List.of("/images/BudgetPilot.ico", "/images/budgetpilot-logo.png")) {
            try (InputStream stream = Main.class.getResourceAsStream(iconPath)) {
                if (stream == null) {
                    continue;
                }
                Image image = new Image(stream);
                if (image.isError()) {
                    continue;
                }
                stage.getIcons().add(image);
                return;
            } catch (Exception ignored) {
                // Keep startup resilient; missing/invalid icons should not block app launch.
            }
        }
    }

    private void handleMonthRollover(
            Stage stage,
            AppContext appContext,
            MonthRolloverService rolloverService,
            DataRetentionService dataRetentionService
    ) {
        YearMonth systemMonth = MonthUtils.currentMonth();
        if (!appContext.onboardingCompleted() || !rolloverService.shouldPromptForMonth(systemMonth)) {
            return;
        }

        List<ExpenseTemplate> expenseTemplates = rolloverService.listActiveExpenseTemplates();
        List<IncomeTemplate> incomeTemplates = rolloverService.listActiveIncomeTemplates();
        String currencyCode = appContext.getCurrentUser() == null ? "EUR" : appContext.getCurrentUser().getCurrencyCode();
        MonthRolloverDialog.Result result = MonthRolloverDialog.show(
                stage,
                systemMonth,
                expenseTemplates,
                incomeTemplates,
                currencyCode
        );
        if (!result.isStartNewMonth()) {
            rolloverService.acknowledgeMonth(systemMonth);
            return;
        }

        try {
            BudgetStore store = appContext.getStore();
            if (store instanceof DbStore dbStore) {
                dbStore.runBulkUpdate(() -> {
                    rolloverService.startNewMonth(systemMonth, result.getOptions());
                    dataRetentionService.purgeOldMonthData();
                });
            } else {
                rolloverService.startNewMonth(systemMonth, result.getOptions());
                dataRetentionService.purgeOldMonthData();
            }
            appContext.setSelectedMonth(systemMonth);
            appContext.notifyContextChanged();
        } catch (RuntimeException ex) {
            System.err.println("Month rollover failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }