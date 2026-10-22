package com.turbo.mobile.data.model;

public class DriverSpending {

    private int totalCents;
    private int monthCents;
    private int completedCount;

    public int getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(int totalCents) {
        this.totalCents = totalCents;
    }

    public int getMonthCents() {
        return monthCents;
    }

    public void setMonthCents(int monthCents) {
        this.monthCents = monthCents;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }
}
