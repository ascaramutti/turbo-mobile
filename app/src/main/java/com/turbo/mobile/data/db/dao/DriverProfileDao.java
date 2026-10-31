package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.DriverProfileEntity;

@Dao
public interface DriverProfileDao {

    @Insert
    long insert(DriverProfileEntity profile);

    @Update
    void update(DriverProfileEntity profile);

    @Query("SELECT * FROM driver_profiles WHERE user_id = :userId")
    DriverProfileEntity findByUserId(long userId);

    @Query("SELECT * FROM driver_profiles WHERE license_number = :licenseNumber")
    DriverProfileEntity findByLicenseNumber(String licenseNumber);
}
