package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleDocumentEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.db.entity.VehiclePhotoEntity;
import com.turbo.mobile.data.db.entity.ZoneEntity;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.PhotoItem;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.SearchFilter;
import com.turbo.mobile.data.model.SearchService;
import com.turbo.mobile.data.model.VehicleBadge;
import com.turbo.mobile.data.model.VehicleDetail;
import com.turbo.mobile.data.model.VehicleEditData;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.data.model.VehicleForm;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.data.model.Zone;

import java.util.ArrayList;
import java.util.List;

public class RoomVehicleRepository {

    private final AppDatabase db;

    public RoomVehicleRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public List<Zone> getZones() {
        List<Zone> zones = new ArrayList<>();
        for (ZoneEntity entity : db.zoneDao().getAll()) {
            Zone zone = new Zone();
            zone.setId(entity.id);
            zone.setName(entity.name);
            zones.add(zone);
        }
        return zones;
    }

    public List<VehicleSummary> searchPublished(SearchFilter filter) {
        Long zoneId = null;
        String category = null;
        boolean taxiOnly = false;
        Integer maxRateCents = null;
        if (filter != null) {
            zoneId = filter.getZoneId();
            if (filter.getCategory() != null) {
                category = filter.getCategory().name();
            }
            taxiOnly = filter.getService() == SearchService.TAXI;
            maxRateCents = filter.getMaxRateCents();
        }
        List<VehicleSummary> vehicles = new ArrayList<>();
        for (VehicleEntity vehicle : db.vehicleDao().search(zoneId, category, taxiOnly, maxRateCents)) {
            if (isPublished(vehicle)) {
                vehicles.add(toSummary(vehicle));
            }
        }
        return vehicles;
    }

    public Result<VehicleDetail> getVehicleDetail(long vehicleId) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null || vehicle.deletedAt != null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            RatingSummary rating = db.reviewDao().getRatingSummary(vehicle.ownerId);
            UserEntity owner = db.userDao().findById(vehicle.ownerId);
            List<String> photoPaths = new ArrayList<>();
            for (VehiclePhotoEntity photo : db.vehiclePhotoDao().getAll(vehicleId)) {
                photoPaths.add(photo.filePath);
            }
            VehicleDetail detail = new VehicleDetail();
            detail.setId(vehicle.id);
            detail.setMake(vehicle.make);
            detail.setModel(vehicle.model);
            detail.setYear(vehicle.year);
            detail.setCategory(vehicle.category);
            detail.setServiceType(vehicle.serviceType);
            detail.setHourlyRateCents(vehicle.hourlyRateCents);
            detail.setZoneName(zoneName(vehicle));
            detail.setThumbnailPath(thumbnail(vehicle));
            detail.setOwnerRatingAvg(rating.getAverage());
            detail.setOwnerRatingCount(rating.getCount());
            detail.setFuelType(vehicle.fuelType);
            detail.setDescription(vehicle.description);
            detail.setAddress(vehicle.address);
            detail.setLatitude(vehicle.latitude);
            detail.setLongitude(vehicle.longitude);
            detail.setPhotoPaths(photoPaths);
            detail.setOwnerId(vehicle.ownerId);
            detail.setOwnerName(owner.firstName + " " + owner.lastName.charAt(0) + ".");
            detail.setPublished(isPublished(vehicle));
            detail.setAvailableUntil(vehicle.availableUntil);
            return Result.success(detail);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    public List<OwnerVehicleItem> getOwnerVehicles(long ownerId) {
        List<OwnerVehicleItem> items = new ArrayList<>();
        for (VehicleEntity vehicle : db.vehicleDao().getByOwner(ownerId)) {
            OwnerVehicleItem item = new OwnerVehicleItem();
            item.setId(vehicle.id);
            item.setTitle(vehicle.make + " " + vehicle.model + " " + vehicle.year);
            item.setPlate(vehicle.licensePlate);
            item.setZoneName(zoneName(vehicle));
            item.setThumbnailPath(thumbnail(vehicle));
            item.setApproved(isApproved(vehicle));
            item.setPublished(isPublished(vehicle));
            item.setAvailableUntil(vehicle.availableUntil);
            for (VehicleDocumentEntity document : db.vehicleDocumentDao().getAll(vehicle.id)) {
                if (document.status == DocumentStatus.PENDING) {
                    item.setPendingDocuments(item.getPendingDocuments() + 1);
                } else if (document.status == DocumentStatus.REJECTED) {
                    item.setRejectedDocument(document.documentType);
                }
            }
            if (item.getRejectedDocument() != null) {
                item.setBadge(VehicleBadge.ACTION_NEEDED);
            } else if (item.getPendingDocuments() > 0) {
                item.setBadge(VehicleBadge.PENDING_REVIEW);
            } else if (item.isPublished()) {
                item.setBadge(VehicleBadge.PUBLISHED);
            } else {
                item.setBadge(VehicleBadge.NOT_PUBLISHED);
            }
            items.add(item);
        }
        return items;
    }

    public Result<VehicleEditData> getVehicleForEdit(long ownerId, long vehicleId) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null || vehicle.deletedAt != null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can edit this vehicle");
            }
            VehicleForm form = new VehicleForm();
            form.setMake(vehicle.make);
            form.setModel(vehicle.model);
            form.setVin(vehicle.vin);
            form.setPlate(vehicle.licensePlate);
            form.setDescription(vehicle.description);
            form.setAddress(vehicle.address);
            form.setYear(vehicle.year);
            form.setCategory(vehicle.category);
            form.setFuelType(vehicle.fuelType);
            form.setServiceType(vehicle.serviceType);
            form.setHourlyRateCents(vehicle.hourlyRateCents);
            form.setZoneId(vehicle.zoneId);
            form.setLatitude(vehicle.latitude);
            form.setLongitude(vehicle.longitude);
            List<PhotoItem> photos = new ArrayList<>();
            for (VehiclePhotoEntity entity : db.vehiclePhotoDao().getAll(vehicleId)) {
                PhotoItem photo = new PhotoItem();
                photo.setId(entity.id);
                photo.setFilePath(entity.filePath);
                photos.add(photo);
            }
            VehicleEditData data = new VehicleEditData();
            data.setVehicleId(vehicleId);
            data.setForm(form);
            data.setPhotos(photos);
            return Result.success(data);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    private VehicleSummary toSummary(VehicleEntity vehicle) {
        RatingSummary rating = db.reviewDao().getRatingSummary(vehicle.ownerId);
        VehicleSummary summary = new VehicleSummary();
        summary.setId(vehicle.id);
        summary.setMake(vehicle.make);
        summary.setModel(vehicle.model);
        summary.setYear(vehicle.year);
        summary.setCategory(vehicle.category);
        summary.setServiceType(vehicle.serviceType);
        summary.setHourlyRateCents(vehicle.hourlyRateCents);
        summary.setZoneName(zoneName(vehicle));
        summary.setThumbnailPath(thumbnail(vehicle));
        summary.setOwnerRatingAvg(rating.getAverage());
        summary.setOwnerRatingCount(rating.getCount());
        summary.setAvailableUntil(vehicle.availableUntil);
        return summary;
    }

    private boolean isApproved(VehicleEntity vehicle) {
        int approved = 0;
        for (VehicleDocumentEntity document : db.vehicleDocumentDao().getAll(vehicle.id)) {
            if (document.status == DocumentStatus.APPROVED) {
                approved++;
            }
        }
        return approved == 2;
    }

    private boolean isPublished(VehicleEntity vehicle) {
        UserEntity owner = db.userDao().findById(vehicle.ownerId);
        return isApproved(vehicle)
                && vehicle.availableUntil != null
                && vehicle.availableUntil > System.currentTimeMillis()
                && vehicle.deletedAt == null
                && owner.isActive;
    }

    private String zoneName(VehicleEntity vehicle) {
        return db.zoneDao().findById(vehicle.zoneId).name;
    }

    private String thumbnail(VehicleEntity vehicle) {
        List<VehiclePhotoEntity> photos = db.vehiclePhotoDao().getAll(vehicle.id);
        if (photos.isEmpty()) {
            return null;
        }
        return photos.get(0).filePath;
    }
}
