package com.budgetpilot.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ToggleCard extends HBox {
    private final CheckBox toggle = new CheckBox();

    public ToggleCard(String title, String description) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        getStyleClass().add("toggle-row");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("muted-text");
        descriptionLabel.setWrapText(true);

        VBox textBox = new VBox(3, titleLabel, descriptionLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(textBox, spacer, toggle);
    }

    public CheckBox getToggle() {
        return toggle;
    }
}
