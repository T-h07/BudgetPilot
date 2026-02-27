package com.budgetpilot.core;

import com.budgetpilot.model.UserProfile;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.YearMonth;

public class AppContext {
    private final ObjectProperty<YearMonth> selectedMonth =
            new SimpleObjectProperty<>(this, "selectedMonth", MonthUtils.currentMonth());
    private final ObjectProperty<UserProfile> currentUser =
            new SimpleObjectProperty<>(this, "currentUser");

    private BudgetStore store;

    public AppContext() {
        this(null, MonthUtils.currentMonth());
    }

    public AppContext(BudgetStore store, YearMonth selectedMonth) {
        setSelectedMonth(selectedMonth == null ? MonthUtils.currentMonth() : selectedMonth);
        setStore(store);
    }

    public YearMonth getSelectedMonth() {
        return selectedMonth.get();
    }

    public void setSelectedMonth(YearMonth selectedMonth) {
        this.selectedMonth.set(ValidationUtils.requireNonNull(selectedMonth, "selectedMonth"));
    }

    public ObjectProperty<YearMonth> selectedMonthProperty() {
        return selectedMonth;
    }

    public BudgetStore getStore() {
        return store;
    }

    public void setStore(BudgetStore store) {
        this.store = store;
        if (store != null) {
            this.currentUser.set(store.getUserProfile());
        }
    }

    public UserProfile getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(UserProfile currentUser) {
        this.currentUser.set(currentUser);
    }

    public ObjectProperty<UserProfile> currentUserProperty() {
        return currentUser;
    }

    public String getCurrentMonthDisplayText() {
        return MonthUtils.format(getSelectedMonth());
    }

    public String getCurrentUserDisplayName() {
        UserProfile profile = getCurrentUser();
        if (profile != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        if (store != null) {
            UserProfile storeProfile = store.getUserProfile();
            if (storeProfile != null && !storeProfile.getDisplayName().isBlank()) {
                return storeProfile.getDisplayName();
            }
        }
        return "User";
    }

    // Compatibility helpers for BP-PT1 pages.
    public YearMonth getCurrentMonth() {
        return getSelectedMonth();
    }

    public void setCurrentMonth(YearMonth month) {
        setSelectedMonth(month);
    }

    public ObjectProperty<YearMonth> currentMonthProperty() {
        return selectedMonthProperty();
    }

    public String getCurrentUserName() {
        return getCurrentUserDisplayName();
    }
}
