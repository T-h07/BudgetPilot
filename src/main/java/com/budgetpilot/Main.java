package com.budgetpilot;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.persistence.DbManager;
import com.budgetpilot.service.PersistenceStatus;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.DbStore;
import com.budgetpilot.store.DemoDataSeeder;
import com.budgetpilot.store.FullDataStore;
import com.budgetpilot.store.InMemoryStore;
import com.budgetpilot.ui.MainLayout;
import com.budgetpilot.util.AppPaths;
import com.budgetpilot.util.MonthUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

public class Main extends Application {
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final boolean DEV_MODE_SEED_DEMO = false;
    private static final String SELECTED_MONTH_SETTING_KEY = "selected_month";

    private AutoCloseable closeableStore;

    @Override
    public void start(Stage stage) {
        Path databasePath = AppPaths.getDatabasePath();
        Path backupsPath = AppPaths.getBackupsDir();

        YearMonth selectedMonth = MonthUtils.currentMonth();
        BudgetStore store;
        PersistenceStatus persistenceStatus;

        try {
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
        appContext.setPersistenceStatus(persistenceStatus);
        appContext.reloadCurrentUserFromStore();

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
        }

        PageId startupPage = appContext.onboardingCompleted() ? PageId.DASHBOARD : PageId.ONBOARDING;

        MainLayout mainLayout = new MainLayout(appContext, startupPage);

        Scene scene = new Scene(mainLayout, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        List<String> stylesheets = List.of(
                "/css/base.css",
                "/css/layout.css",
                "/css/components.css",
                "/css/forms.css",
                "/css/pages/dashboard.css",
                "/css/pages/onboarding.css",
                "/css/pages/settings.css",
                "/css/pages/income.css",
                "/css/pages/planner.css",
                "/css/pages/expenses.css",
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

        stage.setTitle("BudgetPilot");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.show();
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
}
