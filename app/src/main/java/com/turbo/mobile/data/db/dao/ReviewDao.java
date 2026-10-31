package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.ReviewEntity;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert
    long insert(ReviewEntity review);

    @Query("SELECT * FROM reviews WHERE booking_id = :bookingId AND reviewer_id = :reviewerId")
    ReviewEntity find(long bookingId, long reviewerId);

    @Query("SELECT * FROM reviews WHERE booking_id = :bookingId ORDER BY created_at")
    List<ReviewEntity> getAll(long bookingId);
}
