package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class MiniBars extends HBox {
    private final List<Rectangle> bars = new ArrayList<>();

    public MiniBars() {
        setSpacing(4);
        setAlignment(Pos.BOTTOM_LEFT);
        setMinSize(84, 36);
        setPrefSize(84, 36);
        setMaxSize(84, 36);
        getStyleClass().add("mini-bars");
    }

    public void setData(List<Double> points, KpiStatus status) {
        List<Double> source = (points == null || points.isEmpty())
                ? List.of(0.3d, 0.42d, 0.38d, 0.54d, 0.46d, 0.5d)
                : points;
        if (bars.size() != source.size()) {
            getChildren().clear();
            bars.clear();
            for (int i = 0; i < source.size(); i++) {
                Rectangle rect = new Rectangle(8, 10);
                rect.setArcWidth(4);
                rect.setArcHeight(4);
                bars.add(rect);
                getChildren().add(rect);
            }
        }

        Color color = KpiColorPalette.statusColor(status);
        for (int i = 0; i < source.size(); i++) {
            Rectangle rect = bars.get(i);
            double h = 6d + (Math.max(0d, Math.min(1d, source.get(i))) * 26d);
            rect.setHeight(h);
            rect.setFill(color.deriveColor(0, 1, 1, 0.82));
            rect.setStroke(Color.TRANSPARENT);
        }
    }
}
