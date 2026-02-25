package com.budgetpilot.service;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExpenseService {
    private static final Set<ExpenseCategory> CATEGORY_ORDER = EnumSet.allOf(ExpenseCategory.class);

    private final BudgetStore budgetStore;

    public ExpenseService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
    }

    public List<ExpenseEntry> listForMonth(YearMonth month) {
        return budgetStore.listExpenseEntries(ValidationUtils.requireNonNull(month, "month"));
    }

    public List<ExpenseEntry> listForMonth(YearMonth month, ExpenseFilter filter) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        if (filter == null) {
            return listForMonth(targetMonth);
        }
        return listForMonth(targetMonth).stream()
                .filter(entry -> matchesFilter(entry, filter))
                .toList();
    }

    public void saveExpense(ExpenseEntry entry) {
        ExpenseValidationResult validation = validate(entry);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        ExpenseEntry normalized = normalizeForSave(entry);
        budgetStore.saveExpenseEntry(normalized);
    }

    public void deleteExpense(String id) {
        budgetStore.deleteExpenseEntry(ValidationUtils.requireNonBlank(id, "id"));
    }

    public BigDecimal getTotalExpenses(YearMonth month) {
        return MoneyUtils.normalize(listForMonth(month).stream()
                .map(ExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getAverageDailySpend(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        BigDecimal total = getTotalExpenses(targetMonth);
        int denominatorDays = MonthUtils.denominatorDaysForDailyAverage(targetMonth);
        if (denominatorDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return MoneyUtils.normalize(total.divide(BigDecimal.valueOf(denominatorDays), 2, RoundingMode.HALF_UP));
    }

    public List<ExpenseCategorySummary> getCategorySummaries(YearMonth month) {
        List<ExpenseEntry> entries = listForMonth(month);
        BigDecimal total = MoneyUtils.normalize(entries.stream()
                .map(ExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<ExpenseCategory, List<ExpenseEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(ExpenseEntry::getCategory));

        List<ExpenseCategorySummary> summaries = new ArrayList<>();
        for (ExpenseCategory category : CATEGORY_ORDER) {
            List<ExpenseEntry> categoryEntries = grouped.get(category);
            if (categoryEntries == null || categoryEntries.isEmpty()) {
                continue;
            }
            BigDecimal categoryTotal = MoneyUtils.normalize(categoryEntries.stream()
                    .map(ExpenseEntry::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal percentOfTotal = total.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : MoneyUtils.normalize(categoryTotal.multiply(MoneyUtils.HUNDRED).divide(total, 2, RoundingMode.HALF_UP));

            summaries.add(new ExpenseCategorySummary(
                    category,
                    categoryTotal,
                    percentOfTotal,
                    categoryEntries.size()
            ));
        }

        summaries.sort(Comparator
                .comparing(ExpenseCategorySummary::getTotal, Comparator.reverseOrder())
                .thenComparing(summary -> summary.getCategory().getLabel()));
        return List.copyOf(summaries);
    }

    public ExpenseSummary getExpenseSummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<ExpenseEntry> entries = listForMonth(targetMonth);
        List<ExpenseCategorySummary> categorySummaries = getCategorySummaries(targetMonth);

        ExpenseCategory topCategory = categorySummaries.isEmpty() ? null : categorySummaries.get(0).getCategory();
        BigDecimal topCategoryAmount = categorySummaries.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : categorySummaries.get(0).getTotal();

        return new ExpenseSummary(
                targetMonth,
                getTotalExpenses(targetMonth),
                getAverageDailySpend(targetMonth),
                entries.size(),
                (int) entries.stream().map(ExpenseEntry::getCategory).distinct().count(),
                topCategory,
                topCategoryAmount
        );
    }

    public ExpenseValidationResult validate(ExpenseEntry entry) {
        ExpenseValidationResult result = new ExpenseValidationResult();
        if (entry == null) {
            result.addError("Expense entry is required.");
            return result;
        }
        if (entry.getMonth() == null) {
            result.addError("Month is required.");
        }
        if (entry.getExpenseDate() == null) {
            result.addError("Date is required.");
        }
        if (entry.getAmount() == null) {
            result.addError("Amount is required.");
        } else if (entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Amount must be greater than 0.");
        }
        if (entry.getCategory() == null) {
            result.addError("Category is required.");
        }
        if (entry.getPaymentMethod() == null) {
            result.addError("Payment method is required.");
        }
        return result;
    }

    private ExpenseEntry normalizeForSave(ExpenseEntry entry) {
        ExpenseEntry copy = entry.copy();
        if (copy.getExpenseDate() != null) {
            copy.setMonth(YearMonth.from(copy.getExpenseDate()));
        }
        copy.setTag(normalizeTag(copy.getTag()));
        return copy;
    }

    private boolean matchesFilter(ExpenseEntry entry, ExpenseFilter filter) {
        if (filter.getCategory() != null && entry.getCategory() != filter.getCategory()) {
            return false;
        }
        if (filter.getPaymentMethod() != null && entry.getPaymentMethod() != filter.getPaymentMethod()) {
            return false;
        }
        String entryTag = entry.getTag() == null ? "" : entry.getTag().trim();
        if (filter.isOnlyTagged() && entryTag.isBlank()) {
            return false;
        }
        if (filter.hasTagText()) {
            String normalizedTagFilter = normalizeTag(filter.getTagText());
            if (!containsIgnoreCase(entryTag, normalizedTagFilter)) {
                return false;
            }
        }
        if (!filter.hasSearchText()) {
            return true;
        }
        String search = filter.getSearchText();
        return containsIgnoreCase(entry.getNote(), search)
                || containsIgnoreCase(entry.getSubcategory(), search)
                || containsIgnoreCase(entryTag, search)
                || containsIgnoreCase(entry.getCategory().getLabel(), search)
                || containsIgnoreCase(entry.getPaymentMethod().getLabel(), search);
    }

    private String normalizeTag(String rawTag) {
        if (rawTag == null) {
            return "";
        }
        String trimmed = rawTag.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.startsWith("#") ? trimmed : "#" + trimmed;
    }

    private boolean containsIgnoreCase(String source, String search) {
        if (source == null || search == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }
}
