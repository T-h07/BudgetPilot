package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.FamilyExpenseType;
import com.budgetpilot.model.enums.GoalType;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.RelationshipType;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.service.FamilyService;
import com.budgetpilot.service.GoalService;
import com.budgetpilot.service.SavingsService;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public final class DemoDataSeeder {
    private DemoDataSeeder() {
    }

    public static void seed(BudgetStore store, YearMonth month) {
        ValidationUtils.requireNonNull(store, "store");
        YearMonth seedMonth = month == null ? MonthUtils.currentMonth() : month;

        store.clearAll();

        UserProfile profile = new UserProfile("Taulant", "Hasani", "taulant@budgetpilot.app", UserProfileType.MAIN_FAMILY_SUPPORTER);
        profile.setCurrencyCode("EUR");
        profile.setAge(29);
        profile.setFamilyModuleEnabled(true);
        profile.setInvestmentsModuleEnabled(true);
        profile.setAchievementsModuleEnabled(true);
        store.saveUserProfile(profile);

        SavingsService savingsService = new SavingsService(store);
        GoalService goalService = new GoalService(store);
        FamilyService familyService = new FamilyService(store);

        MonthlyPlan plan = new MonthlyPlan(seedMonth);
        plan.setFixedCostsBudget(new BigDecimal("1450"));
        plan.setFoodBudget(new BigDecimal("420"));
        plan.setTransportBudget(new BigDecimal("210"));
        plan.setFamilyBudget(new BigDecimal("300"));
        plan.setDiscretionaryBudget(new BigDecimal("380"));
        plan.setSavingsPercent(new BigDecimal("18.50"));
        plan.setGoalsPercent(new BigDecimal("11.50"));
        plan.setSafetyBufferAmount(new BigDecimal("240"));
        plan.setNotes("Demo monthly plan seeded for BP-PT2.");
        store.saveMonthlyPlan(plan);

        IncomeEntry salary = new IncomeEntry(seedMonth, seedMonth.atDay(1), "Primary Salary", IncomeType.SALARY, new BigDecimal("3250"));
        salary.setRecurring(true);
        salary.setReceived(true);
        salary.setNotes("Main salary deposit");
        store.saveIncomeEntry(salary);

        IncomeEntry sideHustle = new IncomeEntry(seedMonth, seedMonth.atDay(12), "Design Side Gig", IncomeType.SIDE_HUSTLE, new BigDecimal("420"));
        sideHustle.setRecurring(false);
        sideHustle.setReceived(true);
        sideHustle.setNotes("Client payment");
        store.saveIncomeEntry(sideHustle);

        IncomeEntry freelance = new IncomeEntry(seedMonth, seedMonth.atDay(20), "Freelance Dev", IncomeType.FREELANCE, new BigDecimal("610"));
        freelance.setRecurring(false);
        freelance.setReceived(false);
        freelance.setNotes("Pending invoice");
        store.saveIncomeEntry(freelance);

        store.saveExpenseEntry(expense(seedMonth, 2, "78.50", ExpenseCategory.FOOD, "Groceries", "#groceries", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 3, "42.00", ExpenseCategory.CAR, "Fuel", "#car", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 4, "95.00", ExpenseCategory.BILLS, "Electricity", "#utilities", PaymentMethod.BANK_TRANSFER));
        store.saveExpenseEntry(expense(seedMonth, 6, "18.20", ExpenseCategory.FOOD, "Coffee & snacks", "#snacks", PaymentMethod.CASH));
        store.saveExpenseEntry(expense(seedMonth, 9, "31.99", ExpenseCategory.SUBSCRIPTIONS, "Streaming", "#subscriptions", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 11, "64.00", ExpenseCategory.HEALTH, "Pharmacy", "#health", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 14, "26.40", ExpenseCategory.FOOD, "Coffee runs", "#coffee", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 15, "120.00", ExpenseCategory.FAMILY, "Parent support", "#family", PaymentMethod.BANK_TRANSFER));
        store.saveExpenseEntry(expense(seedMonth, 18, "88.00", ExpenseCategory.CLOTHES, "Jacket", "#clothes", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 22, "46.50", ExpenseCategory.ENTERTAINMENT, "Cinema + dinner", "#weekend", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 24, "59.90", ExpenseCategory.CLOTHES, "Shoes", "#clothes", PaymentMethod.CARD));

        SavingsBucket emergency = new SavingsBucket("Emergency Fund", BigDecimal.ZERO, new BigDecimal("5000"));
        emergency.setNotes("Priority reserve");
        store.saveSavingsBucket(emergency);

        SavingsBucket tuition = new SavingsBucket("Tuition Reserve", BigDecimal.ZERO, new BigDecimal("3500"));
        tuition.setNotes("Education savings");
        store.saveSavingsBucket(tuition);

        savingsService.addContribution(emergency.getId(), new BigDecimal("1200"), dateAt(seedMonth, 4), "Initial emergency allocation");
        savingsService.addContribution(emergency.getId(), new BigDecimal("750"), dateAt(seedMonth, 13), "Salary cycle contribution");
        savingsService.withdraw(emergency.getId(), new BigDecimal("110"), dateAt(seedMonth, 21), "Unexpected car maintenance");

        savingsService.addContribution(tuition.getId(), new BigDecimal("600"), dateAt(seedMonth, 6), "Tuition reserve contribution");
        savingsService.addContribution(tuition.getId(), new BigDecimal("420"), dateAt(seedMonth, 18), "Side income allocation");
        savingsService.withdraw(tuition.getId(), new BigDecimal("95"), dateAt(seedMonth, 24), "Books and materials");

        Goal carGoal = new Goal("Car Down Payment", GoalType.CAR, new BigDecimal("6000"), BigDecimal.ZERO, 2);
        carGoal.setNotes("Target by summer");
        store.saveGoal(carGoal);

        Goal laptopGoal = new Goal("New Laptop", GoalType.LAPTOP, new BigDecimal("1800"), BigDecimal.ZERO, 3);
        laptopGoal.setNotes("For work and studies");
        store.saveGoal(laptopGoal);

        goalService.contribute(carGoal.getId(), new BigDecimal("950"), dateAt(seedMonth, 5), "Main goal contribution");
        goalService.contribute(carGoal.getId(), new BigDecimal("700"), dateAt(seedMonth, 16), "Mid-month transfer");
        goalService.withdraw(carGoal.getId(), new BigDecimal("120"), dateAt(seedMonth, 23), "Temporary reallocation");

        goalService.contribute(laptopGoal.getId(), new BigDecimal("420"), dateAt(seedMonth, 8), "Laptop progress contribution");
        goalService.contribute(laptopGoal.getId(), new BigDecimal("360"), dateAt(seedMonth, 19), "Freelance contribution");
        goalService.adjust(laptopGoal.getId(), new BigDecimal("-40"), dateAt(seedMonth, 27), "Price revision adjustment");

        FamilyMember mother = new FamilyMember("Mother", RelationshipType.PARENT);
        mother.setMonthlySupportBudget(new BigDecimal("220"));
        mother.setMonthlyMedicalBudget(new BigDecimal("90"));
        mother.setWeeklyAllowance(new BigDecimal("20"));
        store.saveFamilyMember(mother);

        FamilyMember brother = new FamilyMember("Brother", RelationshipType.SIBLING);
        brother.setMonthlySupportBudget(new BigDecimal("110"));
        brother.setWeeklyAllowance(new BigDecimal("15"));
        brother.setMonthlyMedicalBudget(new BigDecimal("15"));
        store.saveFamilyMember(brother);

        familyService.saveFamilyExpense(familyExpense(mother.getId(), seedMonth, 7, "40.00", FamilyExpenseType.ALLOWANCE, "Weekly allowance transfer"));
        familyService.saveFamilyExpense(familyExpense(mother.getId(), seedMonth, 12, "68.00", FamilyExpenseType.MEDICAL, "Medical checkup"));
        familyService.saveFamilyExpense(familyExpense(mother.getId(), seedMonth, 17, "120.00", FamilyExpenseType.SUPPORT, "Monthly support contribution"));
        familyService.saveFamilyExpense(familyExpense(brother.getId(), seedMonth, 10, "22.00", FamilyExpenseType.ALLOWANCE, "School pocket money"));
        familyService.saveFamilyExpense(familyExpense(brother.getId(), seedMonth, 19, "35.00", FamilyExpenseType.SCHOOL, "Course materials"));
        familyService.saveFamilyExpense(familyExpense(brother.getId(), seedMonth, 26, "54.00", FamilyExpenseType.EXTRA, "Emergency school trip"));

        HabitRule snacksRule = new HabitRule("#snacks", "Snack Control", new BigDecimal("90"), new BigDecimal("70"));
        snacksRule.setLinkedCategory(ExpenseCategory.FOOD);
        snacksRule.setNotes("Warn when snack spending gets too high.");
        store.saveHabitRule(snacksRule);

        HabitRule clothesRule = new HabitRule("#clothes", "Clothes Control", new BigDecimal("140"), new BigDecimal("75"));
        clothesRule.setLinkedCategory(ExpenseCategory.CLOTHES);
        clothesRule.setNotes("Keep non-essential wardrobe spend contained.");
        store.saveHabitRule(clothesRule);

        HabitRule coffeeRule = new HabitRule("#coffee", "Coffee Discipline", new BigDecimal("45"), new BigDecimal("70"));
        coffeeRule.setLinkedCategory(ExpenseCategory.FOOD);
        coffeeRule.setNotes("Cap impulse coffee spending and shift to planned purchases.");
        store.saveHabitRule(coffeeRule);
    }

    private static ExpenseEntry expense(
            YearMonth month,
            int day,
            String amount,
            ExpenseCategory category,
            String note,
            String tag,
            PaymentMethod paymentMethod
    ) {
        ExpenseEntry entry = new ExpenseEntry(month, month.atDay(day), new BigDecimal(amount), category, paymentMethod);
        entry.setNote(note);
        entry.setTag(tag);
        entry.setSubcategory(category.getLabel());
        return entry;
    }

    private static LocalDate dateAt(YearMonth month, int day) {
        int safeDay = Math.max(1, Math.min(day, month.lengthOfMonth()));
        return month.atDay(safeDay);
    }

    private static FamilyExpenseEntry familyExpense(
            String memberId,
            YearMonth month,
            int day,
            String amount,
            FamilyExpenseType type,
            String note
    ) {
        FamilyExpenseEntry entry = new FamilyExpenseEntry();
        entry.setFamilyMemberId(memberId);
        entry.setMonth(month);
        entry.setExpenseDate(dateAt(month, day));
        entry.setAmount(new BigDecimal(amount));
        entry.setExpenseType(type);
        entry.setNote(note);
        return entry;
    }
}
