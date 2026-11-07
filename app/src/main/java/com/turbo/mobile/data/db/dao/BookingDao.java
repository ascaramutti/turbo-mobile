package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.BookingEntity;

import java.util.List;

@Dao
public interface BookingDao {

    @Insert
    long insert(BookingEntity booking);

    @Update
    void update(BookingEntity booking);

    @Query("SELECT * FROM bookings WHERE id = :id")
    BookingEntity findById(long id);

    @Query("SELECT * FROM bookings WHERE driver_id = :driverId AND status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')")
    List<BookingEntity> getOpenByDriver(long driverId);

    @Query("SELECT b.* FROM bookings b JOIN vehicles v ON v.id = b.vehicle_id "
            + "WHERE v.owner_id = :ownerId AND b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')")
    List<BookingEntity> getOpenByOwner(long ownerId);

    @Query("SELECT * FROM bookings WHERE vehicle_id = :vehicleId AND status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')")
    List<BookingEntity> getOpenByVehicle(long vehicleId);
}
