package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "user_id",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index({"user_id", "is_read"})})
public class NotificationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @NonNull
    @ColumnInfo(name = "message")
    public String message = "";

    @ColumnInfo(name = "is_read", defaultValue = "0")
    public boolean isRead;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
