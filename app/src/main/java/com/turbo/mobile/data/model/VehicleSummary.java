package com.turbo.mobile.data.model;

public class VehicleSummary {

    private long id;
    private String make;
    private String model;
    private int year;
    private VehicleCategory category;
    private VehicleServiceType serviceType;
    private int hourlyRateCents;
    private String zoneName;
    private String thumbnailPath;
    private Double ownerRatingAvg;
    private int ownerRatingCount;
    private long availableUntil;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    public VehicleServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(VehicleServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public int getHourlyRateCents() {
        return hourlyRateCents;
    }

    public void setHourlyRateCents(int hourlyRateCents) {
        this.hourlyRateCents = hourlyRateCents;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public Double getOwnerRatingAvg() {
        return ownerRatingAvg;
    }

    public void setOwnerRatingAvg(Double ownerRatingAvg) {
        this.ownerRatingAvg = ownerRatingAvg;
    }

    public int getOwnerRatingCount() {
        return ownerRatingCount;
    }

    public void setOwnerRatingCount(int ownerRatingCount) {
        this.ownerRatingCount = ownerRatingCount;
    }

    public long getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(long availableUntil) {
        this.availableUntil = availableUntil;
    }
}
