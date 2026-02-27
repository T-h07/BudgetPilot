package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.ClosePath;

public class MiniDensityCurve extends Pane {
    private final CubicCurve curve = new CubicCurve();
    private final Path fillPath = new Path();

    public MiniDensityCurve() {
        setMinSize(84, 36);
        setPrefSize(84, 36);
        setMaxSize(84, 36);
        getStyleClass().add("mini-density");

        curve.setStartX(2);
        curve.setEndX(82);
        curve.setStartY(30);
        curve.setEndY(30);
        curve.setControlX1(26);
        curve.setControlX2(58);
        curve.setFill(Color.TRANSPARENT);
        curve.setStrokeWidth(2.2);

        getChildren().addAll(fillPath, curve);
    }

    public void setData(double intensity, KpiStatus status) {
        double clamped = Math.max(0d, Math.min(1d, intensity));
        double peakY = 30d - (clamped * 20d);
        Color color = KpiColorPalette.statusColor(status);

        curve.setControlY1(peakY);
        curve.setControlY2(peakY);
        curve.setStroke(color);

        fillPath.getElements().setAll(
                new MoveTo(2, 30),
                new LineTo(2, 30),
                new LineTo(26, peakY),
                new LineTo(58, peakY),
                new LineTo(82, 30),
                new LineTo(82, 34),
                new LineTo(2, 34),
                new ClosePath()
        );
        fillPath.setFill(color.deriveColor(0, 1, 1, 0.24));
        fillPath.setStroke(Color.TRANSPARENT);
    }
}
