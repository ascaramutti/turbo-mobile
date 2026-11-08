package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleDocumentEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.db.entity.VehiclePhotoEntity;
import com.turbo.mobile.data.db.entity.ZoneEntity;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.PhotoItem;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.SearchFilter;
import com.turbo.mobile.data.model.SearchService;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.data.model.VehicleBadge;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleDetail;
import com.turbo.mobile.data.model.VehicleEditData;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.data.model.VehicleForm;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.data.model.Zone;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.util.ImageStorage;
import com.turbo.mobile.util.Validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RoomVehicleRepository implements VehicleRepository {

    private final AppDatabase db;
    private final ImageStorage imageStorage;

    public RoomVehicleRepository(Context context) {
        db = AppDatabase.getInstance(context);
        imageStorage = new ImageStorage(context);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Result<Long> registerVehicle(long ownerId, VehicleForm form, List<Uri> photos, Uri insurance, Uri registration) {
        try {
            if (!isActiveOwner(ownerId)) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only an active owner can register a vehicle");
            }
            VehicleError formError = validateForm(form);
            if (formError != null) {
                return Result.error(formError, "Invalid vehicle data");
            }
            String vin = normalize(form.getVin());
            if (!vin.matches("[A-HJ-NPR-Z0-9]{17}")) {
                return Result.error(VehicleError.INVALID_VIN, "Invalid VIN");
            }
            if (photos == null || photos.isEmpty()) {
                return Result.error(VehicleError.PHOTO_REQUIRED, "At least one photo is required");
            }
            if (insurance == null || registration == null) {
                return Result.error(VehicleError.DOCUMENTS_REQUIRED, "Both documents are required");
            }
            VehicleEntity sameVin = db.vehicleDao().findByVin(vin);
            if (sameVin != null) {
                if (sameVin.deletedAt != null && sameVin.ownerId == ownerId) {
                    return Result.error(VehicleError.VIN_RESTORABLE, "Vehicle " + sameVin.id + " can be restored");
                }
                return Result.error(VehicleError.VIN_TAKEN, "VIN " + vin + " is already registered");
            }
            String plate = normalize(form.getPlate());
            if (db.vehicleDao().findByPlate(plate) != null) {
                return Result.error(VehicleError.PLATE_TAKEN, "Plate " + plate + " is already registered");
            }
            List<String> savedPaths = new ArrayList<>();
            String insurancePath;
            String registrationPath;
            try {
                for (Uri photo : photos) {
                    savedPaths.add(imageStorage.save(photo, ImageStorage.Folder.VEHICLES));
                }
                insurancePath = imageStorage.save(insurance, ImageStorage.Folder.DOCUMENTS);
                savedPaths.add(insurancePath);
                registrationPath = imageStorage.save(registration, ImageStorage.Folder.DOCUMENTS);
                savedPaths.add(registrationPath);
            } catch (IOException e) {
                Log.e("RoomVehicleRepository", "Cannot save the vehicle images", e);
                for (String path : savedPaths) {
                    imageStorage.delete(path);
                }
                return Result.error(VehicleError.FILE_ERROR, "Cannot save the images");
            }
            long now = System.currentTimeMillis();
            VehicleEntity vehicle = new VehicleEntity();
            vehicle.ownerId = ownerId;
            vehicle.vin = vin;
            vehicle.createdAt = now;
            copyForm(form, vehicle);
            long vehicleId;
            db.beginTransaction();
            try {
                vehicleId = db.vehicleDao().insert(vehicle);
                for (int i = 0; i < photos.size(); i++) {
                    VehiclePhotoEntity photo = new VehiclePhotoEntity();
                    photo.vehicleId = vehicleId;
                    photo.filePath = savedPaths.get(i);
                    photo.uploadedAt = now;
                    db.vehiclePhotoDao().insert(photo);
                }
                insertDocument(vehicleId, DocumentType.INSURANCE, insurancePath, now);
                insertDocument(vehicleId, DocumentType.REGISTRATION, registrationPath, now);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return Result.success(vehicleId);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Long> restoreVehicle(long ownerId, String vin) {
        try {
            if (!isActiveOwner(ownerId)) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only an active owner can restore a vehicle");
            }
            VehicleEntity vehicle = db.vehicleDao().findByVin(normalize(vin));
            if (vehicle == null || vehicle.deletedAt == null || vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_FOUND, "No deleted vehicle with VIN " + vin);
            }
            vehicle.deletedAt = null;
            vehicle.availableUntil = null;
            db.beginTransaction();
            try {
                db.vehicleDao().update(vehicle);
                for (VehicleDocumentEntity document : db.vehicleDocumentDao().getAll(vehicle.id)) {
                    document.status = DocumentStatus.PENDING;
                    document.rejectionReason = null;
                    document.reviewedBy = null;
                    document.reviewedAt = null;
                    db.vehicleDocumentDao().update(document);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return Result.success(vehicle.id);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> updateVehicle(long ownerId, long vehicleId, VehicleForm form) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can edit this vehicle");
            }
            if (vehicle.deletedAt != null) {
                return Result.error(VehicleError.VEHICLE_DELETED, "Vehicle " + vehicleId + " was deleted");
            }
            VehicleError formError = validateForm(form);
            if (formError != null) {
                return Result.error(formError, "Invalid vehicle data");
            }
            String plate = normalize(form.getPlate());
            VehicleEntity samePlate = db.vehicleDao().findByPlate(plate);
            if (samePlate != null && samePlate.id != vehicleId) {
                return Result.error(VehicleError.PLATE_TAKEN, "Plate " + plate + " is already registered");
            }
            copyForm(form, vehicle);
            db.vehicleDao().update(vehicle);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Long> addVehiclePhoto(long ownerId, long vehicleId, Uri photo) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can add photos");
            }
            if (vehicle.deletedAt != null) {
                return Result.error(VehicleError.VEHICLE_DELETED, "Vehicle " + vehicleId + " was deleted");
            }
            VehiclePhotoEntity entity = new VehiclePhotoEntity();
            try {
                entity.filePath = imageStorage.save(photo, ImageStorage.Folder.VEHICLES);
            } catch (IOException e) {
                Log.e("RoomVehicleRepository", "Cannot save the vehicle photo", e);
                return Result.error(VehicleError.FILE_ERROR, "Cannot save the photo");
            }
            entity.vehicleId = vehicleId;
            entity.uploadedAt = System.currentTimeMillis();
            long photoId = db.vehiclePhotoDao().insert(entity);
            return Result.success(photoId);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteVehiclePhoto(long ownerId, long photoId) {
        try {
            VehiclePhotoEntity photo = db.vehiclePhotoDao().findById(photoId);
            VehicleEntity vehicle = null;
            if (photo != null) {
                vehicle = db.vehicleDao().findById(photo.vehicleId);
            }
            if (photo == null || vehicle.deletedAt != null) {
                return Result.error(VehicleError.NOT_FOUND, "Photo " + photoId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can delete photos");
            }
            if (db.vehiclePhotoDao().getAll(vehicle.id).size() == 1) {
                return Result.error(VehicleError.LAST_PHOTO, "A vehicle needs at least one photo");
            }
            db.vehiclePhotoDao().delete(photoId);
            imageStorage.delete(photo.filePath);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> publish(long ownerId, long vehicleId, long availableUntil) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can publish this vehicle");
            }
            if (vehicle.deletedAt != null) {
                return Result.error(VehicleError.VEHICLE_DELETED, "Vehicle " + vehicleId + " was deleted");
            }
            if (!isApproved(vehicle)) {
                return Result.error(VehicleError.NOT_APPROVED, "Both documents must be approved");
            }
            if (availableUntil <= System.currentTimeMillis()) {
                return Result.error(VehicleError.DATE_NOT_FUTURE, "The date must be in the future");
            }
            vehicle.availableUntil = availableUntil;
            db.vehicleDao().update(vehicle);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> unpublish(long ownerId, long vehicleId) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null || vehicle.deletedAt != null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can unpublish this vehicle");
            }
            vehicle.availableUntil = null;
            db.vehicleDao().update(vehicle);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Integer> deleteVehicle(long ownerId, long vehicleId) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null || vehicle.deletedAt != null) {
                return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(VehicleError.NOT_ALLOWED, "Only the owner can delete this vehicle");
            }
            List<BookingEntity> pending = new ArrayList<>();
            for (BookingEntity booking : db.bookingDao().getOpenByVehicle(vehicleId)) {
                if (booking.status != BookingStatus.PENDING) {
                    return Result.error(VehicleError.HAS_ACTIVE_BOOKINGS, "Vehicle " + vehicleId + " has active bookings");
                }
                pending.add(booking);
            }
            vehicle.deletedAt = System.currentTimeMillis();
            vehicle.availableUntil = null;
            db.beginTransaction();
            try {
                db.vehicleDao().update(vehicle);
                for (BookingEntity booking : pending) {
                    booking.status = BookingStatus.REJECTED;
                    booking.statusReason = StatusReasons.VEHICLE_UNAVAILABLE;
                    db.bookingDao().update(booking);
                    notify(booking.driverId, "Booking request rejected", "Your request for the "
                            + vehicle.make + " " + vehicle.model + " " + vehicle.year
                            + " was rejected: " + StatusReasons.VEHICLE_UNAVAILABLE + ".");
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return Result.success(pending.size());
        } catch (RuntimeException e) {
            Log.e("RoomVehicleRepository", "Unexpected database error", e);
            return Result.error(VehicleError.UNEXPECTED, e.getMessage());
        }
    }

    private void notify(long userId, String title, String message) {
        NotificationEntity notification = new NotificationEntity();
        notification.userId = userId;
        notification.title = title;
        notification.message = message;
        notification.isRead = false;
        notification.createdAt = System.currentTimeMillis();
        db.notificationDao().insert(notification);
    }

    private VehicleError validateForm(VehicleForm form) {
        if (Validators.isBlank(form.getMake()) || Validators.isBlank(form.getModel())
                || Validators.isBlank(form.getPlate()) || Validators.isBlank(form.getAddress())
                || form.getCategory() == null || form.getFuelType() == null || form.getServiceType() == null
                || form.getHourlyRateCents() <= 0 || db.zoneDao().findById(form.getZoneId()) == null) {
            return VehicleError.INVALID_INPUT;
        }
        if (Calendar.getInstance().get(Calendar.YEAR) - form.getYear() >= 9) {
            return VehicleError.TOO_OLD;
        }
        if (form.getCategory() == VehicleCategory.COMPACT && form.getServiceType() == VehicleServiceType.TAXI_AND_DELIVERY) {
            return VehicleError.COMPACT_DELIVERY_ONLY;
        }
        return null;
    }

    private void copyForm(VehicleForm form, VehicleEntity vehicle) {
        vehicle.make = form.getMake().trim();
        vehicle.model = form.getModel().trim();
        vehicle.licensePlate = normalize(form.getPlate());
        vehicle.description = form.getDescription();
        vehicle.address = form.getAddress().trim();
        vehicle.year = form.getYear();
        vehicle.category = form.getCategory();
        vehicle.fuelType = form.getFuelType();
        vehicle.serviceType = form.getServiceType();
        vehicle.hourlyRateCents = form.getHourlyRateCents();
        vehicle.zoneId = form.getZoneId();
        vehicle.latitude = form.getLatitude();
        vehicle.longitude = form.getLongitude();
    }

    private void insertDocument(long vehicleId, DocumentType type, String filePath, long now) {
        VehicleDocumentEntity document = new VehicleDocumentEntity();
        document.vehicleId = vehicleId;
        document.documentType = type;
        document.filePath = filePath;
        document.status = DocumentStatus.PENDING;
        document.uploadedAt = now;
        db.vehicleDocumentDao().insert(document);
    }

    private boolean isActiveOwner(long ownerId) {
        UserEntity owner = db.userDao().findById(ownerId);
        return owner != null && owner.role == Role.CAR_OWNER && owner.isActive;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toUpperCase();
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
