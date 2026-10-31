package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.BookingEntity;

@Dao
public interface BookingDao {

    @Insert
    long insert(BookingEntity booking);

    @Update
    void update(BookingEntity booking);

    @Query("SELECT * FROM bookings WHERE id = :id")
    BookingEntity findById(long id);
}
