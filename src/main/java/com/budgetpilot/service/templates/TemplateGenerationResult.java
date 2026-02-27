package com.budgetpilot.service.templates;

public class TemplateGenerationResult {
    private final int generatedExpenseCount;
    private final int generatedIncomeCount;

    public TemplateGenerationResult(int generatedExpenseCount, int generatedIncomeCount) {
        this.generatedExpenseCount = Math.max(0, generatedExpenseCount);
        this.generatedIncomeCount = Math.max(0, generatedIncomeCount);
    }

    public int getGeneratedExpenseCount() {
        return generatedExpenseCount;
    }

    public int getGeneratedIncomeCount() {
        return generatedIncomeCount;
    }
}
