package com.turbo.mobile.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DisplayFormat {

    public static String money(int cents) {
        return String.format(Locale.US, "CAD %,.2f", cents / 100.0);
    }

    public static String moneyWithoutCents(int cents) {
        return String.format(Locale.US, "CAD %,d", cents / 100);
    }

    public static String date(long millis) {
        return formatDate("MMM d", millis);
    }

    public static String dayAndDate(long millis) {
        return formatDate("EEE, MMM d", millis);
    }

    public static String fullDate(long millis) {
        return formatDate("MMM d, yyyy", millis);
    }

    public static String time(long millis) {
        return formatDate("HH:mm", millis);
    }

    public static String timeRange(long start, long end) {
        return time(start) + "–" + time(end);
    }

    public static String[] halfHours() {
        String[] times = new String[48];
        for (int i = 0; i < times.length; i++) {
            times[i] = String.format(Locale.US, "%02d:%02d", i / 2, i % 2 * 30);
        }
        return times;
    }

    private static String formatDate(String pattern, long millis) {
        return new SimpleDateFormat(pattern, Locale.US).format(new Date(millis));
    }
}
