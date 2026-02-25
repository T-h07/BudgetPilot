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
}
