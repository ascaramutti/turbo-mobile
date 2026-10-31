package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.VehicleEntity;

@Dao
public interface VehicleDao {

    @Insert
    long insert(VehicleEntity vehicle);

    @Update
    void update(VehicleEntity vehicle);

    @Query("SELECT * FROM vehicles WHERE id = :id")
    VehicleEntity findById(long id);

    @Query("SELECT * FROM vehicles WHERE vin = :vin")
    VehicleEntity findByVin(String vin);

    @Query("SELECT * FROM vehicles WHERE license_plate = :plate")
    VehicleEntity findByPlate(String plate);
}
