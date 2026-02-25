package com.budgetpilot.ui.components;

import com.budgetpilot.service.dashboard.AlertLevel;
import javafx.scene.control.Label;

public class StatusBadge extends Label {
    public StatusBadge() {
        getStyleClass().add("status-badge");
    }

    public void setAlertLevel(AlertLevel level) {
        if (level == null) {
            setStatus("info");
            return;
        }
        switch (level) {
            case DANGER -> setStatus("danger");
            case WARNING -> setStatus("warn");
            case INFO -> setStatus("info");
        }
    }

    public void setStatus(String status) {
        getStyleClass().removeAll("badge-info", "badge-warn", "badge-danger", "badge-good");
        switch (status == null ? "" : status.toLowerCase()) {
            case "danger" -> getStyleClass().add("badge-danger");
            case "warn", "warning", "caution" -> getStyleClass().add("badge-warn");
            case "good", "healthy" -> getStyleClass().add("badge-good");
            default -> getStyleClass().add("badge-info");
        }
    }
}
