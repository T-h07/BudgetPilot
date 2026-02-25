package com.budgetpilot.ui;

import com.budgetpilot.core.PageId;
import com.budgetpilot.ui.components.NavButton;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class Sidebar extends VBox {
    private final Map<PageId, NavButton> navButtons = new EnumMap<>(PageId.class);

    public Sidebar(Consumer<PageId> onNavigate) {
        Objects.requireNonNull(onNavigate, "onNavigate must not be null");

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

        VBox mainNav = new VBox(6);
        createButtons(
                List.of(
                        PageId.DASHBOARD,
                        PageId.EXPENSES,
                        PageId.PLANNER,
                        PageId.SAVINGS,
                        PageId.INVESTMENTS,
                        PageId.GOALS,
                        PageId.INCOME,
                        PageId.FAMILY,
                        PageId.HABITS,
                        PageId.ACHIEVEMENTS
                ),
                onNavigate,
                mainNav
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region divider = new Region();
        divider.getStyleClass().add("separator");
        divider.setPrefHeight(1);

        VBox footerNav = new VBox(6);
        createButtons(List.of(PageId.ONBOARDING, PageId.SETTINGS), onNavigate, footerNav);

        getChildren().addAll(brandBox, mainNav, spacer, divider, footerNav);
    }

    public void setActivePage(PageId activePage) {
        navButtons.forEach((pageId, button) -> button.setActive(pageId == activePage));
    }

    private void createButtons(List<PageId> pages, Consumer<PageId> onNavigate, VBox targetContainer) {
        for (PageId pageId : pages) {
            NavButton button = new NavButton(pageId, () -> onNavigate.accept(pageId));
            navButtons.put(pageId, button);
            targetContainer.getChildren().add(button);
        }
    }
}
