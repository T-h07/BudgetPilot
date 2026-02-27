package com.budgetpilot.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UiUtils {
    public static final Insets PAGE_PADDING = new Insets(28, 30, 30, 30);
    public static final double SECTION_SPACING = 20;
    public static final double CARD_GAP = 16;

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.getDefault());

    private UiUtils() {
    }

    public static VBox createPageHeader(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(6, title, subtitle);
        return header;
    }

    public static Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-text");
        label.setWrapText(true);
        return label;
    }

    public static VBox createInfoLines(String... lines) {
        VBox infoBox = new VBox(8);
        for (String line : lines) {
            infoBox.getChildren().add(createMutedLabel(line));
        }
        return infoBox;
    }

    public static HBox createTwoColumn(Node left, Node right) {
        HBox row = new HBox(CARD_GAP, left, right);
        if (left instanceof Region leftRegion) {
            leftRegion.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(leftRegion, Priority.ALWAYS);
        }
        if (right instanceof Region rightRegion) {
            rightRegion.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rightRegion, Priority.ALWAYS);
        }
        return row;
    }

    public static ScrollPane createPageScroll(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        return scrollPane;
    }

    public static String formatMonth(YearMonth month) {
        return month == null ? "" : month.format(MONTH_FORMATTER);
    }
}
