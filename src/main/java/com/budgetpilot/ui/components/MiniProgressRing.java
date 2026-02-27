package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;

public class MiniProgressRing extends StackPane {
    private static final double DEFAULT_SIZE = 48d;
    private static final double STROKE_WIDTH = 4d;
    private static final double RADIUS_INSET = 2d;

    private final Arc track = new Arc();
    private final Arc progress = new Arc();
    private final DoubleBinding ringRadius = Bindings.createDoubleBinding(
            () -> {
                double min = Math.min(getWidth(), getHeight());
                double strokeHalf = Math.max(track.getStrokeWidth(), progress.getStrokeWidth()) / 2d;
                return Math.max(0d, (min / 2d) - strokeHalf - RADIUS_INSET);
            },
            widthProperty(),
            heightProperty(),
            track.strokeWidthProperty(),
            progress.strokeWidthProperty()
    );

    public MiniProgressRing() {
        setMinSize(0d, 0d);
        setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getStyleClass().add("mini-ring");

        track.setType(ArcType.OPEN);
        track.setStartAngle(90d);
        track.setLength(-360d);
        track.setStrokeWidth(STROKE_WIDTH);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(KpiColorPalette.mutedTrack());

        progress.setType(ArcType.OPEN);
        progress.setStartAngle(90d);
        progress.setLength(0d);
        progress.setStrokeWidth(STROKE_WIDTH);
        progress.setFill(Color.TRANSPARENT);
        progress.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        bindArcGeometry(track);
        bindArcGeometry(progress);

        getChildren().addAll(track, progress);
    }

    public void setData(double ratio, KpiStatus status) {
        double clamped = Math.max(0d, Math.min(1d, ratio));
        double sweep = 360d * clamped;
        // Keep short arcs centered at the top so low-progress states do not look shifted.
        progress.setStartAngle(90d + (sweep / 2d));
        progress.setLength(-sweep);
        progress.setStroke(KpiColorPalette.statusColor(status));
    }

    private void bindArcGeometry(Arc arc) {
        arc.centerXProperty().bind(widthProperty().divide(2d));
        arc.centerYProperty().bind(heightProperty().divide(2d));
        arc.radiusXProperty().bind(ringRadius);
        arc.radiusYProperty().bind(ringRadius);
    }
}
