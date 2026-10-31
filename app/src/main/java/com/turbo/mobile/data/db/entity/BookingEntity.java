package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.ServiceType;

@Entity(tableName = "bookings",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "driver_id",
                        onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = VehicleEntity.class, parentColumns = "id", childColumns = "vehicle_id",
                        onDelete = ForeignKey.RESTRICT)},
        indices = {@Index({"vehicle_id", "start_time"}), @Index({"driver_id", "start_time"}), @Index("status")})
public class BookingEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "driver_id")
    public long driverId;

    @ColumnInfo(name = "vehicle_id")
    public long vehicleId;

    @ColumnInfo(name = "start_time")
    public long startTime;

    @ColumnInfo(name = "end_time")
    public long endTime;

    @ColumnInfo(name = "total_price_cents")
    public int totalPriceCents;

    @NonNull
    @ColumnInfo(name = "service_type")
    public ServiceType serviceType = ServiceType.DELIVERY;

    @NonNull
    @ColumnInfo(name = "status", defaultValue = "PENDING")
    public BookingStatus status = BookingStatus.PENDING;

    @Nullable
    @ColumnInfo(name = "status_reason")
    public String statusReason;

    @Nullable
    @ColumnInfo(name = "started_at")
    public Long startedAt;

    @Nullable
    @ColumnInfo(name = "completed_at")
    public Long completedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
