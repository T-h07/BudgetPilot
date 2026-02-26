package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.KpiStatus;
import javafx.scene.paint.Color;

public final class KpiColorPalette {
    private KpiColorPalette() {
    }

    public static Color statusColor(KpiStatus status) {
        if (status == null) {
            return Color.web("#9fb0d3");
        }
        return switch (status) {
            case GOOD -> Color.web("#57c678");
            case WARNING -> Color.web("#f6b950");
            case DANGER -> Color.web("#ef6666");
        };
    }

    public static Color mutedTrack() {
        return Color.web("rgba(255,255,255,0.12)");
    }

    public static Color mutedStroke() {
        return Color.web("rgba(255,255,255,0.2)");
    }
}
