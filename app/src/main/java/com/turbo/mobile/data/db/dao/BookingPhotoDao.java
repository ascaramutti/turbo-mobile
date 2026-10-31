package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.BookingPhotoEntity;
import com.turbo.mobile.data.model.PhotoType;

import java.util.List;

@Dao
public interface BookingPhotoDao {

    @Insert
    long insert(BookingPhotoEntity photo);

    @Query("SELECT * FROM booking_photos WHERE booking_id = :bookingId AND photo_type = :type ORDER BY id")
    List<BookingPhotoEntity> getAll(long bookingId, PhotoType type);
}
