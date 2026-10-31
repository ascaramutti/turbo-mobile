package com.turbo.mobile.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.turbo.mobile.data.model.Role;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "email")
    public String email = "";

    @NonNull
    @ColumnInfo(name = "password_hash")
    public String passwordHash = "";

    @NonNull
    @ColumnInfo(name = "password_salt")
    public String passwordSalt = "";

    @NonNull
    @ColumnInfo(name = "first_name")
    public String firstName = "";

    @NonNull
    @ColumnInfo(name = "last_name")
    public String lastName = "";

    @NonNull
    @ColumnInfo(name = "phone")
    public String phone = "";

    @ColumnInfo(name = "date_of_birth")
    public long dateOfBirth;

    @NonNull
    @ColumnInfo(name = "role")
    public Role role = Role.DRIVER;

    @Nullable
    @ColumnInfo(name = "profile_photo_path")
    public String profilePhotoPath;

    @ColumnInfo(name = "is_active", defaultValue = "1")
    public boolean isActive = true;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
