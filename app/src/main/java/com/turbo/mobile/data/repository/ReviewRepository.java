package com.turbo.mobile.data.repository;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewItem;

import java.util.List;

public interface ReviewRepository {

    Result<Long> submitReview(long reviewerId, long bookingId, int rating, @Nullable String comment);

    Result<RatingSummary> getRatingSummary(long userId);

    List<ReviewItem> getReviewsForBooking(long bookingId);
}
