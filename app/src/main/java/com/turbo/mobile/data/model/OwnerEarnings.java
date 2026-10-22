package com.turbo.mobile.data.model;

import java.util.List;

public class OwnerEarnings {

    private int totalCents;
    private int monthCents;
    private int completedRentals;
    private List<EarningItem> history;

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

    public int getCompletedRentals() {
        return completedRentals;
    }

    public void setCompletedRentals(int completedRentals) {
        this.completedRentals = completedRentals;
    }

    public List<EarningItem> getHistory() {
        return history;
    }

    public void setHistory(List<EarningItem> history) {
        this.history = history;
    }
}
