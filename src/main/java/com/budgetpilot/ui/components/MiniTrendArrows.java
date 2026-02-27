package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import com.budgetpilot.service.dashboard.TrendDirection;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class MiniTrendArrows extends HBox {
    private final Polygon upArrow = createArrow(true);
    private final Polygon downArrow = createArrow(false);

    public MiniTrendArrows() {
        setSpacing(8);
        setAlignment(Pos.CENTER);
        getStyleClass().add("mini-arrows");
        getChildren().addAll(upArrow, downArrow);
    }

    public void setData(TrendDirection direction, KpiStatus status) {
        TrendDirection trend = direction == null ? TrendDirection.UNKNOWN : direction;
        Color statusColor = KpiColorPalette.statusColor(status);
        Color upActive = trend == TrendDirection.UP
                ? Color.web("#ef6666")
                : Color.web("rgba(255,255,255,0.28)");
        Color downActive = trend == TrendDirection.DOWN
                ? Color.web("#57c678")
                : Color.web("rgba(255,255,255,0.28)");

        if (trend == TrendDirection.FLAT) {
            upActive = Color.web("rgba(255,255,255,0.42)");
            downActive = Color.web("rgba(255,255,255,0.42)");
        } else if (trend == TrendDirection.UNKNOWN) {
            upActive = statusColor.deriveColor(0, 1, 1, 0.42);
            downActive = statusColor.deriveColor(0, 1, 1, 0.42);
        }

        upArrow.setFill(upActive);
        downArrow.setFill(downActive);
    }

    private Polygon createArrow(boolean up) {
        Polygon arrow = up
                ? new Polygon(6.0, 0.0, 12.0, 10.0, 8.5, 10.0, 8.5, 18.0, 3.5, 18.0, 3.5, 10.0, 0.0, 10.0)
                : new Polygon(3.5, 0.0, 8.5, 0.0, 8.5, 8.0, 12.0, 8.0, 6.0, 18.0, 0.0, 8.0, 3.5, 8.0);
        arrow.setStroke(Color.TRANSPARENT);
        return arrow;
    }
}
