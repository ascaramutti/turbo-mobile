package com.turbo.mobile.data.repository.fake;

import android.net.Uri;

import com.turbo.mobile.data.model.DocumentAction;
import com.turbo.mobile.data.model.DocumentError;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.DocumentReviewContext;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.LicenseClass;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewOutcome;
import com.turbo.mobile.data.model.VehicleBadge;
import com.turbo.mobile.data.model.VehicleDocumentItem;
import com.turbo.mobile.data.repository.DocumentRepository;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.DRIVER_DOCUMENTS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.DRIVER_PROFILES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.NOTIFICATIONS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.USERS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLE_DOCUMENTS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getBoolean;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLongOrNull;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakeDocumentRepository implements DocumentRepository {

    private static final DocumentType[] DRIVER_TYPES = {
            DocumentType.DRIVERS_LICENSE,
            DocumentType.STUDY_PERMIT
    };

    private final FakeDataStore store;

    public FakeDocumentRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public Result<DriverStatus> getDriverStatus(long driverId) {
        RepoError forced = forced(DocumentError.NOT_FOUND, DocumentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject user = store.find(USERS, driverId);
        if (user == null) {
            return Result.error(DocumentError.NOT_FOUND, "User " + driverId + " not found");
        }
        DriverStatus status = new DriverStatus();
        String state = getString(user, "_driver_state");
        if (state != null) {
            status.setState(DriverState.valueOf(state));
        }
        status.setVerified(getBoolean(user, "_verified"));
        status.setCanBook(status.getState() == DriverState.VERIFIED || status.getState() == DriverState.VERIFIED_RENEW);
        JSONObject profile = findProfile(driverId);
        if (profile != null) {
            status.setLicenseClass(LicenseClass.valueOf(getString(profile, "license_class")));
            status.setLicenseExpiry(getLong(profile, "license_expiry"));
            status.setStudyPermitExpiry(getLong(profile, "study_permit_expiry"));
        }
        return Result.success(status);
    }

    @Override
    public List<DriverDocumentItem> getDriverDocuments(long driverId) {
        JSONObject profile = findProfile(driverId);
        List<DriverDocumentItem> result = new ArrayList<>();
        for (DocumentType type : DRIVER_TYPES) {
            DriverDocumentItem item = new DriverDocumentItem();
            item.setType(type);
            JSONObject row = findDriverDocument(driverId, type);
            if (row == null) {
                item.setAllowedAction(DocumentAction.UPLOAD);
            } else {
                item.setId(getLong(row, "id"));
                item.setStatus(DocumentStatus.valueOf(getString(row, "status")));
                item.setRejectionReason(getString(row, "rejection_reason"));
                item.setFilePath(getString(row, "file_path"));
                item.setUploadedAt(getLongOrNull(row, "uploaded_at"));
                item.setAllowedAction(DocumentAction.valueOf(getString(row, "_allowed_action")));
            }
            if (profile != null) {
                item.setExpiryDate(getLongOrNull(profile, expiryColumn(type)));
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public Result<Void> uploadDriverDocument(long driverId, DocumentType type, Uri image, long expiryDate) {
        RepoError forced = forced(DocumentError.ACTION_NOT_ALLOWED, DocumentError.EXPIRY_NOT_FUTURE,
                DocumentError.FILE_ERROR, DocumentError.NOT_FOUND, DocumentError.NOT_ALLOWED, DocumentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (type != DocumentType.DRIVERS_LICENSE && type != DocumentType.STUDY_PERMIT) {
            return Result.error(DocumentError.NOT_ALLOWED, type + " is not a driver document");
        }
        JSONObject user = store.find(USERS, driverId);
        JSONObject profile = findProfile(driverId);
        if (user == null || profile == null) {
            return Result.error(DocumentError.NOT_FOUND, "Driver " + driverId + " not found");
        }
        JSONObject row = findDriverDocument(driverId, type);
        if (row == null) {
            row = new JSONObject();
            set(row, "driver_id", driverId);
            set(row, "document_type", type.name());
            store.insert(DRIVER_DOCUMENTS, row);
        }
        markUploaded(row, image);
        set(profile, expiryColumn(type), expiryDate);

        set(user, "_verified", false);
        JSONObject other = findDriverDocument(driverId, otherDriverType(type));
        set(user, "_driver_state", statusOf(other) == DocumentStatus.REJECTED
                ? DriverState.DOCUMENT_REJECTED.name()
                : DriverState.PENDING_VERIFICATION.name());
        return Result.success(null);
    }

    @Override
    public List<VehicleDocumentItem> getVehicleDocuments(long vehicleId) {
        List<VehicleDocumentItem> result = new ArrayList<>();
        for (JSONObject row : store.where(VEHICLE_DOCUMENTS, "vehicle_id", vehicleId)) {
            VehicleDocumentItem item = new VehicleDocumentItem();
            item.setId(getLong(row, "id"));
            item.setType(DocumentType.valueOf(getString(row, "document_type")));
            item.setStatus(DocumentStatus.valueOf(getString(row, "status")));
            item.setRejectionReason(getString(row, "rejection_reason"));
            item.setFilePath(getString(row, "file_path"));
            item.setUploadedAt(getLong(row, "uploaded_at"));
            item.setAllowedAction(DocumentAction.valueOf(getString(row, "_allowed_action")));
            result.add(item);
        }
        return result;
    }

    @Override
    public Result<Void> uploadVehicleDocument(long ownerId, long vehicleId, DocumentType type, Uri image) {
        RepoError forced = forced(DocumentError.ACTION_NOT_ALLOWED, DocumentError.VEHICLE_DELETED,
                DocumentError.FILE_ERROR, DocumentError.NOT_FOUND, DocumentError.NOT_ALLOWED, DocumentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        JSONObject row = findVehicleDocument(vehicleId, type);
        if (vehicle == null || row == null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + type + " of vehicle " + vehicleId + " not found");
        }
        markUploaded(row, image);

        set(vehicle, "_approved", false);
        set(vehicle, "_published", false);
        JSONObject other = findVehicleDocument(vehicleId, otherVehicleType(type));
        set(vehicle, "_badge", statusOf(other) == DocumentStatus.REJECTED
                ? VehicleBadge.ACTION_NEEDED.name()
                : VehicleBadge.PENDING_REVIEW.name());
        return Result.success(null);
    }

    @Override
    public List<PendingDocumentItem> getPendingDocuments(DocumentKind kind) {
        List<PendingDocumentItem> result = new ArrayList<>();
        for (JSONObject row : store.rows(table(kind))) {
            if (statusOf(row) != DocumentStatus.PENDING) {
                continue;
            }
            PendingDocumentItem item = new PendingDocumentItem();
            item.setKind(kind);
            item.setDocumentId(getLong(row, "id"));
            item.setType(DocumentType.valueOf(getString(row, "document_type")));
            item.setSubjectName(kind == DocumentKind.DRIVER
                    ? fullName(store.find(USERS, getLong(row, "driver_id")))
                    : FakeVehicles.title(store.find(VEHICLES, getLong(row, "vehicle_id"))));
            item.setUploadedAt(getLong(row, "uploaded_at"));
            item.setFilePath(getString(row, "file_path"));
            result.add(item);
        }
        result.sort((a, b) -> a.getUploadedAt() != b.getUploadedAt()
                ? Long.compare(a.getUploadedAt(), b.getUploadedAt())
                : Long.compare(a.getDocumentId(), b.getDocumentId()));
        return result;
    }

    @Override
    public Result<DocumentReviewContext> getDocumentForReview(DocumentKind kind, long documentId) {
        RepoError forced = forced(DocumentError.NOT_FOUND, DocumentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject row = store.find(table(kind), documentId);
        if (row == null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        DocumentType type = DocumentType.valueOf(getString(row, "document_type"));
        DocumentReviewContext context = new DocumentReviewContext();
        context.setKind(kind);
        context.setDocumentId(documentId);
        context.setType(type);
        context.setFilePath(getString(row, "file_path"));
        context.setStatus(statusOf(row));
        if (kind == DocumentKind.DRIVER) {
            long driverId = getLong(row, "driver_id");
            context.setOtherDocumentStatus(statusOf(findDriverDocument(driverId, otherDriverType(type))));
            context.setDriverName(fullName(store.find(USERS, driverId)));
            JSONObject profile = findProfile(driverId);
            if (profile != null) {
                context.setDocumentNumber(getString(profile, type == DocumentType.DRIVERS_LICENSE
                        ? "license_number"
                        : "study_permit_number"));
                context.setExpiryDate(getLongOrNull(profile, expiryColumn(type)));
                context.setLicenseClass(LicenseClass.valueOf(getString(profile, "license_class")));
            }
        } else {
            long vehicleId = getLong(row, "vehicle_id");
            JSONObject vehicle = store.find(VEHICLES, vehicleId);
            context.setOtherDocumentStatus(statusOf(findVehicleDocument(vehicleId, otherVehicleType(type))));
            context.setVehicleTitle(FakeVehicles.title(vehicle));
            context.setOwnerName(fullName(store.find(USERS, getLong(vehicle, "owner_id"))));
            context.setVin(getString(vehicle, "vin"));
            context.setPlate(getString(vehicle, "license_plate"));
        }
        return Result.success(context);
    }

    @Override
    public Result<ReviewOutcome> approve(long adminId, DocumentKind kind, long documentId) {
        RepoError forced = forced(DocumentError.NOT_PENDING, DocumentError.NOT_FOUND, DocumentError.NOT_ALLOWED,
                DocumentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject row = store.find(table(kind), documentId);
        if (row == null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        markReviewed(row, adminId, DocumentStatus.APPROVED, null, DocumentAction.NONE);

        DocumentType type = DocumentType.valueOf(getString(row, "document_type"));
        ReviewOutcome outcome = new ReviewOutcome();
        if (kind == DocumentKind.DRIVER) {
            long driverId = getLong(row, "driver_id");
            JSONObject user = store.find(USERS, driverId);
            DocumentStatus other = statusOf(findDriverDocument(driverId, otherDriverType(type)));
            if (other == DocumentStatus.APPROVED) {
                set(user, "_verified", true);
                set(user, "_driver_state", DriverState.VERIFIED.name());
                notify(driverId, "Account verified", "Both documents are approved. You can book shifts now.");
                outcome.setCompleted(true);
            } else {
                set(user, "_driver_state", other == DocumentStatus.REJECTED
                        ? DriverState.DOCUMENT_REJECTED.name()
                        : DriverState.PENDING_VERIFICATION.name());
            }
        } else {
            long vehicleId = getLong(row, "vehicle_id");
            JSONObject vehicle = store.find(VEHICLES, vehicleId);
            DocumentStatus other = statusOf(findVehicleDocument(vehicleId, otherVehicleType(type)));
            if (other == DocumentStatus.APPROVED) {
                set(vehicle, "_approved", true);
                set(vehicle, "_badge", VehicleBadge.NOT_PUBLISHED.name());
                outcome.setCompleted(true);
            } else {
                set(vehicle, "_badge", other == DocumentStatus.REJECTED
                        ? VehicleBadge.ACTION_NEEDED.name()
                        : VehicleBadge.PENDING_REVIEW.name());
            }
        }
        return Result.success(outcome);
    }

    @Override
    public Result<Void> reject(long adminId, DocumentKind kind, long documentId, String reason) {
        RepoError forced = forced(DocumentError.REASON_TOO_SHORT, DocumentError.NOT_PENDING, DocumentError.NOT_FOUND,
                DocumentError.NOT_ALLOWED, DocumentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject row = store.find(table(kind), documentId);
        if (row == null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        markReviewed(row, adminId, DocumentStatus.REJECTED, reason == null ? null : reason.trim(), DocumentAction.REUPLOAD);

        if (kind == DocumentKind.DRIVER) {
            JSONObject user = store.find(USERS, getLong(row, "driver_id"));
            set(user, "_verified", false);
            set(user, "_driver_state", DriverState.DOCUMENT_REJECTED.name());
        } else {
            JSONObject vehicle = store.find(VEHICLES, getLong(row, "vehicle_id"));
            set(vehicle, "_approved", false);
            set(vehicle, "_published", false);
            set(vehicle, "_badge", VehicleBadge.ACTION_NEEDED.name());
        }
        return Result.success(null);
    }

    private void markUploaded(JSONObject row, Uri image) {
        set(row, "file_path", image == null ? null : image.toString());
        set(row, "status", DocumentStatus.PENDING.name());
        set(row, "rejection_reason", null);
        set(row, "reviewed_by", null);
        set(row, "reviewed_at", null);
        set(row, "uploaded_at", store.now());
        set(row, "_allowed_action", DocumentAction.REPLACE.name());
    }

    private void markReviewed(JSONObject row, long adminId, DocumentStatus status, String reason, DocumentAction action) {
        set(row, "status", status.name());
        set(row, "rejection_reason", reason);
        set(row, "reviewed_by", adminId);
        set(row, "reviewed_at", store.now());
        set(row, "_allowed_action", action.name());
    }

    private void notify(long userId, String title, String message) {
        JSONObject row = new JSONObject();
        set(row, "user_id", userId);
        set(row, "title", title);
        set(row, "message", message);
        set(row, "is_read", 0);
        set(row, "created_at", store.now());
        store.insert(NOTIFICATIONS, row);
    }

    private JSONObject findProfile(long driverId) {
        List<JSONObject> profiles = store.where(DRIVER_PROFILES, "user_id", driverId);
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private JSONObject findDriverDocument(long driverId, DocumentType type) {
        return findByType(store.where(DRIVER_DOCUMENTS, "driver_id", driverId), type);
    }

    private JSONObject findVehicleDocument(long vehicleId, DocumentType type) {
        return findByType(store.where(VEHICLE_DOCUMENTS, "vehicle_id", vehicleId), type);
    }

    private static JSONObject findByType(List<JSONObject> rows, DocumentType type) {
        for (JSONObject row : rows) {
            if (type.name().equals(getString(row, "document_type"))) {
                return row;
            }
        }
        return null;
    }

    private static DocumentStatus statusOf(JSONObject row) {
        return row == null ? null : DocumentStatus.valueOf(getString(row, "status"));
    }

    private static String table(DocumentKind kind) {
        return kind == DocumentKind.DRIVER ? DRIVER_DOCUMENTS : VEHICLE_DOCUMENTS;
    }

    private static String expiryColumn(DocumentType type) {
        return type == DocumentType.DRIVERS_LICENSE ? "license_expiry" : "study_permit_expiry";
    }

    private static DocumentType otherDriverType(DocumentType type) {
        return type == DocumentType.DRIVERS_LICENSE ? DocumentType.STUDY_PERMIT : DocumentType.DRIVERS_LICENSE;
    }

    private static DocumentType otherVehicleType(DocumentType type) {
        return type == DocumentType.INSURANCE ? DocumentType.REGISTRATION : DocumentType.INSURANCE;
    }

    private static String fullName(JSONObject user) {
        return user == null ? null : getString(user, "first_name") + " " + getString(user, "last_name");
    }

    private static RepoError forced(DocumentError first, DocumentError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
