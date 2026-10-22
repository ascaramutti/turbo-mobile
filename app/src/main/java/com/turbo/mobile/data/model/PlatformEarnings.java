package com.turbo.mobile.data.model;

import java.time.YearMonth;

public class PlatformEarnings {

    private YearMonth month;
    private int feesCents;
    private int paymentsCents;
    private int completedCount;

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    public int getFeesCents() {
        return feesCents;
    }

    public void setFeesCents(int feesCents) {
        this.feesCents = feesCents;
    }

    public int getPaymentsCents() {
        return paymentsCents;
    }

    public void setPaymentsCents(int paymentsCents) {
        this.paymentsCents = paymentsCents;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }
}
