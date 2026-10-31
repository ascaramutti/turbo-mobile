package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.turbo.mobile.data.model.FuelType;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleServiceType;

@Entity(tableName = "vehicles",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "owner_id",
                        onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = ZoneEntity.class, parentColumns = "id", childColumns = "zone_id",
                        onDelete = ForeignKey.RESTRICT)},
        indices = {@Index(value = "vin", unique = true), @Index(value = "license_plate", unique = true),
                @Index("owner_id"), @Index({"zone_id", "available_until"})})
public class VehicleEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "owner_id")
    public long ownerId;

    @ColumnInfo(name = "zone_id")
    public long zoneId;

    @NonNull
    @ColumnInfo(name = "vin")
    public String vin = "";

    @NonNull
    @ColumnInfo(name = "license_plate")
    public String licensePlate = "";

    @NonNull
    @ColumnInfo(name = "make")
    public String make = "";

    @NonNull
    @ColumnInfo(name = "model")
    public String model = "";

    @ColumnInfo(name = "year")
    public int year;

    @NonNull
    @ColumnInfo(name = "category")
    public VehicleCategory category = VehicleCategory.SEDAN;

    @NonNull
    @ColumnInfo(name = "fuel_type")
    public FuelType fuelType = FuelType.GASOLINE;

    @NonNull
    @ColumnInfo(name = "service_type")
    public VehicleServiceType serviceType = VehicleServiceType.DELIVERY_ONLY;

    @ColumnInfo(name = "hourly_rate_cents")
    public int hourlyRateCents;

    @Nullable
    @ColumnInfo(name = "description")
    public String description;

    @NonNull
    @ColumnInfo(name = "address")
    public String address = "";

    @Nullable
    @ColumnInfo(name = "latitude")
    public Double latitude;

    @Nullable
    @ColumnInfo(name = "longitude")
    public Double longitude;

    @Nullable
    @ColumnInfo(name = "available_until")
    public Long availableUntil;

    @Nullable
    @ColumnInfo(name = "deleted_at")
    public Long deletedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
