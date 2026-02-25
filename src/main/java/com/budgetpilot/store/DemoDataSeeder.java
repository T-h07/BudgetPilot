package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.GoalType;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.RelationshipType;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
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
        store.saveExpenseEntry(expense(seedMonth, 15, "120.00", ExpenseCategory.FAMILY, "Parent support", "#family", PaymentMethod.BANK_TRANSFER));
        store.saveExpenseEntry(expense(seedMonth, 18, "88.00", ExpenseCategory.CLOTHES, "Jacket", "#clothes", PaymentMethod.CARD));
        store.saveExpenseEntry(expense(seedMonth, 22, "46.50", ExpenseCategory.ENTERTAINMENT, "Cinema + dinner", "#weekend", PaymentMethod.CARD));

        SavingsBucket emergency = new SavingsBucket("Emergency Fund", new BigDecimal("1840"), new BigDecimal("5000"));
        emergency.setNotes("Priority reserve");
        store.saveSavingsBucket(emergency);

        SavingsBucket tuition = new SavingsBucket("Tuition Reserve", new BigDecimal("920"), new BigDecimal("3500"));
        tuition.setNotes("Education savings");
        store.saveSavingsBucket(tuition);

        Goal carGoal = new Goal("Car Down Payment", GoalType.CAR, new BigDecimal("6000"), new BigDecimal("2250"), 2);
        carGoal.setNotes("Target by summer");
        store.saveGoal(carGoal);

        Goal laptopGoal = new Goal("New Laptop", GoalType.LAPTOP, new BigDecimal("1800"), new BigDecimal("760"), 3);
        laptopGoal.setNotes("For work and studies");
        store.saveGoal(laptopGoal);

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

        HabitRule snacksRule = new HabitRule("#snacks", "Snack Control", new BigDecimal("90"), new BigDecimal("70"));
        snacksRule.setLinkedCategory(ExpenseCategory.FOOD);
        snacksRule.setNotes("Warn when snack spending gets too high.");
        store.saveHabitRule(snacksRule);

        HabitRule clothesRule = new HabitRule("#clothes", "Clothes Control", new BigDecimal("140"), new BigDecimal("75"));
        clothesRule.setLinkedCategory(ExpenseCategory.CLOTHES);
        clothesRule.setNotes("Keep non-essential wardrobe spend contained.");
        store.saveHabitRule(clothesRule);
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
}
