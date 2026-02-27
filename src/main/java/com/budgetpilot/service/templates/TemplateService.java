package com.budgetpilot.service.templates;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.ExpenseTemplate;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.IncomeTemplate;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.RecurrenceCadence;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateService {
    private final BudgetStore store;

    public TemplateService(BudgetStore store) {
        this.store = ValidationUtils.requireNonNull(store, "store");
    }

    public List<ExpenseTemplate> listExpenseTemplates() {
        return store.listExpenseTemplates().stream()
                .sorted(Comparator.comparing(ExpenseTemplate::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ExpenseTemplate> listActiveExpenseTemplates() {
        return listExpenseTemplates().stream()
                .filter(ExpenseTemplate::isActive)
                .toList();
    }

    public void saveExpenseTemplate(ExpenseTemplate template) {
        ValidationUtils.requireNonNull(template, "template");
        validateExpenseTemplate(template);
        store.saveExpenseTemplate(template.copy());
    }

    public void deleteExpenseTemplate(String id) {
        store.deleteExpenseTemplate(ValidationUtils.requireNonBlank(id, "id"));
    }

    public List<IncomeTemplate> listIncomeTemplates() {
        return store.listIncomeTemplates().stream()
                .sorted(Comparator.comparing(IncomeTemplate::getSourceName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<IncomeTemplate> listActiveIncomeTemplates() {
        return listIncomeTemplates().stream()
                .filter(IncomeTemplate::isActive)
                .toList();
    }

    public void saveIncomeTemplate(IncomeTemplate template) {
        ValidationUtils.requireNonNull(template, "template");
        validateIncomeTemplate(template);
        store.saveIncomeTemplate(template.copy());
    }

    public void deleteIncomeTemplate(String id) {
        store.deleteIncomeTemplate(ValidationUtils.requireNonBlank(id, "id"));
    }

    public List<ExpenseEntry> generateExpensesForMonth(YearMonth month, List<String> selectedTemplateIds) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        Set<String> templateIds = normalizeTemplateIds(selectedTemplateIds);
        if (templateIds.isEmpty()) {
            return List.of();
        }

        Set<String> existingTemplateIds = store.listExpenseEntries(targetMonth).stream()
                .map(ExpenseEntry::getSourceTemplateId)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ExpenseEntry> generated = listExpenseTemplates().stream()
                .filter(template -> templateIds.contains(template.getId()))
                .filter(ExpenseTemplate::isActive)
                .filter(template -> isDueForMonth(template.getCadence(), targetMonth))
                .filter(template -> MoneyUtils.zeroIfNull(template.getDefaultAmount()).compareTo(BigDecimal.ZERO) > 0)
                .filter(template -> !existingTemplateIds.contains(template.getId()))
                .map(template -> toExpenseEntry(template, targetMonth))
                .toList();

        return List.copyOf(generated);
    }

    public List<IncomeEntry> generateIncomeForMonth(YearMonth month, List<String> selectedTemplateIds) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        Set<String> templateIds = normalizeTemplateIds(selectedTemplateIds);
        if (templateIds.isEmpty()) {
            return List.of();
        }

        Set<String> existingTemplateIds = store.listIncomeEntries(targetMonth).stream()
                .map(IncomeEntry::getSourceTemplateId)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<IncomeEntry> generated = listIncomeTemplates().stream()
                .filter(template -> templateIds.contains(template.getId()))
                .filter(IncomeTemplate::isActive)
                .filter(template -> isDueForMonth(template.getCadence(), targetMonth))
                .filter(template -> MoneyUtils.zeroIfNull(template.getDefaultAmount()).compareTo(BigDecimal.ZERO) > 0)
                .filter(template -> !existingTemplateIds.contains(template.getId()))
                .map(template -> toIncomeEntry(template, targetMonth))
                .toList();

        return List.copyOf(generated);
    }

    public TemplateGenerationResult generateAndSaveForMonth(
            YearMonth month,
            List<String> selectedExpenseTemplateIds,
            List<String> selectedIncomeTemplateIds
    ) {
        List<ExpenseEntry> expenses = generateExpensesForMonth(month, selectedExpenseTemplateIds);
        List<IncomeEntry> incomes = generateIncomeForMonth(month, selectedIncomeTemplateIds);
        for (ExpenseEntry expense : expenses) {
            store.saveExpenseEntry(expense);
        }
        for (IncomeEntry income : incomes) {
            store.saveIncomeEntry(income);
        }
        return new TemplateGenerationResult(expenses.size(), incomes.size());
    }

    private ExpenseEntry toExpenseEntry(ExpenseTemplate template, YearMonth month) {
        ExpenseEntry entry = new ExpenseEntry();
        LocalDateTime now = LocalDateTime.now();
        entry.setMonth(month);
        entry.setExpenseDate(month.atDay(clampDayOfMonth(month, template.getDayOfMonth())));
        entry.setAmount(MoneyUtils.normalize(template.getDefaultAmount()));
        entry.setCategory(template.getCategory());
        entry.setPlannerBucket(template.getPlannerBucket());
        entry.setSubcategory(template.getSubcategory());
        entry.setPaymentMethod(template.getPaymentMethod() == null ? PaymentMethod.CARD : template.getPaymentMethod());
        entry.setRecurring(true);
        entry.setSourceTemplateId(template.getId());
        entry.setTag(template.getTag());
        entry.setNote(template.getNote());
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return entry;
    }

    private IncomeEntry toIncomeEntry(IncomeTemplate template, YearMonth month) {
        IncomeEntry entry = new IncomeEntry();
        LocalDateTime now = LocalDateTime.now();
        entry.setMonth(month);
        entry.setReceivedDate(month.atDay(clampDayOfMonth(month, template.getDayOfMonth())));
        entry.setSourceName(template.getSourceName());
        entry.setIncomeType(template.getIncomeType());
        entry.setAmount(MoneyUtils.normalize(template.getDefaultAmount()));
        entry.setRecurring(true);
        entry.setSourceTemplateId(template.getId());
        entry.setReceived(false);
        entry.setNotes(template.getNote());
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return entry;
    }

    private int clampDayOfMonth(YearMonth month, int requestedDay) {
        int safeDay = Math.max(1, Math.min(requestedDay, 31));
        return Math.min(safeDay, month.lengthOfMonth());
    }

    private boolean isDueForMonth(RecurrenceCadence cadence, YearMonth month) {
        RecurrenceCadence safeCadence = cadence == null ? RecurrenceCadence.MONTHLY : cadence;
        if (safeCadence == RecurrenceCadence.MONTHLY) {
            return true;
        }
        // BP-PT13 scope: treat non-monthly cadences as monthly template generation placeholders.
        return month != null;
    }

    private Set<String> normalizeTemplateIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateExpenseTemplate(ExpenseTemplate template) {
        template.setName(template.getName());
        template.setPlannerBucket(template.getPlannerBucket());
        template.setCategory(template.getCategory());
        template.setCadence(template.getCadence());
        template.setDefaultAmount(template.getDefaultAmount() == null ? BigDecimal.ZERO : template.getDefaultAmount());
        template.setDayOfMonth(template.getDayOfMonth());
    }

    private void validateIncomeTemplate(IncomeTemplate template) {
        template.setSourceName(template.getSourceName());
        template.setIncomeType(template.getIncomeType());
        template.setCadence(template.getCadence());
        template.setDefaultAmount(template.getDefaultAmount() == null ? BigDecimal.ZERO : template.getDefaultAmount());
        template.setDayOfMonth(template.getDayOfMonth());
    }
}
