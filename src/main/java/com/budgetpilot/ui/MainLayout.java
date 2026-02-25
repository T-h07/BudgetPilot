package com.budgetpilot.ui;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.AppRouter;
import com.budgetpilot.core.PageId;
import com.budgetpilot.ui.pages.AchievementsPage;
import com.budgetpilot.ui.pages.DashboardPage;
import com.budgetpilot.ui.pages.ExpensesPage;
import com.budgetpilot.ui.pages.FamilyPage;
import com.budgetpilot.ui.pages.GoalsPage;
import com.budgetpilot.ui.pages.HabitsPage;
import com.budgetpilot.ui.pages.IncomePage;
import com.budgetpilot.ui.pages.InvestmentsPage;
import com.budgetpilot.ui.pages.OnboardingPage;
import com.budgetpilot.ui.pages.PlannerPage;
import com.budgetpilot.ui.pages.SavingsPage;
import com.budgetpilot.ui.pages.SettingsPage;
import com.budgetpilot.util.UiUtils;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.Objects;
import java.util.function.Supplier;

public class MainLayout extends BorderPane {
    private final AppRouter router = new AppRouter();
    private final Sidebar sidebar;
    private final TopBar topBar;
    private final StackPane contentHost = new StackPane();

    public MainLayout(AppContext appContext) {
        Objects.requireNonNull(appContext, "appContext must not be null");

        getStyleClass().add("app-shell");
        contentHost.getStyleClass().add("content-host");

        sidebar = new Sidebar(this::navigateTo);
        topBar = new TopBar(appContext);

        setLeft(sidebar);
        setTop(topBar);
        setCenter(contentHost);

        registerPages(appContext);
        bindRouter();
        navigateTo(PageId.DASHBOARD);
    }

    private void bindRouter() {
        router.currentPageNodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                contentHost.getChildren().setAll(newNode);
            }
        });

        router.currentPageIdProperty().addListener((obs, oldPage, newPage) -> {
            if (newPage != null) {
                sidebar.setActivePage(newPage);
                topBar.setActivePage(newPage);
            }
        });
    }

    private void registerPages(AppContext appContext) {
        registerPage(PageId.DASHBOARD, () -> new DashboardPage(appContext));
        registerPage(PageId.EXPENSES, () -> new ExpensesPage(appContext));
        registerPage(PageId.PLANNER, () -> new PlannerPage(appContext));
        registerPage(PageId.SAVINGS, () -> new SavingsPage(appContext));
        registerPage(PageId.INVESTMENTS, () -> new InvestmentsPage(appContext));
        registerPage(PageId.GOALS, () -> new GoalsPage(appContext));
        registerPage(PageId.INCOME, () -> new IncomePage(appContext));
        registerPage(PageId.ACHIEVEMENTS, () -> new AchievementsPage(appContext));
        registerPage(PageId.FAMILY, () -> new FamilyPage(appContext));
        registerPage(PageId.HABITS, () -> new HabitsPage(appContext));
        registerPage(PageId.SETTINGS, () -> new SettingsPage(appContext));
        registerPage(PageId.ONBOARDING, () -> new OnboardingPage(appContext));
    }

    private void registerPage(PageId pageId, Supplier<Node> pageFactory) {
        router.register(pageId, () -> UiUtils.createPageScroll(pageFactory.get()));
    }

    private void navigateTo(PageId pageId) {
        router.navigate(pageId);
    }
}
