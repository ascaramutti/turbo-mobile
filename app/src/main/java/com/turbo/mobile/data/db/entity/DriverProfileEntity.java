package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.turbo.mobile.data.model.LicenseClass;

@Entity(tableName = "driver_profiles",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "user_id",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index(value = "user_id", unique = true), @Index(value = "license_number", unique = true)})
public class DriverProfileEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    @ColumnInfo(name = "license_number")
    public String licenseNumber = "";

    @NonNull
    @ColumnInfo(name = "license_class")
    public LicenseClass licenseClass = LicenseClass.CLASS_5;

    @ColumnInfo(name = "license_expiry")
    public long licenseExpiry;

    @NonNull
    @ColumnInfo(name = "study_permit_number")
    public String studyPermitNumber = "";

    @ColumnInfo(name = "study_permit_expiry")
    public long studyPermitExpiry;
}
