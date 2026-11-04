package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.VehicleEntity;

import java.util.List;

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

    @Query("SELECT * FROM vehicles WHERE deleted_at IS NULL AND (:zoneId IS NULL OR zone_id = :zoneId) "
            + "AND (:category IS NULL OR category = :category) "
            + "AND (:taxiOnly = 0 OR service_type = 'TAXI_AND_DELIVERY') "
            + "AND (:maxRateCents IS NULL OR hourly_rate_cents <= :maxRateCents) "
            + "ORDER BY hourly_rate_cents, id")
    List<VehicleEntity> search(Long zoneId, String category, boolean taxiOnly, Integer maxRateCents);

    @Query("SELECT * FROM vehicles WHERE owner_id = :ownerId AND deleted_at IS NULL ORDER BY id")
    List<VehicleEntity> getByOwner(long ownerId);
}
