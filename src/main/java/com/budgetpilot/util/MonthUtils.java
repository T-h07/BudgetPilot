package com.budgetpilot.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MonthUtils {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.getDefault());

    private MonthUtils() {
    }

    public static YearMonth currentMonth() {
        return YearMonth.now();
    }

    public static String format(YearMonth month) {
        if (month == null) {
            return "";
        }
        return month.format(FORMATTER);
    }
}
