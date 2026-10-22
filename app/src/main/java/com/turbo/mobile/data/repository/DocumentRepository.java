package com.turbo.mobile.data.repository;

import android.net.Uri;

import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.DocumentReviewContext;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewOutcome;
import com.turbo.mobile.data.model.VehicleDocumentItem;

import java.util.List;

public interface DocumentRepository {

    Result<DriverStatus> getDriverStatus(long driverId);

    List<DriverDocumentItem> getDriverDocuments(long driverId);

    Result<Void> uploadDriverDocument(long driverId, DocumentType type, Uri image, long expiryDate);

    List<VehicleDocumentItem> getVehicleDocuments(long vehicleId);

    Result<Void> uploadVehicleDocument(long ownerId, long vehicleId, DocumentType type, Uri image);

    List<PendingDocumentItem> getPendingDocuments(DocumentKind kind);

    Result<DocumentReviewContext> getDocumentForReview(DocumentKind kind, long documentId);

    Result<ReviewOutcome> approve(long adminId, DocumentKind kind, long documentId);

    Result<Void> reject(long adminId, DocumentKind kind, long documentId, String reason);
}
