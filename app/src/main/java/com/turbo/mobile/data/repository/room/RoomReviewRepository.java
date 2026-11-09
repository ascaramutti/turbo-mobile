package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.ReviewEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewError;
import com.turbo.mobile.data.model.ReviewItem;
import com.turbo.mobile.data.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;

public class RoomReviewRepository implements ReviewRepository {

    private final AppDatabase db;

    public RoomReviewRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    @Override
    public Result<Long> submitReview(long reviewerId, long bookingId, int rating, String comment) {
        try {
            BookingEntity booking = db.bookingDao().findById(bookingId);
            if (booking == null) {
                return Result.error(ReviewError.NOT_FOUND, "Booking " + bookingId + " not found");
            }
            VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
            if (reviewerId != booking.driverId && reviewerId != vehicle.ownerId) {
                return Result.error(ReviewError.NOT_ALLOWED, "User " + reviewerId + " is not part of booking " + bookingId);
            }
            if (booking.status != BookingStatus.COMPLETED) {
                return Result.error(ReviewError.NOT_COMPLETED, "You can review only completed bookings.");
            }
            if (db.reviewDao().find(bookingId, reviewerId) != null) {
                return Result.error(ReviewError.ALREADY_REVIEWED, "You already reviewed this booking.");
            }
            if (rating < 1 || rating > 5) {
                return Result.error(ReviewError.INVALID_RATING, "Choose 1 to 5 stars.");
            }

            ReviewEntity review = new ReviewEntity();
            review.bookingId = bookingId;
            review.reviewerId = reviewerId;
            review.rating = rating;
            review.comment = null;
            if (comment != null && !comment.trim().isEmpty()) {
                review.comment = comment.trim();
            }
            review.createdAt = System.currentTimeMillis();
            long id = db.reviewDao().insert(review);
            return Result.success(id);
        } catch (RuntimeException e) {
            Log.e("RoomReviewRepository", "Unexpected database error", e);
            return Result.error(ReviewError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<RatingSummary> getRatingSummary(long userId) {
        try {
            if (db.userDao().findById(userId) == null) {
                return Result.error(ReviewError.NOT_FOUND, "User " + userId + " not found");
            }
            return Result.success(db.reviewDao().getRatingSummary(userId));
        } catch (RuntimeException e) {
            Log.e("RoomReviewRepository", "Unexpected database error", e);
            return Result.error(ReviewError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public List<ReviewItem> getReviewsForBooking(long bookingId) {
        List<ReviewItem> items = new ArrayList<>();
        for (ReviewEntity review : db.reviewDao().getAll(bookingId)) {
            UserEntity reviewer = db.userDao().findById(review.reviewerId);
            ReviewItem item = new ReviewItem();
            item.setId(review.id);
            item.setReviewerId(review.reviewerId);
            item.setReviewerName(reviewer.firstName + " " + reviewer.lastName);
            item.setReviewerRole(reviewer.role);
            item.setRating(review.rating);
            item.setComment(review.comment);
            item.setCreatedAt(review.createdAt);
            items.add(item);
        }
        return items;
    }
}
