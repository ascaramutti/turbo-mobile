package com.turbo.mobile.data.model;

public class SearchFilter {

    private Long zoneId;
    private VehicleCategory category;
    private SearchService service;
    private Integer maxRateCents;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    public SearchService getService() {
        return service;
    }

    public void setService(SearchService service) {
        this.service = service;
    }

    public Integer getMaxRateCents() {
        return maxRateCents;
    }

    public void setMaxRateCents(Integer maxRateCents) {
        this.maxRateCents = maxRateCents;
    }
}
