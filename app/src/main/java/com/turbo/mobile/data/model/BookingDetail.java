package com.turbo.mobile.data.model;

import java.util.List;

public class BookingDetail extends BookingListItem {

    private String pickupAddress;
    private Double latitude;
    private Double longitude;
    private long driverId;
    private long ownerId;
    private Long startedAt;
    private Long completedAt;
    private long createdAt;
    private long payDeadline;
    private long startWindowCloses;
    private Long paidAt;
    private String cardLast4;
    private Integer feeCents;
    private Integer ownerPayoutCents;
    private int pickupPhotoCount;
    private int returnPhotoCount;
    private boolean reviewedByDriver;
    private boolean reviewedByOwner;
    private List<Long> overlappingPendingIds;

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public long getDriverId() {
        return driverId;
    }

    public void setDriverId(long driverId) {
        this.driverId = driverId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getPayDeadline() {
        return payDeadline;
    }

    public void setPayDeadline(long payDeadline) {
        this.payDeadline = payDeadline;
    }

    public long getStartWindowCloses() {
        return startWindowCloses;
    }

    public void setStartWindowCloses(long startWindowCloses) {
        this.startWindowCloses = startWindowCloses;
    }

    public Long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Long paidAt) {
        this.paidAt = paidAt;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public Integer getFeeCents() {
        return feeCents;
    }

    public void setFeeCents(Integer feeCents) {
        this.feeCents = feeCents;
    }

    public Integer getOwnerPayoutCents() {
        return ownerPayoutCents;
    }

    public void setOwnerPayoutCents(Integer ownerPayoutCents) {
        this.ownerPayoutCents = ownerPayoutCents;
    }

    public int getPickupPhotoCount() {
        return pickupPhotoCount;
    }

    public void setPickupPhotoCount(int pickupPhotoCount) {
        this.pickupPhotoCount = pickupPhotoCount;
    }

    public int getReturnPhotoCount() {
        return returnPhotoCount;
    }

    public void setReturnPhotoCount(int returnPhotoCount) {
        this.returnPhotoCount = returnPhotoCount;
    }

    public boolean isReviewedByDriver() {
        return reviewedByDriver;
    }

    public void setReviewedByDriver(boolean reviewedByDriver) {
        this.reviewedByDriver = reviewedByDriver;
    }

    public boolean isReviewedByOwner() {
        return reviewedByOwner;
    }

    public void setReviewedByOwner(boolean reviewedByOwner) {
        this.reviewedByOwner = reviewedByOwner;
    }

    public List<Long> getOverlappingPendingIds() {
        return overlappingPendingIds;
    }

    public void setOverlappingPendingIds(List<Long> overlappingPendingIds) {
        this.overlappingPendingIds = overlappingPendingIds;
    }
}
