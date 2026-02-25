package com.budgetpilot.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FamilyValidationResult {
    private final List<String> errors = new ArrayList<>();

    public void addError(String error) {
        if (error != null && !error.isBlank()) {
            errors.add(error.trim());
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getPrimaryError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
}
