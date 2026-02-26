package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MiniBatteryBar extends StackPane {
    private final Rectangle track = new Rectangle();
    private final Rectangle fill = new Rectangle();

    public MiniBatteryBar() {
        setMinSize(78, 22);
        setPrefSize(78, 22);
        setMaxSize(78, 22);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("mini-battery");

        track.setArcWidth(10);
        track.setArcHeight(10);
        track.setWidth(78);
        track.setHeight(10);
        track.setFill(KpiColorPalette.mutedTrack());
        track.setStroke(KpiColorPalette.mutedStroke());

        fill.setArcWidth(10);
        fill.setArcHeight(10);
        fill.setHeight(10);

        getChildren().addAll(track, fill);
    }

    public void setData(double ratio, KpiStatus status) {
        double clamped = Math.max(0d, Math.min(1d, ratio));
        Color color = KpiColorPalette.statusColor(status);
        fill.setWidth(78d * clamped);
        fill.setFill(color);
    }
}
