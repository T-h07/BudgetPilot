package com.budgetpilot.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.YearMonth;
import java.util.Objects;

public class AppContext {
    private final StringProperty currentUserName = new SimpleStringProperty(this, "currentUserName", "User");
    private final ObjectProperty<YearMonth> currentMonth =
            new SimpleObjectProperty<>(this, "currentMonth", YearMonth.now());

    // Service placeholders for future phases.
    private Object budgetService;
    private Object expenseService;
    private Object analyticsService;

    public AppContext() {
        this("User", YearMonth.now());
    }

    public AppContext(String currentUserName, YearMonth currentMonth) {
        setCurrentUserName(currentUserName);
        setCurrentMonth(currentMonth);
    }

    public String getCurrentUserName() {
        return currentUserName.get();
    }

    public void setCurrentUserName(String currentUserName) {
        String resolvedName = currentUserName == null || currentUserName.isBlank()
                ? "User"
                : currentUserName.trim();
        this.currentUserName.set(resolvedName);
    }

    public StringProperty currentUserNameProperty() {
        return currentUserName;
    }

    public YearMonth getCurrentMonth() {
        return currentMonth.get();
    }

    public void setCurrentMonth(YearMonth currentMonth) {
        this.currentMonth.set(Objects.requireNonNullElse(currentMonth, YearMonth.now()));
    }

    public ObjectProperty<YearMonth> currentMonthProperty() {
        return currentMonth;
    }

    public Object getBudgetService() {
        return budgetService;
    }

    public void setBudgetService(Object budgetService) {
        this.budgetService = budgetService;
    }

    public Object getExpenseService() {
        return expenseService;
    }

    public void setExpenseService(Object expenseService) {
        this.expenseService = expenseService;
    }

    public Object getAnalyticsService() {
        return analyticsService;
    }

    public void setAnalyticsService(Object analyticsService) {
        this.analyticsService = analyticsService;
    }
}
