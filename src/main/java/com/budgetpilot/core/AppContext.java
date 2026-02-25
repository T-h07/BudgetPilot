package com.budgetpilot.core;

import com.budgetpilot.model.UserProfile;
import com.budgetpilot.service.PersistenceStatus;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AppContext {
    private final ObjectProperty<YearMonth> selectedMonth =
            new SimpleObjectProperty<>(this, "selectedMonth", MonthUtils.currentMonth());
    private final ObjectProperty<UserProfile> currentUser =
            new SimpleObjectProperty<>(this, "currentUser");
    private final ObjectProperty<Boolean> authenticated =
            new SimpleObjectProperty<>(this, "authenticated", false);
    private final ObjectProperty<String> authenticatedUserId =
            new SimpleObjectProperty<>(this, "authenticatedUserId", null);
    private final ObjectProperty<PersistenceStatus> persistenceStatus =
            new SimpleObjectProperty<>(this, "persistenceStatus", new PersistenceStatus(false, "Persistence unavailable", null, null));

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private BudgetStore store;
    private Consumer<PageId> navigator;

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
        YearMonth validatedMonth = ValidationUtils.requireNonNull(selectedMonth, "selectedMonth");
        if (validatedMonth.equals(this.selectedMonth.get())) {
            return;
        }
        this.selectedMonth.set(validatedMonth);
        notifyContextChanged();
    }

    public ObjectProperty<YearMonth> selectedMonthProperty() {
        return selectedMonth;
    }

    public BudgetStore getStore() {
        return store;
    }

    public void setStore(BudgetStore store) {
        this.store = store;
        reloadCurrentUserFromStore();
    }

    public UserProfile getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(UserProfile currentUser) {
        this.currentUser.set(currentUser);
        if (currentUser == null) {
            authenticated.set(false);
            authenticatedUserId.set(null);
        } else if (Boolean.TRUE.equals(authenticated.get())
                && authenticatedUserId.get() != null
                && !authenticatedUserId.get().equals(currentUser.getId())) {
            authenticated.set(false);
            authenticatedUserId.set(null);
        }
        notifyContextChanged();
    }

    public ObjectProperty<UserProfile> currentUserProperty() {
        return currentUser;
    }

    public boolean onboardingCompleted() {
        UserProfile profile = getCurrentUser();
        return profile != null && profile.getPasswordHash() != null && !profile.getPasswordHash().isBlank();
    }

    public void reloadCurrentUserFromStore() {
        UserProfile loaded;
        if (store == null) {
            loaded = null;
        } else {
            loaded = store.getUserProfile();
        }
        currentUser.set(loaded);
        if (loaded == null) {
            authenticated.set(false);
            authenticatedUserId.set(null);
        } else if (Boolean.TRUE.equals(authenticated.get())) {
            String sessionUser = authenticatedUserId.get();
            if (sessionUser == null || !sessionUser.equals(loaded.getId())) {
                authenticated.set(false);
                authenticatedUserId.set(null);
            }
        }
        notifyContextChanged();
    }

    public boolean isAuthenticated() {
        return Boolean.TRUE.equals(authenticated.get());
    }

    public ObjectProperty<Boolean> authenticatedProperty() {
        return authenticated;
    }

    public String getAuthenticatedUserId() {
        return authenticatedUserId.get();
    }

    public ObjectProperty<String> authenticatedUserIdProperty() {
        return authenticatedUserId;
    }

    public void signIn(String userId) {
        String normalizedUserId = ValidationUtils.requireNonBlank(userId, "userId");
        authenticated.set(true);
        authenticatedUserId.set(normalizedUserId);
        notifyContextChanged();
    }

    public void signOut() {
        authenticated.set(false);
        authenticatedUserId.set(null);
        notifyContextChanged();
    }

    public void setNavigator(Consumer<PageId> navigator) {
        this.navigator = navigator;
    }

    public void navigate(PageId pageId) {
        if (navigator != null && pageId != null) {
            navigator.accept(pageId);
        }
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void notifyContextChanged() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException ex) {
                System.err.println("AppContext listener failed: " + ex.getMessage());
            }
        }
    }

    public PersistenceStatus getPersistenceStatus() {
        return persistenceStatus.get();
    }

    public void setPersistenceStatus(PersistenceStatus persistenceStatus) {
        this.persistenceStatus.set(persistenceStatus);
        notifyContextChanged();
    }

    public ObjectProperty<PersistenceStatus> persistenceStatusProperty() {
        return persistenceStatus;
    }

    public boolean isPersistenceAvailable() {
        PersistenceStatus status = getPersistenceStatus();
        return status != null && status.isPersistent();
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

    public String getCurrentUserInitials() {
        String name = getCurrentUserDisplayName();
        if (name.isBlank()) {
            return "US";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    // Compatibility helpers for BP-PT1/BP-PT2 pages.
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
