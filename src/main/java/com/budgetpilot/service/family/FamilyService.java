package com.budgetpilot.service.family;

import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.enums.FamilyExpenseType;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MoneyUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FamilyService {
    private final BudgetStore budgetStore;

    public FamilyService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
    }

    public List<FamilyMember> listMembers() {
        return budgetStore.listFamilyMembers().stream()
                .sorted(Comparator
                        .comparing(FamilyMember::isActive, Comparator.reverseOrder())
                        .thenComparing(FamilyMember::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void saveMember(FamilyMember member) {
        FamilyValidationResult validation = validateMember(member);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }
        budgetStore.saveFamilyMember(member.copy());
    }

    public void deleteMember(String memberId) {
        String targetMemberId = ValidationUtils.requireNonBlank(memberId, "memberId");
        for (FamilyExpenseEntry entry : budgetStore.listFamilyExpenseEntriesForMember(targetMemberId)) {
            budgetStore.deleteFamilyExpenseEntry(entry.getId());
        }
        budgetStore.deleteFamilyMember(targetMemberId);
    }

    public List<FamilyExpenseEntry> listFamilyExpenses(YearMonth month) {
        return budgetStore.listFamilyExpenseEntries(ValidationUtils.requireNonNull(month, "month"));
    }

    public List<FamilyExpenseEntry> listFamilyExpensesForMember(String memberId) {
        return budgetStore.listFamilyExpenseEntriesForMember(
                ValidationUtils.requireNonBlank(memberId, "memberId")
        );
    }

    public List<FamilyExpenseEntry> listFamilyExpensesForMember(String memberId, YearMonth month) {
        return budgetStore.listFamilyExpenseEntriesForMember(
                ValidationUtils.requireNonBlank(memberId, "memberId"),
                ValidationUtils.requireNonNull(month, "month")
        );
    }

    public void saveFamilyExpense(FamilyExpenseEntry entry) {
        FamilyValidationResult validation = validateFamilyExpense(entry);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getPrimaryError());
        }

        FamilyExpenseEntry copy = entry.copy();
        copy.setMonth(YearMonth.from(copy.getExpenseDate()));
        budgetStore.saveFamilyExpenseEntry(copy);
    }

    public void deleteFamilyExpense(String entryId) {
        budgetStore.deleteFamilyExpenseEntry(ValidationUtils.requireNonBlank(entryId, "entryId"));
    }

    public FamilySummary getFamilySummary(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<FamilyExpenseEntry> entries = listFamilyExpenses(targetMonth);

        BigDecimal totalFamilyCosts = sumEntries(entries);
        BigDecimal totalAllowances = sumByType(entries, FamilyExpenseType.ALLOWANCE);
        BigDecimal totalMedical = sumByType(entries, FamilyExpenseType.MEDICAL);
        BigDecimal totalSupport = sumByType(entries, FamilyExpenseType.SUPPORT);
        BigDecimal totalSchool = sumByType(entries, FamilyExpenseType.SCHOOL);
        BigDecimal totalExtra = sumByType(entries, FamilyExpenseType.EXTRA);
        BigDecimal totalEmergency = sumByType(entries, FamilyExpenseType.EMERGENCY);

        List<FamilyMember> members = listMembers();
        int activeMembersCount = (int) members.stream().filter(FamilyMember::isActive).count();
        int memberCount = members.size();

        BigDecimal plannedFamilyBudget = getPlannedFamilyBudget(targetMonth);
        BigDecimal variance = getFamilyBudgetVariance(targetMonth);
        String budgetStatus = buildBudgetStatusMessage(plannedFamilyBudget, variance);

        return new FamilySummary(
                targetMonth,
                totalFamilyCosts,
                totalAllowances,
                totalMedical,
                totalSupport,
                totalSchool,
                totalExtra,
                totalEmergency,
                activeMembersCount,
                memberCount,
                plannedFamilyBudget,
                variance,
                budgetStatus,
                getExpenseTypeSummaries(targetMonth)
        );
    }

    public List<FamilyMemberSummary> getMemberSummaries(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<FamilyMemberSummary> summaries = new ArrayList<>();
        for (FamilyMember member : listMembers()) {
            List<FamilyExpenseEntry> entries = listFamilyExpensesForMember(member.getId(), targetMonth);
            BigDecimal monthlyTotal = sumEntries(entries);
            BigDecimal allowance = sumByType(entries, FamilyExpenseType.ALLOWANCE);
            BigDecimal medical = sumByType(entries, FamilyExpenseType.MEDICAL);
            BigDecimal support = sumByType(entries, FamilyExpenseType.SUPPORT);
            summaries.add(new FamilyMemberSummary(
                    member.getId(),
                    member.getName(),
                    member.getRelationshipType(),
                    monthlyTotal,
                    allowance,
                    medical,
                    support,
                    entries.size(),
                    member.isActive()
            ));
        }
        return List.copyOf(summaries);
    }

    public List<FamilyExpenseSummary> getExpenseTypeSummaries(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        List<FamilyExpenseEntry> entries = listFamilyExpenses(targetMonth);

        Map<FamilyExpenseType, BigDecimal> totals = new EnumMap<>(FamilyExpenseType.class);
        Map<FamilyExpenseType, Integer> counts = new EnumMap<>(FamilyExpenseType.class);
        for (FamilyExpenseType type : FamilyExpenseType.values()) {
            totals.put(type, BigDecimal.ZERO);
            counts.put(type, 0);
        }
        for (FamilyExpenseEntry entry : entries) {
            FamilyExpenseType type = entry.getExpenseType();
            totals.put(type, totals.get(type).add(entry.getAmount()));
            counts.put(type, counts.get(type) + 1);
        }

        List<FamilyExpenseSummary> summaries = new ArrayList<>();
        for (FamilyExpenseType type : FamilyExpenseType.values()) {
            summaries.add(new FamilyExpenseSummary(
                    type,
                    MoneyUtils.normalize(totals.get(type)),
                    counts.get(type)
            ));
        }
        return List.copyOf(summaries);
    }

    public BigDecimal getTotalFamilyCosts(YearMonth month) {
        return sumEntries(listFamilyExpenses(ValidationUtils.requireNonNull(month, "month")));
    }

    public BigDecimal getTotalAllowances(YearMonth month) {
        return sumByType(listFamilyExpenses(ValidationUtils.requireNonNull(month, "month")), FamilyExpenseType.ALLOWANCE);
    }

    public BigDecimal getTotalMedicalCosts(YearMonth month) {
        return sumByType(listFamilyExpenses(ValidationUtils.requireNonNull(month, "month")), FamilyExpenseType.MEDICAL);
    }

    public BigDecimal getTotalSupportCosts(YearMonth month) {
        return sumByType(listFamilyExpenses(ValidationUtils.requireNonNull(month, "month")), FamilyExpenseType.SUPPORT);
    }

    public BigDecimal getPlannedFamilyBudget(YearMonth month) {
        MonthlyPlan plan = budgetStore.getMonthlyPlan(ValidationUtils.requireNonNull(month, "month"));
        return plan == null ? BigDecimal.ZERO.setScale(2) : MoneyUtils.zeroIfNull(plan.getFamilyBudget());
    }

    // Positive means actual family costs exceed planned family budget.
    public BigDecimal getFamilyBudgetVariance(YearMonth month) {
        YearMonth targetMonth = ValidationUtils.requireNonNull(month, "month");
        return MoneyUtils.safeSubtract(getTotalFamilyCosts(targetMonth), getPlannedFamilyBudget(targetMonth));
    }

    public FamilyValidationResult validateMember(FamilyMember member) {
        FamilyValidationResult result = new FamilyValidationResult();
        if (member == null) {
            result.addError("Family member is required.");
            return result;
        }
        if (member.getName() == null || member.getName().isBlank()) {
            result.addError("Member name is required.");
        }
        if (member.getRelationshipType() == null) {
            result.addError("Relationship is required.");
        }
        if (member.getWeeklyAllowance() == null || member.getWeeklyAllowance().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Weekly allowance must be non-negative.");
        }
        if (member.getMonthlyMedicalBudget() == null || member.getMonthlyMedicalBudget().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Monthly medical budget must be non-negative.");
        }
        if (member.getMonthlySupportBudget() == null || member.getMonthlySupportBudget().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Monthly support budget must be non-negative.");
        }
        return result;
    }

    public FamilyValidationResult validateFamilyExpense(FamilyExpenseEntry entry) {
        FamilyValidationResult result = new FamilyValidationResult();
        if (entry == null) {
            result.addError("Family expense entry is required.");
            return result;
        }
        if (entry.getFamilyMemberId() == null || entry.getFamilyMemberId().isBlank()) {
            result.addError("Family member is required.");
        } else if (!memberExists(entry.getFamilyMemberId())) {
            result.addError("Selected family member does not exist.");
        }
        if (entry.getMonth() == null) {
            result.addError("Month is required.");
        }
        if (entry.getExpenseDate() == null) {
            result.addError("Date is required.");
        }
        if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Amount must be greater than 0.");
        }
        if (entry.getExpenseType() == null) {
            result.addError("Expense type is required.");
        }
        return result;
    }

    private boolean memberExists(String memberId) {
        return listMembers().stream().anyMatch(member -> member.getId().equals(memberId));
    }

    private BigDecimal sumEntries(List<FamilyExpenseEntry> entries) {
        return MoneyUtils.normalize(entries.stream()
                .map(FamilyExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumByType(List<FamilyExpenseEntry> entries, FamilyExpenseType type) {
        return MoneyUtils.normalize(entries.stream()
                .filter(entry -> entry.getExpenseType() == type)
                .map(FamilyExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private String buildBudgetStatusMessage(BigDecimal plannedFamilyBudget, BigDecimal variance) {
        if (plannedFamilyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return "No monthly plan found for this month.";
        }
        if (variance.compareTo(BigDecimal.ZERO) > 0) {
            return "Over family budget by " + variance.toPlainString() + ".";
        }
        if (variance.compareTo(BigDecimal.ZERO) < 0) {
            return "Under family budget by " + variance.abs().toPlainString() + ".";
        }
        return "On target with planned family budget.";
    }
}
