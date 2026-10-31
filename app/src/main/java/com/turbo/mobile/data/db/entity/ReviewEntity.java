package com.turbo.mobile.data.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviews",
        foreignKeys = {
                @ForeignKey(entity = BookingEntity.class, parentColumns = "id", childColumns = "booking_id",
                        onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "reviewer_id",
                        onDelete = ForeignKey.RESTRICT)},
        indices = {@Index(value = {"booking_id", "reviewer_id"}, unique = true), @Index("reviewer_id")})
public class ReviewEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "booking_id")
    public long bookingId;

    @ColumnInfo(name = "reviewer_id")
    public long reviewerId;

    @ColumnInfo(name = "rating")
    public int rating;

    @Nullable
    @ColumnInfo(name = "comment")
    public String comment;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
