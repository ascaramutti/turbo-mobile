package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.PaymentEntity;

@Dao
public interface PaymentDao {

    @Insert
    long insert(PaymentEntity payment);

    @Query("SELECT * FROM payments WHERE booking_id = :bookingId")
    PaymentEntity findByBooking(long bookingId);
}
