package com.budgetpilot.ui.components;

import com.budgetpilot.service.insights.InsightItem;
import com.budgetpilot.service.insights.InsightLevel;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class InsightListCard extends VBox {
    public InsightListCard() {
        setSpacing(10);
    }

    public void setInsights(List<InsightItem> insights) {
        setInsights(insights, null);
    }

    public void setInsights(List<InsightItem> insights, Consumer<InsightItem> actionHandler) {
        getChildren().clear();
        if (insights == null || insights.isEmpty()) {
            getChildren().add(new Label("No critical insights. You're on track."));
            return;
        }
        for (InsightItem insight : insights) {
            getChildren().add(buildItem(insight, actionHandler));
        }
    }

    private VBox buildItem(InsightItem insight, Consumer<InsightItem> actionHandler) {
        StatusBadge badge = new StatusBadge();
        badge.setStatus(statusFromLevel(insight.getLevel()));
        badge.setText(formatLevel(insight.getLevel()));

        Label titleLabel = new Label(insight.getTitle());
        titleLabel.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, titleLabel, spacer, badge);
        head.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(insight.getMessage());
        messageLabel.getStyleClass().add("muted-text");
        messageLabel.setWrapText(true);

        Label sourceLabel = new Label("Source: " + formatSource(insight.getSource()));
        sourceLabel.getStyleClass().add("muted-text");

        VBox item = new VBox(6, head, messageLabel, sourceLabel);
        item.getStyleClass().add("alert-item");

        if (actionHandler != null && insight.getActionTarget() != null && !insight.getActionLabel().isBlank()) {
            Button actionButton = new Button(insight.getActionLabel());
            actionButton.getStyleClass().addAll("secondary-button", "btn-secondary", "btn-small");
            actionButton.setOnAction(event -> actionHandler.accept(insight));
            item.getChildren().add(actionButton);
        }

        return item;
    }

    private String statusFromLevel(InsightLevel level) {
        return switch (level) {
            case DANGER -> "danger";
            case WARNING -> "warn";
            case SUCCESS -> "good";
            case INFO -> "info";
        };
    }

    private String formatLevel(InsightLevel level) {
        return switch (level) {
            case DANGER -> "Danger";
            case WARNING -> "Warning";
            case SUCCESS -> "Success";
            case INFO -> "Info";
        };
    }

    private String formatSource(String source) {
        if (source == null || source.isBlank()) {
            return "General";
        }
        String normalized = source.trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
