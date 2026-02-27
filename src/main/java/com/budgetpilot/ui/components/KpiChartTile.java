package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.DashboardKpi;
import com.budgetpilot.service.dashboard.KpiStatus;
import com.budgetpilot.service.dashboard.KpiVisualData;
import com.budgetpilot.service.dashboard.KpiVisualType;
import com.budgetpilot.util.TextFitUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.shape.Circle;

public class KpiChartTile extends VBox {
    private static final double TILE_HEIGHT = 154;
    private static final double H_PADDING = 14;
    private static final double V_PADDING = 12;
    private static final double CENTER_GAP = 8;
    private static final double VISUAL_WIDTH = 84;
    private static final double VISUAL_HEIGHT = 52;
    private static final double VALUE_MAX_FONT = 24;
    private static final double VALUE_MIN_FONT = 14;

    private final Label titleLabel = new Label();
    private final Circle statusDot = new Circle(4.2);
    private final Label valueLabel = new Label();
    private final Label accentLabel = new Label();
    private final Label subtextLabel = new Label();
    private final VBox valueStack = new VBox(3);
    private final HBox midRow = new HBox(CENTER_GAP);
    private final HBox topRow = new HBox(8);
    private final StackPane visualHost = new StackPane();
    private final Font initialValueFont;

    public KpiChartTile() {
        setSpacing(8);
        setPadding(new Insets(V_PADDING, H_PADDING, V_PADDING, H_PADDING));
        setMinHeight(TILE_HEIGHT);
        setPrefHeight(TILE_HEIGHT);
        setMaxHeight(TILE_HEIGHT);
        getStyleClass().addAll("card", "kpi-tile", "kpi-chart-tile");

        titleLabel.getStyleClass().add("kpi-title");
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setWrapText(false);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        statusDot.getStyleClass().add("kpi-status-dot");

        valueLabel.getStyleClass().add("kpi-value");
        valueLabel.setTextOverrun(OverrunStyle.CLIP);
        valueLabel.setWrapText(false);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        initialValueFont = valueLabel.getFont();

        accentLabel.getStyleClass().add("kpi-secondary");
        accentLabel.setWrapText(false);
        accentLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        accentLabel.setMaxWidth(Double.MAX_VALUE);
        accentLabel.setMinHeight(14);
        accentLabel.setPrefHeight(14);
        accentLabel.setMaxHeight(14);

        subtextLabel.getStyleClass().add("kpi-subtext");
        subtextLabel.setWrapText(true);
        subtextLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        applySubtextClamp();
        subtextLabel.maxWidthProperty().bind(widthProperty().subtract(H_PADDING * 2 + 2));

        visualHost.getStyleClass().add("kpi-visual-host");
        visualHost.setMinSize(VISUAL_WIDTH, VISUAL_HEIGHT);
        visualHost.setPrefSize(VISUAL_WIDTH, VISUAL_HEIGHT);
        visualHost.setMaxSize(VISUAL_WIDTH, VISUAL_HEIGHT);

        topRow.getChildren().addAll(titleLabel, spacer(), statusDot);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setMinHeight(20);
        topRow.setPrefHeight(20);
        topRow.setMaxHeight(20);

        valueStack.getChildren().addAll(valueLabel, accentLabel);
        valueStack.setFillWidth(true);
        valueStack.setMaxWidth(Double.MAX_VALUE);
        valueStack.setMinHeight(VISUAL_HEIGHT);
        valueStack.setPrefHeight(VISUAL_HEIGHT);
        valueStack.setMaxHeight(VISUAL_HEIGHT);
        valueStack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valueStack, Priority.ALWAYS);
        accentLabel.maxWidthProperty().bind(valueStack.widthProperty());

        midRow.getChildren().addAll(valueStack, spacer(), visualHost);
        midRow.setAlignment(Pos.CENTER_LEFT);
        midRow.setMinHeight(VISUAL_HEIGHT);
        midRow.setPrefHeight(VISUAL_HEIGHT);
        midRow.setMaxHeight(VISUAL_HEIGHT);

        getChildren().addAll(topRow, midRow, subtextLabel);

        widthProperty().addListener((obs, oldWidth, newWidth) -> updateLayoutMetrics());
        valueStack.widthProperty().addListener((obs, oldWidth, newWidth) -> updateLayoutMetrics());
        valueLabel.textProperty().addListener((obs, oldValue, newValue) -> updateLayoutMetrics());
        sceneProperty().addListener((obs, oldScene, newScene) -> updateLayoutMetrics());
    }

    public void setContent(DashboardKpi kpi) {
        titleLabel.setText(kpi == null ? "" : kpi.getTitle());
        valueLabel.setText(kpi == null ? "" : kpi.getValueText());
        String accent = kpi == null ? "" : kpi.getAccentText();
        accentLabel.setText(accent == null || accent.isBlank() ? " " : accent);
        accentLabel.setManaged(true);
        accentLabel.setVisible(true);
        subtextLabel.setText(kpi == null ? "" : kpi.getSubtext());

        KpiStatus status = kpi == null ? KpiStatus.WARNING : kpi.getStatus();
        applyStatus(status);
        renderVisual(kpi == null ? null : kpi.getVisualData());
        updateLayoutMetrics();
    }

    private void applyStatus(KpiStatus status) {
        getStyleClass().removeAll("kpi-good", "kpi-warn", "kpi-danger");
        valueLabel.getStyleClass().removeAll("kpi-good", "kpi-warn", "kpi-danger");
        accentLabel.getStyleClass().removeAll("kpi-good", "kpi-warn", "kpi-danger");
        switch (status) {
            case GOOD -> {
                getStyleClass().add("kpi-good");
                valueLabel.getStyleClass().add("kpi-good");
            }
            case WARNING -> {
                getStyleClass().add("kpi-warn");
                valueLabel.getStyleClass().add("kpi-warn");
            }
            case DANGER -> {
                getStyleClass().add("kpi-danger");
                valueLabel.getStyleClass().add("kpi-danger");
            }
        }
        statusDot.setFill(KpiColorPalette.statusColor(status));
        accentLabel.setTextFill(KpiColorPalette.statusColor(status));
    }

    private void renderVisual(KpiVisualData visualData) {
        visualHost.getChildren().clear();
        if (visualData == null) {
            return;
        }
        Node node = createVisualNode(visualData);
        if (node != null) {
            visualHost.getChildren().add(node);
        }
    }

    private Node createVisualNode(KpiVisualData data) {
        KpiVisualType type = data.getVisualType();
        switch (type) {
            case BATTERY_BAR -> {
                MiniBatteryBar batteryBar = new MiniBatteryBar();
                batteryBar.setData(data.getPrimaryRatio(), data.getStatus());
                return fitVisualNode(batteryBar);
            }
            case DENSITY_CURVE -> {
                MiniDensityCurve densityCurve = new MiniDensityCurve();
                densityCurve.setData(data.getPrimaryRatio(), data.getStatus());
                return fitVisualNode(densityCurve);
            }
            case STACKED_INCOME_BAR -> {
                MiniIncomeStackBar stackBar = new MiniIncomeStackBar();
                stackBar.setData(data.getPrimaryRatio(), data.getStatus());
                return fitVisualNode(stackBar);
            }
            case TREND_ARROWS -> {
                MiniTrendArrows arrows = new MiniTrendArrows();
                arrows.setData(data.getTrendDirection(), data.getStatus());
                return fitVisualNode(arrows);
            }
            case PROGRESS_RING -> {
                MiniProgressRing ring = new MiniProgressRing();
                ring.setData(data.getPrimaryRatio(), data.getStatus());
                return fitVisualNode(ring);
            }
            case SPARKLINE -> {
                MiniSparkline sparkline = new MiniSparkline();
                sparkline.setData(data.getSparkPoints(), data.getStatus());
                return fitVisualNode(sparkline);
            }
            case MICRO_BARS -> {
                MiniBars bars = new MiniBars();
                bars.setData(data.getSparkPoints(), data.getStatus());
                return fitVisualNode(bars);
            }
            case GAUGE -> {
                MiniGauge gauge = new MiniGauge();
                gauge.setData(data.getPrimaryRatio(), data.getStatus());
                return fitVisualNode(gauge);
            }
            default -> {
                return null;
            }
        }
    }

    private Node fitVisualNode(Node node) {
        if (node instanceof Region region) {
            region.setMinSize(VISUAL_WIDTH, VISUAL_HEIGHT);
            region.setPrefSize(VISUAL_WIDTH, VISUAL_HEIGHT);
            region.setMaxSize(VISUAL_WIDTH, VISUAL_HEIGHT);
        }
        return node;
    }

    private void updateLayoutMetrics() {
        double available = valueStack.getWidth();
        if (available <= 0) {
            double total = getWidth();
            if (total > 0) {
                available = total - (H_PADDING * 2) - VISUAL_WIDTH - CENTER_GAP - 2;
            }
        }
        TextFitUtils.fitLabelToWidth(valueLabel, available, VALUE_MAX_FONT, VALUE_MIN_FONT, initialValueFont);
        applySubtextClamp();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateLayoutMetrics();
    }

    private void applySubtextClamp() {
        double fontSize = subtextLabel.getFont() == null ? 11 : subtextLabel.getFont().getSize();
        double lineHeight = fontSize * 1.35;
        double maxHeight = lineHeight * 2;
        subtextLabel.setMinHeight(0);
        subtextLabel.setPrefHeight(maxHeight);
        subtextLabel.setMaxHeight(maxHeight);
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
