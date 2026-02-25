package com.budgetpilot.service;

import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class IncomeService {
    private final BudgetStore budgetStore;

    public IncomeService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
    }

    public List<IncomeEntry> listForMonth(YearMonth month) {
        return budgetStore.listIncomeEntries(ValidationUtils.requireNonNull(month, "month"));
    }

    public void saveIncome(IncomeEntry entry) {
        IncomeValidationResult validation = validate(entry);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }
        budgetStore.saveIncomeEntry(entry);
    }

    public void deleteIncome(String id) {
        budgetStore.deleteIncomeEntry(ValidationUtils.requireNonBlank(id, "id"));
    }

    public BigDecimal getPlannedIncomeTotal(YearMonth month) {
        return MoneyUtils.normalize(listForMonth(month).stream()
                .map(IncomeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getReceivedIncomeTotal(YearMonth month) {
        return MoneyUtils.normalize(listForMonth(month).stream()
                .filter(IncomeEntry::isReceived)
                .map(IncomeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getRecurringIncomeTotal(YearMonth month) {
        return MoneyUtils.normalize(listForMonth(month).stream()
                .filter(IncomeEntry::isRecurring)
                .map(IncomeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public IncomeValidationResult validate(IncomeEntry entry) {
        IncomeValidationResult result = new IncomeValidationResult();
        if (entry == null) {
            result.addError("Income entry is required.");
            return result;
        }

        if (entry.getMonth() == null) {
            result.addError("Month is required.");
        }
        if (entry.getReceivedDate() == null) {
            result.addError("Received date is required.");
        }
        if (entry.getSourceName() == null || entry.getSourceName().isBlank()) {
            result.addError("Income source is required.");
        }
        if (entry.getIncomeType() == null) {
            result.addError("Income type is required.");
        }
        if (entry.getAmount() == null) {
            result.addError("Amount is required.");
        } else if (entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Amount must be greater than 0.");
        }
        return result;
    }
}
