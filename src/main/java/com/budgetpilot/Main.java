package com.budgetpilot;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.DemoDataSeeder;
import com.budgetpilot.store.InMemoryStore;
import com.budgetpilot.ui.MainLayout;
import com.budgetpilot.util.MonthUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.net.URL;
import java.time.YearMonth;

public class Main extends Application {
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final boolean DEV_MODE_SEED_DEMO = false;

    @Override
    public void start(Stage stage) {
        YearMonth selectedMonth = MonthUtils.currentMonth();
        BudgetStore store = new InMemoryStore();
        if (DEV_MODE_SEED_DEMO) {
            DemoDataSeeder.seed(store, selectedMonth);
        }

        AppContext appContext = new AppContext(store, selectedMonth);
        appContext.reloadCurrentUserFromStore();
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
                "/css/pages/planner.css"
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

    public static void main(String[] args) {
        launch(args);
    }
}
