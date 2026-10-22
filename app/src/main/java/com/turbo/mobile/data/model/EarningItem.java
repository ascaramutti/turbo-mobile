package com.turbo.mobile.data.model;

public class EarningItem {

    private long bookingId;
    private String vehicleTitle;
    private String driverName;
    private long paidAt;
    private int hours;
    private int payoutCents;

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public String getVehicleTitle() {
        return vehicleTitle;
    }

    public void setVehicleTitle(String vehicleTitle) {
        this.vehicleTitle = vehicleTitle;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getPayoutCents() {
        return payoutCents;
    }

    public void setPayoutCents(int payoutCents) {
        this.payoutCents = payoutCents;
    }
}
