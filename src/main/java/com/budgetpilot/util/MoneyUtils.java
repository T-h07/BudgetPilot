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
}
