package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.turbo.mobile.data.model.PhotoType;

@Entity(tableName = "booking_photos",
        foreignKeys = @ForeignKey(entity = BookingEntity.class, parentColumns = "id", childColumns = "booking_id",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index("booking_id")})
public class BookingPhotoEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "booking_id")
    public long bookingId;

    @NonNull
    @ColumnInfo(name = "photo_type")
    public PhotoType photoType = PhotoType.PICKUP;

    @NonNull
    @ColumnInfo(name = "file_path")
    public String filePath = "";

    @ColumnInfo(name = "taken_at")
    public long takenAt;
}
