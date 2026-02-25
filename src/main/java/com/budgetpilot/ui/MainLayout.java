package com.budgetpilot.ui;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.AppRouter;
import com.budgetpilot.core.PageId;
import com.budgetpilot.model.UserProfile;
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
    private final AppContext appContext;
    private final AppRouter router = new AppRouter();
    private final Sidebar sidebar;
    private final TopBar topBar;
    private final StackPane contentHost = new StackPane();

    public MainLayout(AppContext appContext, PageId startupPage) {
        this.appContext = Objects.requireNonNull(appContext, "appContext must not be null");

        getStyleClass().add("app-shell");
        contentHost.getStyleClass().add("content-host");

        sidebar = new Sidebar(appContext, this::navigateTo);
        topBar = new TopBar(appContext);

        setCenter(contentHost);
        setLeft(sidebar);
        setTop(topBar);

        appContext.setNavigator(this::navigateTo);

        registerPages(appContext);
        bindRouter();
        bindContext();

        PageId initialPage = startupPage == null ? defaultStartupPage() : startupPage;
        navigateTo(initialPage);
    }

    public MainLayout(AppContext appContext) {
        this(appContext, null);
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
                refreshChrome();
            }
        });
    }

    private void bindContext() {
        appContext.addChangeListener(() -> {
            sidebar.refreshNavigation();
            ensureCurrentPageAllowed();
            refreshActiveDataPage();
            refreshChrome();
        });
    }

    private void refreshActiveDataPage() {
        PageId currentPage = router.getCurrentPageId();
        if (currentPage == null) {
            return;
        }
        if (currentPage == PageId.DASHBOARD
                || currentPage == PageId.INCOME
                || currentPage == PageId.PLANNER
                || currentPage == PageId.EXPENSES) {
            navigateTo(currentPage);
        }
    }

    private void refreshChrome() {
        boolean onboardingMode = !appContext.onboardingCompleted() || router.getCurrentPageId() == PageId.ONBOARDING;
        setLeft(onboardingMode ? null : sidebar);
        setTop(onboardingMode ? null : topBar);
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

    public void navigateTo(PageId pageId) {
        PageId target = pageId == null ? defaultStartupPage() : pageId;
        if (!isPageVisible(target)) {
            target = defaultStartupPage();
        }
        router.navigate(target);
    }

    private void ensureCurrentPageAllowed() {
        PageId currentPage = router.getCurrentPageId();
        if (currentPage == null) {
            return;
        }
        if (!isPageVisible(currentPage)) {
            navigateTo(defaultStartupPage());
        }
    }

    private boolean isPageVisible(PageId pageId) {
        if (!appContext.onboardingCompleted()) {
            return pageId == PageId.ONBOARDING;
        }

        UserProfile profile = appContext.getCurrentUser();
        if (profile == null) {
            return pageId == PageId.ONBOARDING;
        }

        return switch (pageId) {
            case FAMILY -> profile.isFamilyModuleEnabled();
            case INVESTMENTS -> profile.isInvestmentsModuleEnabled();
            case ACHIEVEMENTS -> profile.isAchievementsModuleEnabled();
            default -> true;
        };
    }

    private PageId defaultStartupPage() {
        return appContext.onboardingCompleted() ? PageId.DASHBOARD : PageId.ONBOARDING;
    }
}
