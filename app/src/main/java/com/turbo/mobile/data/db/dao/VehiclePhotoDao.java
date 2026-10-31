package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.VehiclePhotoEntity;

import java.util.List;

@Dao
public interface VehiclePhotoDao {

    @Insert
    long insert(VehiclePhotoEntity photo);

    @Query("SELECT * FROM vehicle_photos WHERE id = :id")
    VehiclePhotoEntity findById(long id);

    @Query("SELECT * FROM vehicle_photos WHERE vehicle_id = :vehicleId ORDER BY id")
    List<VehiclePhotoEntity> getAll(long vehicleId);

    @Query("DELETE FROM vehicle_photos WHERE id = :id")
    void delete(long id);
}
