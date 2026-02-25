package com.budgetpilot.service;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OnboardingService {
    private static final Map<String, BigDecimal> HABIT_LIMITS = new LinkedHashMap<>();

    static {
        HABIT_LIMITS.put("#snacks", new BigDecimal("70"));
        HABIT_LIMITS.put("#clothes", new BigDecimal("120"));
        HABIT_LIMITS.put("#coffee", new BigDecimal("55"));
        HABIT_LIMITS.put("#gaming", new BigDecimal("95"));
        HABIT_LIMITS.put("#fuel", new BigDecimal("140"));
        HABIT_LIMITS.put("#subscriptions", new BigDecimal("90"));
        HABIT_LIMITS.put("#food-delivery", new BigDecimal("110"));
        HABIT_LIMITS.put("#other", new BigDecimal("80"));
    }

    public UserProfile completeOnboarding(AppContext context, OnboardingData data) {
        ValidationUtils.requireNonNull(context, "context");
        ValidationUtils.requireNonNull(data, "data");
        BudgetStore store = ValidationUtils.requireNonNull(context.getStore(), "store");

        validateData(data);

        UserProfile profile = new UserProfile();
        profile.setFirstName(data.getFirstName());
        profile.setLastName(data.getLastName());
        profile.setEmail(ValidationUtils.requireValidEmail(data.getEmail(), "email"));
        profile.setAge(ValidationUtils.parseOptionalNonNegativeInteger(data.getAgeText(), "age"));
        profile.setCurrencyCode(data.getCurrencyCode().isBlank() ? "EUR" : data.getCurrencyCode());
        profile.setProfileType(data.getProfileType());
        profile.setFamilyModuleEnabled(data.getProfileType() == UserProfileType.MAIN_FAMILY_SUPPORTER);
        profile.setInvestmentsModuleEnabled(true);
        profile.setAchievementsModuleEnabled(true);
        store.saveUserProfile(profile);

        saveHabitTemplates(store, data.getSelectedHabitTags());
        saveInitialIncome(store, context.getSelectedMonth(), data.getIncomeInputs());
        saveMonthlyPlan(store, context.getSelectedMonth(), data.getPlanInput(), profile.isFamilyModuleEnabled());

        context.reloadCurrentUserFromStore();
        return context.getCurrentUser();
    }

    private void validateData(OnboardingData data) {
        ValidationUtils.requireNonBlank(data.getFirstName(), "firstName");
        ValidationUtils.requireNonBlank(data.getLastName(), "lastName");
        ValidationUtils.requireValidEmail(data.getEmail(), "email");
        ValidationUtils.parseOptionalNonNegativeInteger(data.getAgeText(), "age");
        ValidationUtils.requireNonNull(data.getProfileType(), "profileType");
        ValidationUtils.requireNonNull(data.getPlanInput(), "planInput");

        if (data.getIncomeInputs().isEmpty()) {
            throw new IllegalArgumentException("At least one income entry is required");
        }

        for (IncomeInput incomeInput : data.getIncomeInputs()) {
            ValidationUtils.requireNonBlank(incomeInput.getSourceName(), "income.sourceName");
            ValidationUtils.requireNonNull(incomeInput.getIncomeType(), "income.incomeType");
            BigDecimal amount = ValidationUtils.requireNonNull(incomeInput.getAmount(), "income.amount");
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("income.amount must be greater than 0");
            }
        }
    }

    private void saveHabitTemplates(BudgetStore store, Set<String> selectedHabitTags) {
        for (HabitRule existing : store.listHabitRules()) {
            store.deleteHabitRule(existing.getId());
        }

        for (String rawTag : selectedHabitTags) {
            if (rawTag == null || rawTag.isBlank()) {
                continue;
            }
            String tag = normalizeTag(rawTag);
            HabitRule rule = new HabitRule();
            rule.setTag(tag);
            rule.setDisplayName(toDisplayName(tag));
            rule.setMonthlyLimit(HABIT_LIMITS.getOrDefault(tag, HABIT_LIMITS.get("#other")));
            rule.setWarningThresholdPercent(new BigDecimal("70"));
            rule.setLinkedCategory(inferCategory(tag));
            rule.setActive(true);
            rule.setNotes("Created during onboarding");
            store.saveHabitRule(rule);
        }
    }

    private void saveInitialIncome(BudgetStore store, YearMonth month, List<IncomeInput> incomeInputs) {
        for (IncomeEntry existing : store.listIncomeEntries(month)) {
            store.deleteIncomeEntry(existing.getId());
        }

        int day = 1;
        for (IncomeInput input : incomeInputs) {
            IncomeEntry entry = new IncomeEntry(
                    month,
                    month.atDay(Math.min(day++, month.lengthOfMonth())),
                    input.getSourceName(),
                    input.getIncomeType(),
                    input.getAmount()
            );
            entry.setRecurring(input.isRecurring());
            entry.setReceived(input.isReceived());
            entry.setNotes("Onboarding setup");
            store.saveIncomeEntry(entry);
        }
    }

    private void saveMonthlyPlan(BudgetStore store, YearMonth month, PlanInput input, boolean familyModuleEnabled) {
        MonthlyPlan plan = store.getMonthlyPlan(month);
        if (plan == null) {
            plan = new MonthlyPlan(month);
        }

        plan.setFixedCostsBudget(input.getFixedCostsBudget());
        plan.setFoodBudget(input.getFoodBudget());
        plan.setTransportBudget(input.getTransportBudget());
        plan.setFamilyBudget(familyModuleEnabled ? input.getFamilyBudget() : BigDecimal.ZERO);
        plan.setDiscretionaryBudget(input.getDiscretionaryBudget());
        plan.setSavingsPercent(input.getSavingsPercent());
        plan.setGoalsPercent(input.getGoalsPercent());
        plan.setSafetyBufferAmount(input.getSafetyBufferAmount());
        plan.setNotes(input.getNotes());
        store.saveMonthlyPlan(plan);
    }

    private String normalizeTag(String rawTag) {
        String trimmed = rawTag.trim().toLowerCase();
        return trimmed.startsWith("#") ? trimmed : "#" + trimmed;
    }

    private String toDisplayName(String tag) {
        String core = tag.startsWith("#") ? tag.substring(1) : tag;
        core = core.replace('-', ' ');
        if (core.isBlank()) {
            return "Habit Rule";
        }
        return Character.toUpperCase(core.charAt(0)) + core.substring(1);
    }

    private ExpenseCategory inferCategory(String tag) {
        return switch (tag) {
            case "#snacks", "#coffee", "#food-delivery" -> ExpenseCategory.FOOD;
            case "#clothes" -> ExpenseCategory.CLOTHES;
            case "#gaming" -> ExpenseCategory.ENTERTAINMENT;
            case "#fuel" -> ExpenseCategory.CAR;
            case "#subscriptions" -> ExpenseCategory.SUBSCRIPTIONS;
            default -> ExpenseCategory.OTHER;
        };
    }

    public static class OnboardingData {
        private String firstName = "";
        private String lastName = "";
        private String email = "";
        private String ageText = "";
        private String currencyCode = "EUR";
        private UserProfileType profileType = UserProfileType.PERSONAL_USE;
        private final Set<String> selectedHabitTags = new LinkedHashSet<>();
        private final List<IncomeInput> incomeInputs = new ArrayList<>();
        private PlanInput planInput = new PlanInput();

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName == null ? "" : firstName.trim();
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName == null ? "" : lastName.trim();
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email == null ? "" : email.trim();
        }

        public String getAgeText() {
            return ageText;
        }

        public void setAgeText(String ageText) {
            this.ageText = ageText == null ? "" : ageText.trim();
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode == null ? "EUR" : currencyCode.trim();
        }

        public UserProfileType getProfileType() {
            return profileType;
        }

        public void setProfileType(UserProfileType profileType) {
            this.profileType = profileType;
        }

        public Set<String> getSelectedHabitTags() {
            return selectedHabitTags;
        }

        public List<IncomeInput> getIncomeInputs() {
            return incomeInputs;
        }

        public PlanInput getPlanInput() {
            return planInput;
        }

        public void setPlanInput(PlanInput planInput) {
            this.planInput = planInput;
        }
    }

    public static class IncomeInput {
        private String sourceName;
        private IncomeType incomeType;
        private BigDecimal amount;
        private boolean recurring;
        private boolean received;

        public IncomeInput(String sourceName, IncomeType incomeType, BigDecimal amount, boolean recurring, boolean received) {
            this.sourceName = sourceName;
            this.incomeType = incomeType;
            this.amount = amount;
            this.recurring = recurring;
            this.received = received;
        }

        public String getSourceName() {
            return sourceName;
        }

        public IncomeType getIncomeType() {
            return incomeType;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public boolean isRecurring() {
            return recurring;
        }

        public boolean isReceived() {
            return received;
        }
    }

    public static class PlanInput {
        private BigDecimal fixedCostsBudget = BigDecimal.ZERO;
        private BigDecimal foodBudget = BigDecimal.ZERO;
        private BigDecimal transportBudget = BigDecimal.ZERO;
        private BigDecimal familyBudget = BigDecimal.ZERO;
        private BigDecimal discretionaryBudget = BigDecimal.ZERO;
        private BigDecimal savingsPercent = BigDecimal.ZERO;
        private BigDecimal goalsPercent = BigDecimal.ZERO;
        private BigDecimal safetyBufferAmount = BigDecimal.ZERO;
        private String notes = "";

        public BigDecimal getFixedCostsBudget() {
            return fixedCostsBudget;
        }

        public void setFixedCostsBudget(BigDecimal fixedCostsBudget) {
            this.fixedCostsBudget = ValidationUtils.requireNonNegative(fixedCostsBudget, "fixedCostsBudget");
        }

        public BigDecimal getFoodBudget() {
            return foodBudget;
        }

        public void setFoodBudget(BigDecimal foodBudget) {
            this.foodBudget = ValidationUtils.requireNonNegative(foodBudget, "foodBudget");
        }

        public BigDecimal getTransportBudget() {
            return transportBudget;
        }

        public void setTransportBudget(BigDecimal transportBudget) {
            this.transportBudget = ValidationUtils.requireNonNegative(transportBudget, "transportBudget");
        }

        public BigDecimal getFamilyBudget() {
            return familyBudget;
        }

        public void setFamilyBudget(BigDecimal familyBudget) {
            this.familyBudget = ValidationUtils.requireNonNegative(familyBudget, "familyBudget");
        }

        public BigDecimal getDiscretionaryBudget() {
            return discretionaryBudget;
        }

        public void setDiscretionaryBudget(BigDecimal discretionaryBudget) {
            this.discretionaryBudget = ValidationUtils.requireNonNegative(discretionaryBudget, "discretionaryBudget");
        }

        public BigDecimal getSavingsPercent() {
            return savingsPercent;
        }

        public void setSavingsPercent(BigDecimal savingsPercent) {
            this.savingsPercent = ValidationUtils.requirePercent(savingsPercent, "savingsPercent");
        }

        public BigDecimal getGoalsPercent() {
            return goalsPercent;
        }

        public void setGoalsPercent(BigDecimal goalsPercent) {
            this.goalsPercent = ValidationUtils.requirePercent(goalsPercent, "goalsPercent");
        }

        public BigDecimal getSafetyBufferAmount() {
            return safetyBufferAmount;
        }

        public void setSafetyBufferAmount(BigDecimal safetyBufferAmount) {
            this.safetyBufferAmount = ValidationUtils.requireNonNegative(safetyBufferAmount, "safetyBufferAmount");
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes == null ? "" : notes.trim();
        }
    }
}
