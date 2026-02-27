package com.budgetpilot.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class WizardStepHeader extends VBox {
    private final Label progressLabel = new Label();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();

    public WizardStepHeader() {
        setSpacing(4);
        getStyleClass().add("wizard-header");

        progressLabel.getStyleClass().add("muted-text");
        titleLabel.getStyleClass().add("wizard-step-title");
        subtitleLabel.getStyleClass().add("wizard-step-subtitle");
        subtitleLabel.setWrapText(true);

        getChildren().addAll(progressLabel, titleLabel, subtitleLabel);
    }

    public void update(int stepNumber, int totalSteps, String title, String subtitle) {
        progressLabel.setText("Step " + stepNumber + " of " + totalSteps);
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }
}
