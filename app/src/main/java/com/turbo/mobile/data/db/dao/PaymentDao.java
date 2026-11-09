package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.PaymentEntity;

import java.util.List;

@Dao
public interface PaymentDao {

    @Insert
    long insert(PaymentEntity payment);

    @Query("SELECT * FROM payments WHERE booking_id = :bookingId")
    PaymentEntity findByBooking(long bookingId);

    @Query("SELECT p.* FROM payments p JOIN bookings b ON b.id = p.booking_id WHERE b.driver_id = :driverId")
    List<PaymentEntity> getByDriver(long driverId);

    @Query("SELECT * FROM payments ORDER BY paid_at DESC, booking_id DESC")
    List<PaymentEntity> getAll();
}
