package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.DashboardAlert;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class InsightListCard extends VBox {
    public InsightListCard() {
        setSpacing(10);
    }

    public void setAlerts(List<DashboardAlert> alerts) {
        getChildren().clear();
        if (alerts == null || alerts.isEmpty()) {
            getChildren().add(new Label("Everything looks on track this month."));
            return;
        }
        for (DashboardAlert alert : alerts) {
            getChildren().add(buildItem(alert));
        }
    }

    private VBox buildItem(DashboardAlert alert) {
        StatusBadge badge = new StatusBadge();
        badge.setAlertLevel(alert.getLevel());
        badge.setText(alert.getLevel().name());

        Label titleLabel = new Label(alert.getTitle());
        titleLabel.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, titleLabel, spacer, badge);
        head.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(alert.getMessage());
        messageLabel.getStyleClass().add("muted-text");
        messageLabel.setWrapText(true);

        VBox item = new VBox(6, head, messageLabel);
        item.getStyleClass().add("alert-item");

        if (alert.getActionHint() != null && !alert.getActionHint().isBlank()) {
            Label hintLabel = new Label("Hint: " + alert.getActionHint());
            hintLabel.getStyleClass().add("muted-text");
            hintLabel.setWrapText(true);
            item.getChildren().add(hintLabel);
        }

        return item;
    }
}
