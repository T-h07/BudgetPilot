package com.budgetpilot.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SummaryStatCard extends VBox {
    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label subtitleLabel = new Label();

    public SummaryStatCard() {
        setSpacing(6);
        setPadding(new Insets(14));
        getStyleClass().addAll("card", "summary-stat-card");

        titleLabel.getStyleClass().add("kpi-title");
        valueLabel.getStyleClass().add("kpi-value");
        subtitleLabel.getStyleClass().add("muted-text");

        getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
    }

    public void setValues(String title, String value, String subtitle) {
        titleLabel.setText(title == null ? "" : title);
        valueLabel.setText(value == null ? "" : value);
        subtitleLabel.setText(subtitle == null ? "" : subtitle);
    }
}
