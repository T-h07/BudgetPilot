package com.budgetpilot.ui;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.ui.components.NavButton;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class Sidebar extends VBox {
    private final AppContext appContext;
    private final Consumer<PageId> onNavigate;
    private final Map<PageId, NavButton> navButtons = new EnumMap<>(PageId.class);
    private final VBox mainNav = new VBox(6);
    private final VBox footerNav = new VBox(6);

    private PageId activePage;

    public Sidebar(AppContext appContext, Consumer<PageId> onNavigate) {
        this.appContext = Objects.requireNonNull(appContext, "appContext must not be null");
        this.onNavigate = Objects.requireNonNull(onNavigate, "onNavigate must not be null");

        setPrefWidth(250);
        setMinWidth(220);
        setSpacing(14);
        setPadding(new Insets(22, 16, 22, 16));
        getStyleClass().add("sidebar");

        Label appTitle = new Label("BudgetPilot");
        appTitle.getStyleClass().add("sidebar-title");

        Label appSubtitle = new Label("Financial cockpit");
        appSubtitle.getStyleClass().add("muted-text");

        VBox brandBox = new VBox(4, appTitle, appSubtitle);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region divider = new Region();
        divider.getStyleClass().add("separator");
        divider.setPrefHeight(1);

        getChildren().addAll(brandBox, mainNav, spacer, divider, footerNav);

        refreshNavigation();
    }

    public void refreshNavigation() {
        PageId currentActive = activePage;
        navButtons.clear();
        mainNav.getChildren().clear();
        footerNav.getChildren().clear();

        for (PageId pageId : buildMainPages()) {
            NavButton button = createButton(pageId);
            mainNav.getChildren().add(button);
        }

        footerNav.getChildren().add(createButton(PageId.SETTINGS));
        setActivePage(currentActive);
    }

    public void setActivePage(PageId activePage) {
        this.activePage = activePage;
        navButtons.forEach((pageId, button) -> button.setActive(pageId == activePage));
    }

    private NavButton createButton(PageId pageId) {
        NavButton button = new NavButton(pageId, () -> onNavigate.accept(pageId));
        navButtons.put(pageId, button);
        return button;
    }

    private List<PageId> buildMainPages() {
        List<PageId> pages = new ArrayList<>(List.of(
                PageId.DASHBOARD,
                PageId.ANALYTICS,
                PageId.INSIGHTS,
                PageId.EXPENSES,
                PageId.PLANNER,
                PageId.SAVINGS,
                PageId.GOALS,
                PageId.INCOME,
                PageId.TEMPLATES,
                PageId.HABITS
        ));

        UserProfile profile = appContext.getCurrentUser();
        if (profile != null) {
            if (profile.isFamilyModuleEnabled()) {
                pages.add(PageId.FAMILY);
            }
            if (profile.isInvestmentsModuleEnabled()) {
                pages.add(PageId.INVESTMENTS);
            }
            if (profile.isAchievementsModuleEnabled()) {
                pages.add(PageId.ACHIEVEMENTS);
            }
        }

        return pages;
    }
}
