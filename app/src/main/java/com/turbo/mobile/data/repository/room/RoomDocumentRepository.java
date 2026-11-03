package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.DriverDocumentEntity;
import com.turbo.mobile.data.db.entity.DriverProfileEntity;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleDocumentEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.model.DocumentAction;
import com.turbo.mobile.data.model.DocumentError;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.DocumentReviewContext;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewOutcome;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.VehicleDocumentItem;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.util.ImageStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RoomDocumentRepository implements DocumentRepository {

    private static final String TAG = "RoomDocumentRepository";
    private static final long RENEW_WINDOW = 30L * 24 * 60 * 60 * 1000;
    private static final int MIN_REASON_LENGTH = 5;

    private final AppDatabase db;
    private final ImageStorage imageStorage;

    public RoomDocumentRepository(Context context) {
        db = AppDatabase.getInstance(context);
        imageStorage = new ImageStorage(context);
    }

    @Override
    public Result<DriverStatus> getDriverStatus(long driverId) {
        try {
            UserEntity user = db.userDao().findById(driverId);
            if (user == null) {
                return Result.error(DocumentError.NOT_FOUND, "User " + driverId + " not found");
            }
            DriverStatus status = new DriverStatus();
            DriverProfileEntity profile = db.driverProfileDao().findByUserId(driverId);
            if (user.role == Role.DRIVER && profile != null) {
                DriverState state = calculateDriverState(user, profile);
                status.setState(state);
                status.setVerified(countApproved(driverId) == 2);
                status.setCanBook(state == DriverState.VERIFIED || state == DriverState.VERIFIED_RENEW);
                status.setLicenseClass(profile.licenseClass);
                status.setLicenseExpiry(profile.licenseExpiry);
                status.setStudyPermitExpiry(profile.studyPermitExpiry);
            }
            return Result.success(status);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected database error", e);
            return Result.error(DocumentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public List<DriverDocumentItem> getDriverDocuments(long driverId) {
        List<DriverDocumentItem> items = new ArrayList<>();
        DriverProfileEntity profile = db.driverProfileDao().findByUserId(driverId);
        DocumentType[] types = {DocumentType.DRIVERS_LICENSE, DocumentType.STUDY_PERMIT};
        for (DocumentType type : types) {
            DriverDocumentItem item = new DriverDocumentItem();
            item.setType(type);
            if (profile != null) {
                item.setExpiryDate(expiryOf(profile, type));
            }
            DriverDocumentEntity document = db.driverDocumentDao().find(driverId, type);
            if (document == null) {
                item.setAllowedAction(DocumentAction.UPLOAD);
            } else {
                item.setId(document.id);
                item.setStatus(document.status);
                item.setRejectionReason(document.rejectionReason);
                item.setFilePath(document.filePath);
                item.setUploadedAt(document.uploadedAt);
                item.setAllowedAction(driverAction(document, profile));
            }
            items.add(item);
        }
        return items;
    }

    @Override
    public Result<Void> uploadDriverDocument(long driverId, DocumentType type, Uri image, long expiryDate) {
        try {
            if (type != DocumentType.DRIVERS_LICENSE && type != DocumentType.STUDY_PERMIT) {
                return Result.error(DocumentError.NOT_ALLOWED, type + " is not a driver document");
            }
            UserEntity driver = db.userDao().findById(driverId);
            DriverProfileEntity profile = db.driverProfileDao().findByUserId(driverId);
            if (driver == null || driver.role != Role.DRIVER || profile == null) {
                return Result.error(DocumentError.NOT_FOUND, "Driver " + driverId + " not found");
            }
            DriverDocumentEntity document = db.driverDocumentDao().find(driverId, type);
            if (document != null && driverAction(document, profile) == DocumentAction.NONE) {
                return Result.error(DocumentError.ACTION_NOT_ALLOWED, "The document is approved and not near expiry");
            }
            long now = System.currentTimeMillis();
            if (expiryDate <= now) {
                return Result.error(DocumentError.EXPIRY_NOT_FUTURE, "The expiry date must be in the future");
            }
            String newPath;
            try {
                newPath = imageStorage.save(image, ImageStorage.Folder.DOCUMENTS);
            } catch (IOException e) {
                Log.e(TAG, "Cannot save the document image", e);
                return Result.error(DocumentError.FILE_ERROR, "Cannot save the image");
            }
            String oldPath = null;
            if (document == null) {
                document = new DriverDocumentEntity();
                document.driverId = driverId;
                document.documentType = type;
            } else {
                oldPath = document.filePath;
            }
            document.filePath = newPath;
            document.status = DocumentStatus.PENDING;
            document.rejectionReason = null;
            document.reviewedBy = null;
            document.reviewedAt = null;
            document.uploadedAt = now;
            if (type == DocumentType.DRIVERS_LICENSE) {
                profile.licenseExpiry = expiryDate;
            } else {
                profile.studyPermitExpiry = expiryDate;
            }
            db.beginTransaction();
            try {
                if (document.id == 0) {
                    db.driverDocumentDao().insert(document);
                } else {
                    db.driverDocumentDao().update(document);
                }
                db.driverProfileDao().update(profile);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            imageStorage.delete(oldPath);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected database error", e);
            return Result.error(DocumentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public List<VehicleDocumentItem> getVehicleDocuments(long vehicleId) {
        List<VehicleDocumentItem> items = new ArrayList<>();
        for (VehicleDocumentEntity document : db.vehicleDocumentDao().getAll(vehicleId)) {
            VehicleDocumentItem item = new VehicleDocumentItem();
            item.setId(document.id);
            item.setType(document.documentType);
            item.setStatus(document.status);
            item.setRejectionReason(document.rejectionReason);
            item.setFilePath(document.filePath);
            item.setUploadedAt(document.uploadedAt);
            item.setAllowedAction(vehicleAction(document));
            items.add(item);
        }
        return items;
    }

    @Override
    public Result<Void> uploadVehicleDocument(long ownerId, long vehicleId, DocumentType type, Uri image) {
        try {
            VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
            if (vehicle == null) {
                return Result.error(DocumentError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
            }
            if (vehicle.ownerId != ownerId) {
                return Result.error(DocumentError.NOT_ALLOWED, "Only the owner can upload documents");
            }
            if (vehicle.deletedAt != null) {
                return Result.error(DocumentError.VEHICLE_DELETED, "Vehicle " + vehicleId + " was deleted");
            }
            VehicleDocumentEntity document = db.vehicleDocumentDao().find(vehicleId, type);
            if (document == null) {
                return Result.error(DocumentError.NOT_FOUND, "Document " + type + " of vehicle " + vehicleId + " not found");
            }
            if (document.status == DocumentStatus.APPROVED) {
                return Result.error(DocumentError.ACTION_NOT_ALLOWED, "Approved documents cannot be replaced");
            }
            String newPath;
            try {
                newPath = imageStorage.save(image, ImageStorage.Folder.DOCUMENTS);
            } catch (IOException e) {
                Log.e(TAG, "Cannot save the document image", e);
                return Result.error(DocumentError.FILE_ERROR, "Cannot save the image");
            }
            String oldPath = document.filePath;
            document.filePath = newPath;
            document.status = DocumentStatus.PENDING;
            document.rejectionReason = null;
            document.reviewedBy = null;
            document.reviewedAt = null;
            document.uploadedAt = System.currentTimeMillis();
            db.vehicleDocumentDao().update(document);
            imageStorage.delete(oldPath);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected database error", e);
            return Result.error(DocumentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public List<PendingDocumentItem> getPendingDocuments(DocumentKind kind) {
        List<PendingDocumentItem> items = new ArrayList<>();
        if (kind == DocumentKind.DRIVER) {
            for (DriverDocumentEntity document : db.driverDocumentDao().getPending()) {
                UserEntity driver = db.userDao().findById(document.driverId);
                items.add(pendingItem(kind, document.id, document.documentType, fullName(driver),
                        document.uploadedAt, document.filePath));
            }
        } else {
            for (VehicleDocumentEntity document : db.vehicleDocumentDao().getPending()) {
                VehicleEntity vehicle = db.vehicleDao().findById(document.vehicleId);
                if (vehicle.deletedAt != null) {
                    continue;
                }
                items.add(pendingItem(kind, document.id, document.documentType, title(vehicle),
                        document.uploadedAt, document.filePath));
            }
        }
        return items;
    }

    @Override
    public Result<DocumentReviewContext> getDocumentForReview(DocumentKind kind, long documentId) {
        try {
            DocumentReviewContext context = new DocumentReviewContext();
            context.setKind(kind);
            context.setDocumentId(documentId);
            if (kind == DocumentKind.DRIVER) {
                DriverDocumentEntity document = db.driverDocumentDao().findById(documentId);
                if (document == null) {
                    return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
                }
                UserEntity driver = db.userDao().findById(document.driverId);
                DriverProfileEntity profile = db.driverProfileDao().findByUserId(document.driverId);
                DriverDocumentEntity other = db.driverDocumentDao().find(document.driverId, otherType(document.documentType));
                context.setType(document.documentType);
                context.setFilePath(document.filePath);
                context.setStatus(document.status);
                context.setOtherDocumentStatus(other == null ? null : other.status);
                context.setDriverName(fullName(driver));
                if (profile != null) {
                    context.setDocumentNumber(document.documentType == DocumentType.DRIVERS_LICENSE
                            ? profile.licenseNumber
                            : profile.studyPermitNumber);
                    context.setExpiryDate(expiryOf(profile, document.documentType));
                    context.setLicenseClass(profile.licenseClass);
                }
            } else {
                VehicleDocumentEntity document = db.vehicleDocumentDao().findById(documentId);
                VehicleEntity vehicle = document == null ? null : db.vehicleDao().findById(document.vehicleId);
                if (document == null || vehicle == null || vehicle.deletedAt != null) {
                    return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
                }
                UserEntity owner = db.userDao().findById(vehicle.ownerId);
                VehicleDocumentEntity other = db.vehicleDocumentDao().find(vehicle.id, otherType(document.documentType));
                context.setType(document.documentType);
                context.setFilePath(document.filePath);
                context.setStatus(document.status);
                context.setOtherDocumentStatus(other == null ? null : other.status);
                context.setVehicleTitle(title(vehicle));
                context.setOwnerName(fullName(owner));
                context.setVin(vehicle.vin);
                context.setPlate(vehicle.licensePlate);
            }
            return Result.success(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected database error", e);
            return Result.error(DocumentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<ReviewOutcome> approve(long adminId, DocumentKind kind, long documentId) {
        try {
            if (!isActiveAdmin(adminId)) {
                return Result.error(DocumentError.NOT_ALLOWED, "Only an active Admin can do this");
            }
            if (kind == DocumentKind.DRIVER) {
                return approveDriverDocument(adminId, documentId);
            }
            return approveVehicleDocument(adminId, documentId);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected database error", e);
            return Result.error(DocumentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> reject(long adminId, DocumentKind kind, long documentId, String reason) {
        try {
            if (!isActiveAdmin(adminId)) {
                return Result.error(DocumentError.NOT_ALLOWED, "Only an active Admin can do this");
            }
            if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
                return Result.error(DocumentError.REASON_TOO_SHORT, "The reason needs at least 5 characters");
            }
            if (kind == DocumentKind.DRIVER) {
                return rejectDriverDocument(adminId, documentId, reason.trim());
            }
            return rejectVehicleDocument(adminId, documentId, reason.trim());
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected database error", e);
            return Result.error(DocumentError.UNEXPECTED, e.getMessage());
        }
    }

    private Result<ReviewOutcome> approveDriverDocument(long adminId, long documentId) {
        DriverDocumentEntity document = db.driverDocumentDao().findById(documentId);
        if (document == null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        if (document.status != DocumentStatus.PENDING) {
            return Result.error(DocumentError.NOT_PENDING, "The document was already reviewed");
        }
        document.status = DocumentStatus.APPROVED;
        document.reviewedBy = adminId;
        document.reviewedAt = System.currentTimeMillis();
        DriverDocumentEntity other = db.driverDocumentDao().find(document.driverId, otherType(document.documentType));
        ReviewOutcome outcome = new ReviewOutcome();
        outcome.setCompleted(other != null && other.status == DocumentStatus.APPROVED);
        db.beginTransaction();
        try {
            db.driverDocumentDao().update(document);
            notify(document.driverId, "Document approved",
                    "Your " + label(document.documentType).toLowerCase() + " was approved.");
            if (outcome.isCompleted()) {
                notify(document.driverId, "Account verified", "Both documents are approved. You can book shifts now.");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return Result.success(outcome);
    }

    private Result<ReviewOutcome> approveVehicleDocument(long adminId, long documentId) {
        VehicleDocumentEntity document = db.vehicleDocumentDao().findById(documentId);
        VehicleEntity vehicle = document == null ? null : db.vehicleDao().findById(document.vehicleId);
        if (document == null || vehicle == null || vehicle.deletedAt != null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        if (document.status != DocumentStatus.PENDING) {
            return Result.error(DocumentError.NOT_PENDING, "The document was already reviewed");
        }
        document.status = DocumentStatus.APPROVED;
        document.reviewedBy = adminId;
        document.reviewedAt = System.currentTimeMillis();
        VehicleDocumentEntity other = db.vehicleDocumentDao().find(vehicle.id, otherType(document.documentType));
        ReviewOutcome outcome = new ReviewOutcome();
        outcome.setCompleted(other != null && other.status == DocumentStatus.APPROVED);
        String message;
        if (outcome.isCompleted()) {
            message = "Both documents of your " + title(vehicle) + " were approved. You can publish it now.";
        } else {
            message = label(document.documentType) + " of your " + title(vehicle) + " was approved.";
        }
        db.beginTransaction();
        try {
            db.vehicleDocumentDao().update(document);
            notify(vehicle.ownerId, "Document approved", message);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return Result.success(outcome);
    }

    private Result<Void> rejectDriverDocument(long adminId, long documentId, String reason) {
        DriverDocumentEntity document = db.driverDocumentDao().findById(documentId);
        if (document == null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        if (document.status != DocumentStatus.PENDING) {
            return Result.error(DocumentError.NOT_PENDING, "The document was already reviewed");
        }
        document.status = DocumentStatus.REJECTED;
        document.rejectionReason = reason;
        document.reviewedBy = adminId;
        document.reviewedAt = System.currentTimeMillis();
        db.beginTransaction();
        try {
            db.driverDocumentDao().update(document);
            notify(document.driverId, "Document rejected",
                    "Your " + label(document.documentType).toLowerCase() + " was rejected: " + reason);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return Result.success(null);
    }

    private Result<Void> rejectVehicleDocument(long adminId, long documentId, String reason) {
        VehicleDocumentEntity document = db.vehicleDocumentDao().findById(documentId);
        VehicleEntity vehicle = document == null ? null : db.vehicleDao().findById(document.vehicleId);
        if (document == null || vehicle == null || vehicle.deletedAt != null) {
            return Result.error(DocumentError.NOT_FOUND, "Document " + documentId + " not found");
        }
        if (document.status != DocumentStatus.PENDING) {
            return Result.error(DocumentError.NOT_PENDING, "The document was already reviewed");
        }
        document.status = DocumentStatus.REJECTED;
        document.rejectionReason = reason;
        document.reviewedBy = adminId;
        document.reviewedAt = System.currentTimeMillis();
        db.beginTransaction();
        try {
            db.vehicleDocumentDao().update(document);
            notify(vehicle.ownerId, "Document rejected",
                    label(document.documentType) + " of your " + title(vehicle) + " was rejected: " + reason);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return Result.success(null);
    }

    private DriverState calculateDriverState(UserEntity driver, DriverProfileEntity profile) {
        if (!driver.isActive) {
            return DriverState.DEACTIVATED;
        }
        for (DriverDocumentEntity document : db.driverDocumentDao().getAll(driver.id)) {
            if (document.status == DocumentStatus.REJECTED) {
                return DriverState.DOCUMENT_REJECTED;
            }
        }
        if (countApproved(driver.id) < 2) {
            return DriverState.PENDING_VERIFICATION;
        }
        long now = System.currentTimeMillis();
        if (Math.min(profile.licenseExpiry, profile.studyPermitExpiry) <= now + RENEW_WINDOW) {
            return DriverState.VERIFIED_RENEW;
        }
        return DriverState.VERIFIED;
    }

    private int countApproved(long driverId) {
        int approved = 0;
        for (DriverDocumentEntity document : db.driverDocumentDao().getAll(driverId)) {
            if (document.status == DocumentStatus.APPROVED) {
                approved++;
            }
        }
        return approved;
    }

    private DocumentAction driverAction(DriverDocumentEntity document, DriverProfileEntity profile) {
        if (document.status == DocumentStatus.REJECTED) {
            return DocumentAction.REUPLOAD;
        }
        if (document.status == DocumentStatus.PENDING) {
            return DocumentAction.REPLACE;
        }
        long now = System.currentTimeMillis();
        if (profile != null && expiryOf(profile, document.documentType) <= now + RENEW_WINDOW) {
            return DocumentAction.RENEW;
        }
        return DocumentAction.NONE;
    }

    private DocumentAction vehicleAction(VehicleDocumentEntity document) {
        if (document.status == DocumentStatus.REJECTED) {
            return DocumentAction.REUPLOAD;
        }
        if (document.status == DocumentStatus.PENDING) {
            return DocumentAction.REPLACE;
        }
        return DocumentAction.NONE;
    }

    private boolean isActiveAdmin(long adminId) {
        UserEntity admin = db.userDao().findById(adminId);
        return admin != null && admin.role == Role.ADMIN && admin.isActive;
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

    private PendingDocumentItem pendingItem(DocumentKind kind, long documentId, DocumentType type, String subjectName,
                                            long uploadedAt, String filePath) {
        PendingDocumentItem item = new PendingDocumentItem();
        item.setKind(kind);
        item.setDocumentId(documentId);
        item.setType(type);
        item.setSubjectName(subjectName);
        item.setUploadedAt(uploadedAt);
        item.setFilePath(filePath);
        return item;
    }

    private long expiryOf(DriverProfileEntity profile, DocumentType type) {
        if (type == DocumentType.DRIVERS_LICENSE) {
            return profile.licenseExpiry;
        }
        return profile.studyPermitExpiry;
    }

    private DocumentType otherType(DocumentType type) {
        if (type == DocumentType.DRIVERS_LICENSE) {
            return DocumentType.STUDY_PERMIT;
        }
        if (type == DocumentType.STUDY_PERMIT) {
            return DocumentType.DRIVERS_LICENSE;
        }
        if (type == DocumentType.INSURANCE) {
            return DocumentType.REGISTRATION;
        }
        return DocumentType.INSURANCE;
    }

    private String label(DocumentType type) {
        if (type == DocumentType.DRIVERS_LICENSE) {
            return "Driver's license";
        }
        if (type == DocumentType.STUDY_PERMIT) {
            return "Study permit";
        }
        if (type == DocumentType.INSURANCE) {
            return "Insurance certificate";
        }
        return "Vehicle registration";
    }

    private String fullName(UserEntity user) {
        return user.firstName + " " + user.lastName;
    }

    private String title(VehicleEntity vehicle) {
        return vehicle.make + " " + vehicle.model + " " + vehicle.year;
    }
}
