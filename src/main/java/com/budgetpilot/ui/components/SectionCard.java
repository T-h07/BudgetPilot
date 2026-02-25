package com.budgetpilot.ui.components;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SectionCard extends VBox {
    public SectionCard(String title, String subtitle) {
        this(title, subtitle, null);
    }

    public SectionCard(String title, String subtitle, Node body) {
        setSpacing(12);
        setPadding(new Insets(18));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("muted-text");
            subtitleLabel.setWrapText(true);
            getChildren().add(subtitleLabel);
        }

        if (body != null) {
            VBox.setVgrow(body, Priority.ALWAYS);
            getChildren().add(body);
        }
    }
}
