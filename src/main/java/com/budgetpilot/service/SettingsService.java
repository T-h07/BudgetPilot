package com.budgetpilot.service;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.DemoDataSeeder;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;

import java.time.YearMonth;

public class SettingsService {
    private final AppContext appContext;

    public SettingsService(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
    }

    public void updateProfile(ProfileSettingsData data) {
        ValidationUtils.requireNonNull(data, "data");

        BudgetStore store = requiredStore();
        UserProfile profile = store.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
        }

        profile.setFirstName(ValidationUtils.requireNonBlank(data.getFirstName(), "firstName"));
        profile.setLastName(ValidationUtils.requireNonBlank(data.getLastName(), "lastName"));
        profile.setEmail(ValidationUtils.requireValidEmail(data.getEmail(), "email"));
        profile.setAge(ValidationUtils.parseOptionalNonNegativeInteger(data.getAgeText(), "age"));
        profile.setCurrencyCode(ValidationUtils.requireNonBlank(data.getCurrencyCode(), "currencyCode"));
        profile.setProfileType(ValidationUtils.requireNonNull(data.getProfileType(), "profileType"));

        store.saveUserProfile(profile);
        appContext.reloadCurrentUserFromStore();
    }

    public void updateModuleFlags(boolean familyEnabled, boolean investmentsEnabled, boolean achievementsEnabled) {
        BudgetStore store = requiredStore();
        UserProfile profile = ValidationUtils.requireNonNull(store.getUserProfile(), "userProfile");

        profile.setFamilyModuleEnabled(familyEnabled);
        profile.setInvestmentsModuleEnabled(investmentsEnabled);
        profile.setAchievementsModuleEnabled(achievementsEnabled);

        store.saveUserProfile(profile);
        appContext.reloadCurrentUserFromStore();
    }

    public void shiftSelectedMonth(int delta) {
        appContext.setSelectedMonth(MonthUtils.shift(appContext.getSelectedMonth(), delta));
    }

    public void jumpToCurrentMonth() {
        appContext.setSelectedMonth(MonthUtils.currentMonth());
    }

    public void startNewMonth() {
        YearMonth nextMonth = MonthUtils.nextMonth(appContext.getSelectedMonth());
        appContext.setSelectedMonth(nextMonth);
        BudgetStore store = requiredStore();
        if (store.getMonthlyPlan(nextMonth) == null) {
            store.saveMonthlyPlan(new MonthlyPlan(nextMonth));
        }
        appContext.notifyContextChanged();
    }

    public void clearAllData() {
        BudgetStore store = requiredStore();
        store.clearAll();
        appContext.setCurrentUser(null);
        appContext.notifyContextChanged();
    }

    public void seedDemoDataForSelectedMonth() {
        BudgetStore store = requiredStore();
        DemoDataSeeder.seed(store, appContext.getSelectedMonth());
        appContext.reloadCurrentUserFromStore();
    }

    public void resetToFreshOnboarding() {
        clearAllData();
        appContext.setSelectedMonth(MonthUtils.currentMonth());
    }

    public static class ProfileSettingsData {
        private String firstName;
        private String lastName;
        private String email;
        private String ageText;
        private String currencyCode;
        private UserProfileType profileType;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAgeText() {
            return ageText;
        }

        public void setAgeText(String ageText) {
            this.ageText = ageText;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public UserProfileType getProfileType() {
            return profileType;
        }

        public void setProfileType(UserProfileType profileType) {
            this.profileType = profileType;
        }
    }

    private BudgetStore requiredStore() {
        return ValidationUtils.requireNonNull(appContext.getStore(), "store");
    }
}
