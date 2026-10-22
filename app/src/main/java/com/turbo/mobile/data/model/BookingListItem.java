package com.turbo.mobile.data.model;

import java.util.List;

public class BookingListItem {

    private long id;
    private long vehicleId;
    private String vehicleTitle;
    private String thumbnailPath;
    private String driverName;
    private String ownerName;
    private Double counterpartRatingAvg;
    private long start;
    private long end;
    private int hours;
    private ServiceType serviceType;
    private int totalPriceCents;
    private BookingStatus status;
    private String statusReason;
    private boolean paid;
    private boolean overdue;
    private List<DriverAction> driverActions;
    private List<OwnerAction> ownerActions;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleTitle() {
        return vehicleTitle;
    }

    public void setVehicleTitle(String vehicleTitle) {
        this.vehicleTitle = vehicleTitle;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Double getCounterpartRatingAvg() {
        return counterpartRatingAvg;
    }

    public void setCounterpartRatingAvg(Double counterpartRatingAvg) {
        this.counterpartRatingAvg = counterpartRatingAvg;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public int getTotalPriceCents() {
        return totalPriceCents;
    }

    public void setTotalPriceCents(int totalPriceCents) {
        this.totalPriceCents = totalPriceCents;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public List<DriverAction> getDriverActions() {
        return driverActions;
    }

    public void setDriverActions(List<DriverAction> driverActions) {
        this.driverActions = driverActions;
    }

    public List<OwnerAction> getOwnerActions() {
        return ownerActions;
    }

    public void setOwnerActions(List<OwnerAction> ownerActions) {
        this.ownerActions = ownerActions;
    }
}
