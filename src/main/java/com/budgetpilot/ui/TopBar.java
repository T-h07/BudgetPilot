package com.budgetpilot.ui;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

public class TopBar extends HBox {
    private final Label pageTitleLabel = new Label();

    public TopBar(AppContext appContext) {
        Objects.requireNonNull(appContext, "appContext must not be null");

        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(16, 24, 16, 24));
        getStyleClass().add("topbar");

        pageTitleLabel.getStyleClass().add("topbar-page-title");

        Label appTagline = new Label("Monthly financial operating system");
        appTagline.getStyleClass().add("muted-text");

        VBox leftSection = new VBox(4, pageTitleLabel, appTagline);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label monthChip = new Label();
        monthChip.getStyleClass().add("month-pill");
        monthChip.textProperty().bind(
                Bindings.createStringBinding(
                        appContext::getCurrentMonthDisplayText,
                        appContext.selectedMonthProperty()
                )
        );

        Button quickAddButton = new Button("Quick Add");
        quickAddButton.getStyleClass().add("quick-add-button");
        quickAddButton.setFocusTraversable(false);

        Label profileInitials = new Label();
        profileInitials.getStyleClass().add("profile-initials");
        profileInitials.textProperty().bind(
                Bindings.createStringBinding(
                        appContext::getCurrentUserInitials,
                        appContext.currentUserProperty()
                )
        );

        StackPane profileBadge = new StackPane(profileInitials);
        profileBadge.getStyleClass().add("profile-badge");
        profileBadge.setPrefSize(36, 36);

        getChildren().addAll(leftSection, spacer, monthChip, quickAddButton, profileBadge);
    }

    public void setActivePage(PageId pageId) {
        if (pageId != null) {
            pageTitleLabel.setText(pageId.getDisplayLabel());
        }
    }
}
