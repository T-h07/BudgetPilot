package com.budgetpilot.service.export;

public enum ExportFormat {
    CSV("CSV"),
    JSON("JSON");

    private final String label;

    ExportFormat(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
