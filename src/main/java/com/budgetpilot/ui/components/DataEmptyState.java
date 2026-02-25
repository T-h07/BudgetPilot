package com.budgetpilot.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DataEmptyState extends VBox {
    public DataEmptyState(String title, String message) {
        setSpacing(8);
        setPadding(new Insets(16));
        getStyleClass().addAll("card", "empty-state");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("muted-text");
        messageLabel.setWrapText(true);

        getChildren().addAll(titleLabel, messageLabel);
    }
}
