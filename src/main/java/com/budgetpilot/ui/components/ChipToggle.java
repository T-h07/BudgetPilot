package com.budgetpilot.ui.components;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.ToggleButton;

public class ChipToggle extends ToggleButton {
    public ChipToggle(String text) {
        super(text);
        getStyleClass().add("chip");
        setFocusTraversable(false);

        ChangeListener<Boolean> selectedListener = (obs, wasSelected, isSelected) -> {
            if (isSelected) {
                if (!getStyleClass().contains("chip-selected")) {
                    getStyleClass().add("chip-selected");
                }
            } else {
                getStyleClass().remove("chip-selected");
            }
        };
        selectedProperty().addListener(selectedListener);
    }
}
