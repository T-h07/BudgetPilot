package com.budgetpilot.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardKpiTile extends VBox {
    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label subtextLabel = new Label();

    public DashboardKpiTile() {
        setSpacing(6);
        setPadding(new Insets(14));
        getStyleClass().addAll("card", "dashboard-kpi-tile");

        titleLabel.getStyleClass().add("kpi-title");
        valueLabel.getStyleClass().add("kpi-value");
        subtextLabel.getStyleClass().add("muted-text");
        subtextLabel.setWrapText(true);

        getChildren().addAll(titleLabel, valueLabel, subtextLabel);
    }

    public void setContent(String title, String value, String subtext) {
        titleLabel.setText(title == null ? "" : title);
        valueLabel.setText(value == null ? "" : value);
        subtextLabel.setText(subtext == null ? "" : subtext);
    }
}
