package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;

public class MiniIncomeStackBar extends VBox {
    private final Rectangle track = new Rectangle(16, 44);
    private final Rectangle fill = new Rectangle(16, 0);
    private final Label percentLabel = new Label();

    public MiniIncomeStackBar() {
        setSpacing(4);
        setAlignment(Pos.CENTER);
        getStyleClass().add("mini-income-stack");

        track.setArcWidth(8);
        track.setArcHeight(8);
        track.setFill(KpiColorPalette.mutedTrack());
        track.setStroke(KpiColorPalette.mutedStroke());

        fill.setArcWidth(8);
        fill.setArcHeight(8);
        StackPane bar = new StackPane(track, fill);
        bar.setAlignment(Pos.BOTTOM_CENTER);

        percentLabel.getStyleClass().add("kpi-micro-label");

        getChildren().addAll(bar, percentLabel);
    }

    public void setData(double ratio, KpiStatus status) {
        double clamped = Math.max(0d, Math.min(1d, ratio));
        Color color = KpiColorPalette.statusColor(status);
        fill.setHeight(44d * clamped);
        fill.setFill(color);
        percentLabel.setText(Math.round(clamped * 100d) + "%");
    }
}
