package com.budgetpilot.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class KpiTile extends VBox {
    public KpiTile(String title, String value, String subtext) {
        setSpacing(6);
        setPadding(new Insets(16));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().addAll("card", "kpi-tile");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kpi-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");

        getChildren().addAll(titleLabel, valueLabel);

        if (subtext != null && !subtext.isBlank()) {
            Label subtextLabel = new Label(subtext);
            subtextLabel.getStyleClass().add("muted-text");
            subtextLabel.setWrapText(true);
            getChildren().add(subtextLabel);
        }
    }
}
