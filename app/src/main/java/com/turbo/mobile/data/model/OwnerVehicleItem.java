package com.turbo.mobile.data.model;

public class OwnerVehicleItem {

    private long id;
    private String title;
    private String plate;
    private String zoneName;
    private String thumbnailPath;
    private VehicleBadge badge;
    private boolean approved;
    private boolean published;
    private Long availableUntil;
    private int pendingDocuments;
    private DocumentType rejectedDocument;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
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

    public VehicleBadge getBadge() {
        return badge;
    }

    public void setBadge(VehicleBadge badge) {
        this.badge = badge;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
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

    public int getPendingDocuments() {
        return pendingDocuments;
    }

    public void setPendingDocuments(int pendingDocuments) {
        this.pendingDocuments = pendingDocuments;
    }

    public DocumentType getRejectedDocument() {
        return rejectedDocument;
    }

    public void setRejectedDocument(DocumentType rejectedDocument) {
        this.rejectedDocument = rejectedDocument;
    }
}
