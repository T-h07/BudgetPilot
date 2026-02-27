package com.budgetpilot.service.export;

import com.budgetpilot.util.ValidationUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportResult {
    private final List<Path> filesWritten;
    private final Map<String, Integer> rowCounts;
    private final String message;

    public ExportResult(List<Path> filesWritten, Map<String, Integer> rowCounts, String message) {
        this.filesWritten = List.copyOf(filesWritten == null ? List.of() : filesWritten);
        this.rowCounts = Collections.unmodifiableMap(
                rowCounts == null ? Map.of() : new LinkedHashMap<>(rowCounts)
        );
        this.message = ValidationUtils.requireNonBlank(message, "message");
    }

    public List<Path> getFilesWritten() {
        return filesWritten;
    }

    public Map<String, Integer> getRowCounts() {
        return rowCounts;
    }

    public String getMessage() {
        return message;
    }
}
