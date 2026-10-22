package com.turbo.mobile.data.db;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeedTime {

    private static final Pattern TIME = Pattern.compile(
            "^(now|floor30\\((now(?:[+-]\\d+[mhd])*)\\)|d([+-]\\d+) (\\d\\d):(\\d\\d))((?:[+-]\\d+[mhd])*)$");
    private static final Pattern DATE = Pattern.compile("^(?:d([+-]\\d+)|(\\d{4}-\\d\\d-\\d\\d))$");
    private static final Pattern OFFSET = Pattern.compile("([+-])(\\d+)([mhd])");

    private final ZonedDateTime now;

    public SeedTime(long nowMillis, ZoneId zone) {
        this.now = Instant.ofEpochMilli(nowMillis).atZone(zone);
    }

    public long time(String expr) {
        Matcher m = TIME.matcher(expr);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad time expression: " + expr);
        }
        long base;
        if (m.group(1).equals("now")) {
            base = now.toInstant().toEpochMilli();
        } else if (m.group(2) != null) {
            ZonedDateTime t = Instant.ofEpochMilli(now.toInstant().toEpochMilli()
                    + offsets(m.group(2).substring(3))).atZone(now.getZone());
            base = t.withMinute(t.getMinute() / 30 * 30).withSecond(0).withNano(0)
                    .toInstant().toEpochMilli();
        } else {
            LocalDate day = now.toLocalDate().plusDays(Integer.parseInt(m.group(3)));
            LocalTime clock = LocalTime.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));
            base = ZonedDateTime.of(day, clock, now.getZone()).toInstant().toEpochMilli();
        }
        return base + offsets(m.group(6));
    }

    public long date(String expr) {
        Matcher m = DATE.matcher(expr);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad date expression: " + expr);
        }
        LocalDate day = m.group(2) != null ? LocalDate.parse(m.group(2))
                : now.toLocalDate().plusDays(Integer.parseInt(m.group(1)));
        return day.atStartOfDay(now.getZone()).toInstant().toEpochMilli();
    }

    private static long offsets(String s) {
        long total = 0;
        Matcher m = OFFSET.matcher(s == null ? "" : s);
        while (m.find()) {
            long n = Long.parseLong(m.group(2));
            String u = m.group(3);
            long unit = u.equals("m") ? 60_000L : u.equals("h") ? 3_600_000L : 86_400_000L;
            total += (m.group(1).equals("-") ? -1 : 1) * n * unit;
        }
        return total;
    }
}
