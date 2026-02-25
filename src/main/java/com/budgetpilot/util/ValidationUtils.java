package com.budgetpilot.util;

import java.math.BigDecimal;
import java.util.Objects;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

    public static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalized = MoneyUtils.normalize(requireNonNull(value, fieldName));
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return normalized;
    }

    public static BigDecimal requirePercent(BigDecimal value, String fieldName) {
        BigDecimal normalized = requireNonNegative(value, fieldName);
        if (normalized.compareTo(MoneyUtils.HUNDRED) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
        }
        return normalized;
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        int dotIndex = normalized.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < normalized.length() - 1;
    }

    public static String requireValidEmail(String email, String fieldName) {
        String normalized = requireNonBlank(email, fieldName);
        if (!isValidEmail(normalized)) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }

    public static Integer parseOptionalNonNegativeInteger(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw new IllegalArgumentException(fieldName + " must be non-negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer", ex);
        }
    }
}
