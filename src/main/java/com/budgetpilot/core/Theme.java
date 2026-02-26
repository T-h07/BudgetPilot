package com.budgetpilot.core;

public enum Theme {
    DARK("Dark", "dark"),
    LIGHT("Light", "light");

    private final String label;
    private final String settingValue;

    Theme(String label, String settingValue) {
        this.label = label;
        this.settingValue = settingValue;
    }

    public String getLabel() {
        return label;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public static Theme fromSettingValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DARK;
        }
        String normalized = rawValue.trim().toLowerCase();
        for (Theme theme : values()) {
            if (theme.settingValue.equals(normalized)) {
                return theme;
            }
        }
        return DARK;
    }

    @Override
    public String toString() {
        return label;
    }
}
