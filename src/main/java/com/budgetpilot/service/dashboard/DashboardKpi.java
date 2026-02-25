package com.budgetpilot.service.dashboard;

import java.util.Objects;

public class DashboardKpi {
    private final String key;
    private final String title;
    private final String valueText;
    private final String subtext;

    public DashboardKpi(String key, String title, String valueText, String subtext) {
        this.key = Objects.requireNonNull(key, "key");
        this.title = Objects.requireNonNull(title, "title");
        this.valueText = Objects.requireNonNull(valueText, "valueText");
        this.subtext = subtext == null ? "" : subtext;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getValueText() {
        return valueText;
    }

    public String getSubtext() {
        return subtext;
    }
}
