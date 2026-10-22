package com.turbo.mobile.data.model;

import java.util.List;

public class InspectionPhotos {

    private long bookingId;
    private List<String> pickupPaths;
    private List<String> returnPaths;

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public List<String> getPickupPaths() {
        return pickupPaths;
    }

    public void setPickupPaths(List<String> pickupPaths) {
        this.pickupPaths = pickupPaths;
    }

    public List<String> getReturnPaths() {
        return returnPaths;
    }

    public void setReturnPaths(List<String> returnPaths) {
        this.returnPaths = returnPaths;
    }
}
