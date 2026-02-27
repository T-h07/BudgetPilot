package com.budgetpilot.persistence;

import com.budgetpilot.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

public final class DbUtils {
    private DbUtils() {
    }

    public static BigDecimal parseBigDecimal(String raw, String fieldName, BigDecimal fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback == null ? BigDecimal.ZERO.setScale(2) : MoneyUtils.normalize(fallback);
        }
        try {
            return MoneyUtils.normalize(new BigDecimal(raw.trim()));
        } catch (NumberFormatException ex) {
            System.err.println("Invalid decimal for " + fieldName + ": " + raw + ", using fallback.");
            return fallback == null ? BigDecimal.ZERO.setScale(2) : MoneyUtils.normalize(fallback);
        }
    }

    public static BigDecimal parseNullableBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MoneyUtils.normalize(new BigDecimal(raw.trim()));
        } catch (NumberFormatException ex) {
            System.err.println("Invalid nullable decimal value: " + raw + ", returning null.");
            return null;
        }
    }

    public static YearMonth parseYearMonth(String raw, YearMonth fallback, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return YearMonth.parse(raw.trim());
        } catch (Exception ex) {
            System.err.println("Invalid YearMonth for " + fieldName + ": " + raw + ", using fallback.");
            return fallback;
        }
    }

    public static LocalDate parseLocalDate(String raw, LocalDate fallback, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            System.err.println("Invalid LocalDate for " + fieldName + ": " + raw + ", using fallback.");
            return fallback;
        }
    }

    public static LocalDateTime parseLocalDateTime(String raw, LocalDateTime fallback, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (Exception ex) {
            System.err.println("Invalid LocalDateTime for " + fieldName + ": " + raw + ", using fallback.");
            return fallback;
        }
    }

    public static <E extends Enum<E>> E parseEnum(String raw, Class<E> enumClass, E fallback, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumClass, raw.trim());
        } catch (Exception ex) {
            System.err.println("Invalid enum for " + fieldName + ": " + raw + ", using fallback.");
            return fallback;
        }
    }

    public static int toIntBoolean(boolean value) {
        return value ? 1 : 0;
    }

    public static boolean fromIntBoolean(int value) {
        return value != 0;
    }

    public static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value);
    }

    public static void setNullableBigDecimal(PreparedStatement statement, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, MoneyUtils.normalize(value).toPlainString());
    }

    public static void setNullableLocalDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value.toString());
    }

    public static void setNullableLocalDateTime(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value.toString());
    }
}
