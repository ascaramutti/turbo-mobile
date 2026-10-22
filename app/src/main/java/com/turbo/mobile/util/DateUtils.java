package com.turbo.mobile.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public final class DateUtils {

    private DateUtils() {
    }

    public static Range weekRange(long now) {
        return weekRange(now, ZoneId.systemDefault());
    }

    public static Range weekRange(long now, ZoneId zone) {
        LocalDate monday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Range(startOfDay(monday, zone), startOfDay(monday.plusWeeks(1), zone));
    }

    public static Range monthRange(long now) {
        return monthRange(yearMonth(now));
    }

    public static Range monthRange(YearMonth month) {
        return monthRange(month, ZoneId.systemDefault());
    }

    public static Range monthRange(YearMonth month, ZoneId zone) {
        return new Range(startOfDay(month.atDay(1), zone), startOfDay(month.plusMonths(1).atDay(1), zone));
    }

    public static YearMonth yearMonth(long millis) {
        return yearMonth(millis, ZoneId.systemDefault());
    }

    public static YearMonth yearMonth(long millis, ZoneId zone) {
        return YearMonth.from(Instant.ofEpochMilli(millis).atZone(zone));
    }

    private static long startOfDay(LocalDate day, ZoneId zone) {
        ZonedDateTime start = day.atStartOfDay(zone);
        return start.toInstant().toEpochMilli();
    }

    public static final class Range {

        private final long start;
        private final long end;

        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public boolean contains(long millis) {
            return millis >= start && millis < end;
        }
    }
}
