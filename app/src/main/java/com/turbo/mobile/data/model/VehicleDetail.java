package com.turbo.mobile.data.model;

import java.util.List;

public class VehicleDetail {

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
    private FuelType fuelType;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private List<String> photoPaths;
    private long ownerId;
    private String ownerName;
    private boolean published;
    private Long availableUntil;

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

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public List<String> getPhotoPaths() {
        return photoPaths;
    }

    public void setPhotoPaths(List<String> photoPaths) {
        this.photoPaths = photoPaths;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Long getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(Long availableUntil) {
        this.availableUntil = availableUntil;
    }
}
