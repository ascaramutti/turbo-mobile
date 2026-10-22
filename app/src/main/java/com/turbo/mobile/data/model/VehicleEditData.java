package com.turbo.mobile.data.model;

import java.util.List;

public class VehicleEditData {

    private long vehicleId;
    private VehicleForm form;
    private List<PhotoItem> photos;

    public long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleForm getForm() {
        return form;
    }

    public void setForm(VehicleForm form) {
        this.form = form;
    }

    public List<PhotoItem> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoItem> photos) {
        this.photos = photos;
    }
}
