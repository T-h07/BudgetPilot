package com.budgetpilot.service.month;

import com.budgetpilot.util.ValidationUtils;

public class ExpenseTemplateSelection {
    private final ExpenseTemplateCandidate candidate;
    private final boolean markRecurring;

    public ExpenseTemplateSelection(ExpenseTemplateCandidate candidate, boolean markRecurring) {
        this.candidate = ValidationUtils.requireNonNull(candidate, "candidate");
        this.markRecurring = markRecurring;
    }

    public ExpenseTemplateCandidate getCandidate() {
        return candidate;
    }

    public boolean isMarkRecurring() {
        return markRecurring;
    }
}
