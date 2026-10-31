package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicle_photos",
        foreignKeys = @ForeignKey(entity = VehicleEntity.class, parentColumns = "id", childColumns = "vehicle_id",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index("vehicle_id")})
public class VehiclePhotoEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "vehicle_id")
    public long vehicleId;

    @NonNull
    @ColumnInfo(name = "file_path")
    public String filePath = "";

    @ColumnInfo(name = "uploaded_at")
    public long uploadedAt;
}
