package com.budgetpilot.util;

import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public final class TextFitUtils {
    private TextFitUtils() {
    }

    public static void fitLabelToWidth(
            Label label,
            double maxWidth,
            double maxFont,
            double minFont,
            Font fallbackBaseFont
    ) {
        if (label == null) {
            return;
        }
        String textValue = label.getText();
        if (textValue == null || textValue.isBlank()) {
            return;
        }
        if (maxWidth <= 0) {
            return;
        }

        Font source = label.getFont() != null ? label.getFont() : fallbackBaseFont;
        if (source == null) {
            source = Font.getDefault();
        }
        FontWeight weight = parseWeight(source.getStyle());
        FontPosture posture = parsePosture(source.getStyle());
        String family = source.getFamily();

        Text measure = new Text(textValue);
        double targetMax = Math.max(maxWidth - 4, 24);
        double start = Math.max(minFont, maxFont);

        for (double size = start; size >= minFont; size -= 0.5d) {
            Font candidate = Font.font(family, weight, posture, size);
            measure.setFont(candidate);
            if (measure.getLayoutBounds().getWidth() <= targetMax) {
                label.setFont(candidate);
                label.setScaleX(1d);
                label.setScaleY(1d);
                return;
            }
        }
        Font minCandidate = Font.font(family, weight, posture, minFont);
        label.setFont(minCandidate);
        measure.setFont(minCandidate);
        double widthAtMin = measure.getLayoutBounds().getWidth();
        if (widthAtMin > 0 && widthAtMin > targetMax) {
            double scale = Math.max(0.82d, targetMax / widthAtMin);
            label.setScaleX(scale);
            label.setScaleY(scale);
        } else {
            label.setScaleX(1d);
            label.setScaleY(1d);
        }
    }

    private static FontWeight parseWeight(String style) {
        String normalized = style == null ? "" : style.toLowerCase();
        if (normalized.contains("black")) {
            return FontWeight.BLACK;
        }
        if (normalized.contains("extra bold")) {
            return FontWeight.EXTRA_BOLD;
        }
        if (normalized.contains("bold")) {
            return FontWeight.BOLD;
        }
        if (normalized.contains("medium")) {
            return FontWeight.MEDIUM;
        }
        if (normalized.contains("light")) {
            return FontWeight.LIGHT;
        }
        return FontWeight.NORMAL;
    }

    private static FontPosture parsePosture(String style) {
        String normalized = style == null ? "" : style.toLowerCase();
        return normalized.contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
    }
}
