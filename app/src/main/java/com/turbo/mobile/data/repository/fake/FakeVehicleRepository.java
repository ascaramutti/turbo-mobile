package com.turbo.mobile.data.repository.fake;

import android.net.Uri;

import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DocumentAction;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.FuelType;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.PhotoItem;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.BOOKINGS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.USERS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLE_DOCUMENTS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLE_PHOTOS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.ZONES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getBoolean;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getDoubleOrNull;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getInt;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLongOrNull;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakeVehicleRepository implements VehicleRepository {

    private final FakeDataStore store;

    public FakeVehicleRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public List<Zone> getZones() {
        List<Zone> result = new ArrayList<>();
        for (JSONObject row : store.rows(ZONES)) {
            Zone zone = new Zone();
            zone.setId(getLong(row, "id"));
            zone.setName(getString(row, "name"));
            result.add(zone);
        }
        return result;
    }

    @Override
    public List<VehicleSummary> searchPublished(SearchFilter filter) {
        List<VehicleSummary> result = new ArrayList<>();
        for (JSONObject vehicle : store.rows(VEHICLES)) {
            if (getBoolean(vehicle, "_published") && matches(vehicle, filter)) {
                result.add(toSummary(vehicle));
            }
        }
        result.sort((a, b) -> a.getHourlyRateCents() != b.getHourlyRateCents()
                ? Integer.compare(a.getHourlyRateCents(), b.getHourlyRateCents())
                : Long.compare(a.getId(), b.getId()));
        return result;
    }

    @Override
    public Result<VehicleDetail> getVehicleDetail(long vehicleId) {
        RepoError forced = forced(VehicleError.NOT_FOUND, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (vehicle == null || !vehicle.isNull("deleted_at")) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        long ownerId = getLong(vehicle, "owner_id");
        RatingSummary rating = FakeRatings.of(store, ownerId);
        VehicleDetail detail = new VehicleDetail();
        detail.setId(vehicleId);
        detail.setMake(getString(vehicle, "make"));
        detail.setModel(getString(vehicle, "model"));
        detail.setYear(getInt(vehicle, "year"));
        detail.setCategory(VehicleCategory.valueOf(getString(vehicle, "category")));
        detail.setServiceType(VehicleServiceType.valueOf(getString(vehicle, "service_type")));
        detail.setHourlyRateCents(getInt(vehicle, "hourly_rate_cents"));
        detail.setZoneName(FakeVehicles.zoneName(store, vehicle));
        detail.setThumbnailPath(FakeVehicles.thumbnail(store, vehicleId));
        detail.setOwnerRatingAvg(rating.getAverage());
        detail.setOwnerRatingCount(rating.getCount());
        detail.setFuelType(FuelType.valueOf(getString(vehicle, "fuel_type")));
        detail.setDescription(getString(vehicle, "description"));
        detail.setAddress(getString(vehicle, "address"));
        detail.setLatitude(getDoubleOrNull(vehicle, "latitude"));
        detail.setLongitude(getDoubleOrNull(vehicle, "longitude"));
        List<String> photoPaths = new ArrayList<>();
        for (JSONObject photo : store.where(VEHICLE_PHOTOS, "vehicle_id", vehicleId)) {
            photoPaths.add(getString(photo, "file_path"));
        }
        detail.setPhotoPaths(photoPaths);
        detail.setOwnerId(ownerId);
        detail.setOwnerName(FakeVehicles.shortName(store.find(USERS, ownerId)));
        detail.setPublished(getBoolean(vehicle, "_published"));
        detail.setAvailableUntil(getLongOrNull(vehicle, "available_until"));
        return Result.success(detail);
    }

    @Override
    public List<OwnerVehicleItem> getOwnerVehicles(long ownerId) {
        List<OwnerVehicleItem> result = new ArrayList<>();
        for (JSONObject vehicle : store.where(VEHICLES, "owner_id", ownerId)) {
            String badge = getString(vehicle, "_badge");
            if (badge == null) {
                continue;
            }
            long vehicleId = getLong(vehicle, "id");
            OwnerVehicleItem item = new OwnerVehicleItem();
            item.setId(vehicleId);
            item.setTitle(FakeVehicles.title(vehicle));
            item.setPlate(getString(vehicle, "license_plate"));
            item.setZoneName(FakeVehicles.zoneName(store, vehicle));
            item.setThumbnailPath(FakeVehicles.thumbnail(store, vehicleId));
            item.setBadge(VehicleBadge.valueOf(badge));
            item.setApproved(getBoolean(vehicle, "_approved"));
            item.setPublished(getBoolean(vehicle, "_published"));
            item.setAvailableUntil(getLongOrNull(vehicle, "available_until"));
            for (JSONObject document : store.where(VEHICLE_DOCUMENTS, "vehicle_id", vehicleId)) {
                String status = getString(document, "status");
                if (DocumentStatus.PENDING.name().equals(status)) {
                    item.setPendingDocuments(item.getPendingDocuments() + 1);
                } else if (DocumentStatus.REJECTED.name().equals(status) && item.getRejectedDocument() == null) {
                    item.setRejectedDocument(DocumentType.valueOf(getString(document, "document_type")));
                }
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public Result<Long> registerVehicle(long ownerId, VehicleForm form, List<Uri> photos, Uri insurance, Uri registration) {
        RepoError forced = forced(VehicleError.INVALID_VIN, VehicleError.VIN_TAKEN, VehicleError.VIN_RESTORABLE,
                VehicleError.PLATE_TAKEN, VehicleError.TOO_OLD, VehicleError.COMPACT_DELIVERY_ONLY,
                VehicleError.INVALID_INPUT, VehicleError.PHOTO_REQUIRED, VehicleError.DOCUMENTS_REQUIRED,
                VehicleError.FILE_ERROR, VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        String vin = normalize(form.getVin());
        JSONObject sameVin = findBy("vin", vin);
        if (sameVin != null) {
            boolean restorable = !sameVin.isNull("deleted_at") && getLong(sameVin, "owner_id") == ownerId;
            return restorable
                    ? Result.error(VehicleError.VIN_RESTORABLE, "Vehicle " + getLong(sameVin, "id") + " can be restored")
                    : Result.error(VehicleError.VIN_TAKEN, "VIN " + vin + " is already registered");
        }
        String plate = normalize(form.getPlate());
        if (findBy("license_plate", plate) != null) {
            return Result.error(VehicleError.PLATE_TAKEN, "Plate " + plate + " is already registered");
        }

        long now = store.now();
        JSONObject vehicle = new JSONObject();
        set(vehicle, "owner_id", ownerId);
        set(vehicle, "vin", vin);
        copyForm(vehicle, form);
        set(vehicle, "available_until", null);
        set(vehicle, "deleted_at", null);
        set(vehicle, "created_at", now);
        markPendingReview(vehicle);
        long vehicleId = store.insert(VEHICLES, vehicle);

        if (photos != null) {
            for (Uri photo : photos) {
                insertPhoto(vehicleId, photo);
            }
        }
        insertDocument(vehicleId, DocumentType.INSURANCE, insurance, now);
        insertDocument(vehicleId, DocumentType.REGISTRATION, registration, now);
        return Result.success(vehicleId);
    }

    @Override
    public Result<Long> restoreVehicle(long ownerId, String vin) {
        RepoError forced = forced(VehicleError.NOT_FOUND, VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = findBy("vin", normalize(vin));
        if (vehicle == null || vehicle.isNull("deleted_at") || getLong(vehicle, "owner_id") != ownerId) {
            return Result.error(VehicleError.NOT_FOUND, "No deleted vehicle with VIN " + vin);
        }
        long vehicleId = getLong(vehicle, "id");
        set(vehicle, "deleted_at", null);
        set(vehicle, "available_until", null);
        markPendingReview(vehicle);
        for (JSONObject document : store.where(VEHICLE_DOCUMENTS, "vehicle_id", vehicleId)) {
            set(document, "status", DocumentStatus.PENDING.name());
            set(document, "rejection_reason", null);
            set(document, "reviewed_by", null);
            set(document, "reviewed_at", null);
            set(document, "_allowed_action", DocumentAction.REPLACE.name());
        }
        return Result.success(vehicleId);
    }

    @Override
    public Result<VehicleEditData> getVehicleForEdit(long ownerId, long vehicleId) {
        RepoError forced = forced(VehicleError.NOT_FOUND, VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (vehicle == null) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        VehicleForm form = new VehicleForm();
        form.setMake(getString(vehicle, "make"));
        form.setModel(getString(vehicle, "model"));
        form.setVin(getString(vehicle, "vin"));
        form.setPlate(getString(vehicle, "license_plate"));
        form.setDescription(getString(vehicle, "description"));
        form.setAddress(getString(vehicle, "address"));
        form.setYear(getInt(vehicle, "year"));
        form.setCategory(VehicleCategory.valueOf(getString(vehicle, "category")));
        form.setFuelType(FuelType.valueOf(getString(vehicle, "fuel_type")));
        form.setServiceType(VehicleServiceType.valueOf(getString(vehicle, "service_type")));
        form.setHourlyRateCents(getInt(vehicle, "hourly_rate_cents"));
        form.setZoneId(getLong(vehicle, "zone_id"));
        form.setLatitude(getDoubleOrNull(vehicle, "latitude"));
        form.setLongitude(getDoubleOrNull(vehicle, "longitude"));
        List<PhotoItem> photos = new ArrayList<>();
        for (JSONObject row : store.where(VEHICLE_PHOTOS, "vehicle_id", vehicleId)) {
            PhotoItem photo = new PhotoItem();
            photo.setId(getLong(row, "id"));
            photo.setFilePath(getString(row, "file_path"));
            photos.add(photo);
        }
        VehicleEditData data = new VehicleEditData();
        data.setVehicleId(vehicleId);
        data.setForm(form);
        data.setPhotos(photos);
        return Result.success(data);
    }

    @Override
    public Result<Void> updateVehicle(long ownerId, long vehicleId, VehicleForm form) {
        RepoError forced = forced(VehicleError.PLATE_TAKEN, VehicleError.TOO_OLD, VehicleError.COMPACT_DELIVERY_ONLY,
                VehicleError.INVALID_INPUT, VehicleError.VEHICLE_DELETED, VehicleError.NOT_FOUND,
                VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (vehicle == null) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        String plate = normalize(form.getPlate());
        JSONObject samePlate = findBy("license_plate", plate);
        if (samePlate != null && getLong(samePlate, "id") != vehicleId) {
            return Result.error(VehicleError.PLATE_TAKEN, "Plate " + plate + " is already registered");
        }
        copyForm(vehicle, form);
        return Result.success(null);
    }

    @Override
    public Result<Long> addVehiclePhoto(long ownerId, long vehicleId, Uri photo) {
        RepoError forced = forced(VehicleError.FILE_ERROR, VehicleError.VEHICLE_DELETED, VehicleError.NOT_FOUND,
                VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(VEHICLES, vehicleId) == null) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        return Result.success(insertPhoto(vehicleId, photo));
    }

    @Override
    public Result<Void> deleteVehiclePhoto(long ownerId, long photoId) {
        RepoError forced = forced(VehicleError.LAST_PHOTO, VehicleError.NOT_FOUND, VehicleError.NOT_ALLOWED,
                VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(VEHICLE_PHOTOS, photoId) == null) {
            return Result.error(VehicleError.NOT_FOUND, "Photo " + photoId + " not found");
        }
        store.delete(VEHICLE_PHOTOS, photoId);
        return Result.success(null);
    }

    @Override
    public Result<Void> publish(long ownerId, long vehicleId, long availableUntil) {
        RepoError forced = forced(VehicleError.NOT_APPROVED, VehicleError.DATE_NOT_FUTURE, VehicleError.VEHICLE_DELETED,
                VehicleError.NOT_FOUND, VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (vehicle == null) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        set(vehicle, "available_until", availableUntil);
        set(vehicle, "_published", true);
        set(vehicle, "_badge", VehicleBadge.PUBLISHED.name());
        return Result.success(null);
    }

    @Override
    public Result<Void> unpublish(long ownerId, long vehicleId) {
        RepoError forced = forced(VehicleError.NOT_FOUND, VehicleError.NOT_ALLOWED, VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (vehicle == null) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        set(vehicle, "available_until", null);
        set(vehicle, "_published", false);
        set(vehicle, "_badge", VehicleBadge.NOT_PUBLISHED.name());
        return Result.success(null);
    }

    @Override
    public Result<Integer> deleteVehicle(long ownerId, long vehicleId) {
        RepoError forced = forced(VehicleError.HAS_ACTIVE_BOOKINGS, VehicleError.NOT_FOUND, VehicleError.NOT_ALLOWED,
                VehicleError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (vehicle == null || !vehicle.isNull("deleted_at")) {
            return Result.error(VehicleError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        set(vehicle, "deleted_at", store.now());
        set(vehicle, "_published", false);
        set(vehicle, "_badge", null);
        int rejected = 0;
        for (JSONObject booking : store.where(BOOKINGS, "vehicle_id", vehicleId)) {
            if (BookingStatus.PENDING.name().equals(getString(booking, "status"))) {
                FakeBookingUpdates.close(store, booking, BookingStatus.REJECTED, StatusReasons.VEHICLE_UNAVAILABLE);
                rejected++;
            }
        }
        return Result.success(rejected);
    }

    private boolean matches(JSONObject vehicle, SearchFilter filter) {
        if (filter == null) {
            return true;
        }
        if (filter.getZoneId() != null && getLong(vehicle, "zone_id") != filter.getZoneId()) {
            return false;
        }
        if (filter.getCategory() != null && !filter.getCategory().name().equals(getString(vehicle, "category"))) {
            return false;
        }
        if (filter.getService() == SearchService.TAXI
                && !VehicleServiceType.TAXI_AND_DELIVERY.name().equals(getString(vehicle, "service_type"))) {
            return false;
        }
        return filter.getMaxRateCents() == null || getInt(vehicle, "hourly_rate_cents") <= filter.getMaxRateCents();
    }

    private VehicleSummary toSummary(JSONObject vehicle) {
        long vehicleId = getLong(vehicle, "id");
        RatingSummary rating = FakeRatings.of(store, getLong(vehicle, "owner_id"));
        VehicleSummary summary = new VehicleSummary();
        summary.setId(vehicleId);
        summary.setMake(getString(vehicle, "make"));
        summary.setModel(getString(vehicle, "model"));
        summary.setYear(getInt(vehicle, "year"));
        summary.setCategory(VehicleCategory.valueOf(getString(vehicle, "category")));
        summary.setServiceType(VehicleServiceType.valueOf(getString(vehicle, "service_type")));
        summary.setHourlyRateCents(getInt(vehicle, "hourly_rate_cents"));
        summary.setZoneName(FakeVehicles.zoneName(store, vehicle));
        summary.setThumbnailPath(FakeVehicles.thumbnail(store, vehicleId));
        summary.setOwnerRatingAvg(rating.getAverage());
        summary.setOwnerRatingCount(rating.getCount());
        summary.setAvailableUntil(getLong(vehicle, "available_until"));
        return summary;
    }

    private void copyForm(JSONObject vehicle, VehicleForm form) {
        set(vehicle, "zone_id", form.getZoneId());
        set(vehicle, "license_plate", normalize(form.getPlate()));
        set(vehicle, "make", form.getMake());
        set(vehicle, "model", form.getModel());
        set(vehicle, "year", form.getYear());
        set(vehicle, "category", form.getCategory() == null ? null : form.getCategory().name());
        set(vehicle, "fuel_type", form.getFuelType() == null ? null : form.getFuelType().name());
        set(vehicle, "service_type", form.getServiceType() == null ? null : form.getServiceType().name());
        set(vehicle, "hourly_rate_cents", form.getHourlyRateCents());
        set(vehicle, "description", form.getDescription());
        set(vehicle, "address", form.getAddress());
        set(vehicle, "latitude", form.getLatitude());
        set(vehicle, "longitude", form.getLongitude());
    }

    private static void markPendingReview(JSONObject vehicle) {
        set(vehicle, "_approved", false);
        set(vehicle, "_published", false);
        set(vehicle, "_badge", VehicleBadge.PENDING_REVIEW.name());
    }

    private long insertPhoto(long vehicleId, Uri photo) {
        JSONObject row = new JSONObject();
        set(row, "vehicle_id", vehicleId);
        set(row, "file_path", photo == null ? null : photo.toString());
        set(row, "uploaded_at", store.now());
        return store.insert(VEHICLE_PHOTOS, row);
    }

    private void insertDocument(long vehicleId, DocumentType type, Uri file, long now) {
        JSONObject row = new JSONObject();
        set(row, "vehicle_id", vehicleId);
        set(row, "document_type", type.name());
        set(row, "file_path", file == null ? null : file.toString());
        set(row, "status", DocumentStatus.PENDING.name());
        set(row, "rejection_reason", null);
        set(row, "reviewed_by", null);
        set(row, "reviewed_at", null);
        set(row, "uploaded_at", now);
        set(row, "_allowed_action", DocumentAction.REPLACE.name());
        store.insert(VEHICLE_DOCUMENTS, row);
    }

    private JSONObject findBy(String column, String value) {
        for (JSONObject vehicle : store.rows(VEHICLES)) {
            if (value != null && value.equals(normalize(getString(vehicle, column)))) {
                return vehicle;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static RepoError forced(VehicleError first, VehicleError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
