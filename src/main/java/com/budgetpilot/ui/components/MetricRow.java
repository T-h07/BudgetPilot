package com.budgetpilot.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class MetricRow extends HBox {
    private final Label label = new Label();
    private final Label valueLabel = new Label();

    public MetricRow(String labelText, String valueText) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        getStyleClass().add("metric-row");

        label.getStyleClass().add("muted-text");
        valueLabel.getStyleClass().add("info-row-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(label, spacer, valueLabel);
        setValues(labelText, valueText);
    }

    public void setValues(String labelText, String valueText) {
        label.setText(labelText == null ? "" : labelText);
        valueLabel.setText(valueText == null ? "" : valueText);
    }
}
