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

    @Query("SELECT * FROM bookings WHERE driver_id = :driverId ORDER BY start_time, id")
    List<BookingEntity> getByDriver(long driverId);

    @Query("SELECT * FROM bookings WHERE driver_id = :driverId "
            + "AND status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED') "
            + "AND start_time >= :weekStart AND start_time < :weekEnd")
    List<BookingEntity> getWeekBookings(long driverId, long weekStart, long weekEnd);

    @Query("SELECT id FROM bookings WHERE vehicle_id = :vehicleId AND id != :bookingId AND status = 'PENDING' "
            + "AND start_time < :end AND end_time > :start ORDER BY id")
    List<Long> getOverlappingPendingIds(long vehicleId, long bookingId, long start, long end);

    @Query("SELECT * FROM bookings WHERE status = 'PENDING' AND start_time - 900000 <= :now")
    List<BookingEntity> getExpiredRequests(long now);

    @Query("SELECT * FROM bookings WHERE status = 'CONFIRMED' AND start_time - 900000 <= :now "
            + "AND id NOT IN (SELECT booking_id FROM payments)")
    List<BookingEntity> getNotPaid(long now);

    @Query("SELECT * FROM bookings WHERE status = 'CONFIRMED' AND start_time + 3600000 < :now "
            + "AND id IN (SELECT booking_id FROM payments)")
    List<BookingEntity> getNoShows(long now);
}
