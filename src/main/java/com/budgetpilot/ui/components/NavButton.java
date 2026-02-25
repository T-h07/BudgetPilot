package com.budgetpilot.ui.components;

import com.budgetpilot.core.PageId;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;

import java.util.Objects;

public class NavButton extends Button {
    private final PageId pageId;

    public NavButton(PageId pageId, Runnable onNavigate) {
        super(Objects.requireNonNull(pageId, "pageId must not be null").getDisplayLabel());
        this.pageId = pageId;
        Runnable safeNavigate = Objects.requireNonNull(onNavigate, "onNavigate must not be null");

        getStyleClass().add("nav-button");
        setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(false);
        setOnAction(event -> safeNavigate.run());
    }

    public PageId getPageId() {
        return pageId;
    }

    public void setActive(boolean active) {
        ObservableList<String> styleClasses = getStyleClass();
        if (active) {
            if (!styleClasses.contains("nav-button-active")) {
                styleClasses.add("nav-button-active");
            }
        } else {
            styleClasses.remove("nav-button-active");
        }
    }
}
