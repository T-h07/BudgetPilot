package com.budgetpilot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public final class MoneyUtils {
    public static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private MoneyUtils() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? ZERO : normalize(amount);
    }

    public static String format(BigDecimal amount, String currencyCode) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        String resolvedCurrency = (currencyCode == null || currencyCode.isBlank())
                ? "EUR"
                : currencyCode.trim().toUpperCase(Locale.ROOT);
        try {
            formatter.setCurrency(Currency.getInstance(resolvedCurrency));
        } catch (IllegalArgumentException ignored) {
            formatter.setCurrency(Currency.getInstance("EUR"));
        }
        return formatter.format(zeroIfNull(amount));
    }

    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        return normalize(zeroIfNull(a).subtract(zeroIfNull(b)));
    }

    public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return normalize(zeroIfNull(a).add(zeroIfNull(b)));
    }

    public static BigDecimal percentOf(BigDecimal baseAmount, BigDecimal percent) {
        BigDecimal base = zeroIfNull(baseAmount);
        BigDecimal pct = zeroIfNull(percent);
        return normalize(base.multiply(pct).divide(HUNDRED, 2, RoundingMode.HALF_UP));
    }

    public static BigDecimal parse(String rawValue, String fieldName, boolean allowZero) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalizedInput = rawValue.trim().replace(',', '.');
        try {
            BigDecimal parsed = normalize(new BigDecimal(normalizedInput));
            int comparison = parsed.compareTo(BigDecimal.ZERO);
            if (comparison < 0 || (!allowZero && comparison == 0)) {
                throw new IllegalArgumentException(
                        allowZero
                                ? fieldName + " must be non-negative"
                                : fieldName + " must be greater than 0"
                );
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid amount");
        }
    }

    public static BigDecimal parseNonNegativeOrZero(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return ZERO;
        }
        return parse(rawValue, fieldName, true);
    }
}
