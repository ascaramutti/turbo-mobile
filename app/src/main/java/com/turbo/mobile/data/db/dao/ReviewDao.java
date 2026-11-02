package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.ReviewEntity;
import com.turbo.mobile.data.model.RatingSummary;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert
    long insert(ReviewEntity review);

    @Query("SELECT * FROM reviews WHERE booking_id = :bookingId AND reviewer_id = :reviewerId")
    ReviewEntity find(long bookingId, long reviewerId);

    @Query("SELECT * FROM reviews WHERE booking_id = :bookingId ORDER BY created_at")
    List<ReviewEntity> getAll(long bookingId);

    @Query("SELECT AVG(r.rating) AS average, COUNT(r.id) AS count FROM reviews r "
            + "JOIN bookings b ON b.id = r.booking_id JOIN vehicles v ON v.id = b.vehicle_id "
            + "WHERE r.reviewer_id != :userId AND (b.driver_id = :userId OR v.owner_id = :userId)")
    RatingSummary getRatingSummary(long userId);
}
