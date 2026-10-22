package com.turbo.mobile.data.model;

public class WeeklyHours {

    private long weekStart;
    private long weekEnd;
    private double bookedHours;
    private int limitHours;

    public long getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(long weekStart) {
        this.weekStart = weekStart;
    }

    public long getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(long weekEnd) {
        this.weekEnd = weekEnd;
    }

    public double getBookedHours() {
        return bookedHours;
    }

    public void setBookedHours(double bookedHours) {
        this.bookedHours = bookedHours;
    }

    public int getLimitHours() {
        return limitHours;
    }

    public void setLimitHours(int limitHours) {
        this.limitHours = limitHours;
    }
}
