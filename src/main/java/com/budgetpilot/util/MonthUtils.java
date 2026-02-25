package com.budgetpilot.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
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

    public static YearMonth previousMonth(YearMonth month) {
        return shift(month, -1);
    }

    public static YearMonth nextMonth(YearMonth month) {
        return shift(month, 1);
    }

    public static YearMonth shift(YearMonth month, int deltaMonths) {
        YearMonth source = month == null ? currentMonth() : month;
        return source.plusMonths(deltaMonths);
    }

    public static int daysInMonth(YearMonth month) {
        YearMonth targetMonth = month == null ? currentMonth() : month;
        return targetMonth.lengthOfMonth();
    }

    public static boolean isCurrentMonth(YearMonth month) {
        YearMonth targetMonth = month == null ? currentMonth() : month;
        return targetMonth.equals(currentMonth());
    }

    public static int elapsedDaysForForecast(YearMonth month, LocalDate referenceDate) {
        YearMonth targetMonth = month == null ? currentMonth() : month;
        LocalDate date = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth current = YearMonth.from(date);
        if (targetMonth.isBefore(current)) {
            return targetMonth.lengthOfMonth();
        }
        if (targetMonth.isAfter(current)) {
            return 0;
        }
        return Math.min(date.getDayOfMonth(), targetMonth.lengthOfMonth());
    }

    public static int denominatorDaysForDailyAverage(YearMonth month) {
        YearMonth targetMonth = month == null ? currentMonth() : month;
        YearMonth current = currentMonth();
        if (targetMonth.isBefore(current)) {
            return targetMonth.lengthOfMonth();
        }
        if (targetMonth.isAfter(current)) {
            return targetMonth.lengthOfMonth();
        }
        return Math.max(LocalDate.now().getDayOfMonth(), 1);
    }

    public static List<WeekRange> calendarWeekRanges(YearMonth month) {
        YearMonth targetMonth = month == null ? currentMonth() : month;
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        DayOfWeek firstDayOfWeek = weekFields.getFirstDayOfWeek();
        DayOfWeek lastDayOfWeek = firstDayOfWeek.minus(1);

        LocalDate firstCalendarWeekStart = monthStart.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
        LocalDate lastCalendarWeekEnd = monthEnd.with(TemporalAdjusters.nextOrSame(lastDayOfWeek));

        List<WeekRange> ranges = new ArrayList<>();
        int weekNumber = 1;
        for (LocalDate weekStart = firstCalendarWeekStart;
             !weekStart.isAfter(lastCalendarWeekEnd);
             weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate visibleStart = weekStart.isBefore(monthStart) ? monthStart : weekStart;
            LocalDate visibleEnd = weekEnd.isAfter(monthEnd) ? monthEnd : weekEnd;
            ranges.add(new WeekRange("Week " + weekNumber++, visibleStart, visibleEnd));
        }
        return List.copyOf(ranges);
    }

    public static String format(YearMonth month) {
        if (month == null) {
            return "";
        }
        return month.format(FORMATTER);
    }

    public static final class WeekRange {
        private final String weekLabel;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public WeekRange(String weekLabel, LocalDate startDate, LocalDate endDate) {
            this.weekLabel = weekLabel;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getWeekLabel() {
            return weekLabel;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }
    }
}
