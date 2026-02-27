package com.budgetpilot.service.dashboard;

import java.util.Objects;

public class DashboardKpi {
    private final String key;
    private final String title;
    private final String valueText;
    private final String accentText;
    private final String subtext;
    private final KpiStatus status;
    private final KpiVisualData visualData;

    public DashboardKpi(
            String key,
            String title,
            String valueText,
            String accentText,
            String subtext,
            KpiStatus status,
            KpiVisualData visualData
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.title = Objects.requireNonNull(title, "title");
        this.valueText = Objects.requireNonNull(valueText, "valueText");
        this.accentText = accentText == null ? "" : accentText;
        this.subtext = subtext == null ? "" : subtext;
        this.status = Objects.requireNonNull(status, "status");
        this.visualData = Objects.requireNonNull(visualData, "visualData");
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

    public String getAccentText() {
        return accentText;
    }

    public String getSubtext() {
        return subtext;
    }

    public KpiStatus getStatus() {
        return status;
    }

    public KpiVisualData getVisualData() {
        return visualData;
    }
}
