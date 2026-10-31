package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;

@Entity(tableName = "vehicle_documents",
        foreignKeys = {
                @ForeignKey(entity = VehicleEntity.class, parentColumns = "id", childColumns = "vehicle_id",
                        onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "reviewed_by",
                        onDelete = ForeignKey.SET_NULL)},
        indices = {@Index(value = {"vehicle_id", "document_type"}, unique = true), @Index("status"),
                @Index("reviewed_by")})
public class VehicleDocumentEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "vehicle_id")
    public long vehicleId;

    @NonNull
    @ColumnInfo(name = "document_type")
    public DocumentType documentType = DocumentType.INSURANCE;

    @NonNull
    @ColumnInfo(name = "file_path")
    public String filePath = "";

    @NonNull
    @ColumnInfo(name = "status", defaultValue = "PENDING")
    public DocumentStatus status = DocumentStatus.PENDING;

    @Nullable
    @ColumnInfo(name = "rejection_reason")
    public String rejectionReason;

    @Nullable
    @ColumnInfo(name = "reviewed_by")
    public Long reviewedBy;

    @Nullable
    @ColumnInfo(name = "reviewed_at")
    public Long reviewedAt;

    @ColumnInfo(name = "uploaded_at")
    public long uploadedAt;
}
