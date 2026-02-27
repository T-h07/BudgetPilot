package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;

import java.util.List;

public class MiniSparkline extends Pane {
    private final Polyline line = new Polyline();

    public MiniSparkline() {
        setMinSize(84, 36);
        setPrefSize(84, 36);
        setMaxSize(84, 36);
        getStyleClass().add("mini-sparkline");

        line.setStrokeWidth(2.1);
        line.setFill(Color.TRANSPARENT);
        getChildren().add(line);
    }

    public void setData(List<Double> points, KpiStatus status) {
        List<Double> source = (points == null || points.isEmpty())
                ? List.of(0.4d, 0.45d, 0.42d, 0.5d, 0.48d, 0.5d)
                : points;
        int count = source.size();
        if (count == 1) {
            source = List.of(source.get(0), source.get(0));
            count = 2;
        }

        double width = 82d;
        double height = 32d;
        line.getPoints().clear();
        for (int i = 0; i < count; i++) {
            double x = (width / (count - 1)) * i + 1d;
            double y = (1d - clamp(source.get(i))) * height + 2d;
            line.getPoints().addAll(x, y);
        }
        line.setStroke(KpiColorPalette.statusColor(status));
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value));
    }
}
