package com.budgetpilot.service.auth;

import com.budgetpilot.model.UserProfile;
import com.budgetpilot.security.PasswordHasher;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.util.ValidationUtils;

import java.util.Locale;

public class AuthService {
    private final BudgetStore budgetStore;
    private final PasswordHasher passwordHasher;

    public AuthService(BudgetStore budgetStore) {
        this.budgetStore = ValidationUtils.requireNonNull(budgetStore, "budgetStore");
        this.passwordHasher = new PasswordHasher();
    }

    public UserProfile authenticate(String email, char[] password) {
        String normalizedEmail = ValidationUtils.requireValidEmail(email, "email").toLowerCase(Locale.ROOT);
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password is required.");
        }

        UserProfile profile = budgetStore.getUserProfile();
        if (profile == null) {
            throw invalidCredentials();
        }
        if (!profile.isActive()) {
            throw new IllegalArgumentException("Account is inactive.");
        }
        if (!normalizedEmail.equals(profile.getEmail().toLowerCase(Locale.ROOT))) {
            throw invalidCredentials();
        }
        if (!passwordHasher.verify(password, profile.getPasswordHash())) {
            throw invalidCredentials();
        }
        return profile;
    }

    public String hashPassword(char[] password) {
        return passwordHasher.hash(password);
    }

    private IllegalArgumentException invalidCredentials() {
        return new IllegalArgumentException("Invalid email or password");
    }
}
