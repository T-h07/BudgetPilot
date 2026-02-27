package com.budgetpilot.service.retention;

import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.ValidationUtils;

import java.time.YearMonth;

public class DataRetentionService {
    public static final int RETENTION_MONTHS = 12;

    private final BudgetStore store;

    public DataRetentionService(BudgetStore store) {
        this.store = ValidationUtils.requireNonNull(store, "store");
    }

    public void purgeOldMonthData() {
        store.purgeMonthsBefore(resolveCutoffMonth());
    }

    public YearMonth resolveCutoffMonth() {
        return YearMonth.now().minusMonths(RETENTION_MONTHS);
    }
}
