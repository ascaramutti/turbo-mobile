package com.turbo.mobile.data.repository.fake;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.DriverAction;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewError;
import com.turbo.mobile.data.model.ReviewItem;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.repository.ReviewRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.BOOKINGS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.REVIEWS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.USERS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getInt;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getStringList;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakeReviewRepository implements ReviewRepository {

    private final FakeDataStore store;

    public FakeReviewRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public Result<Long> submitReview(long reviewerId, long bookingId, int rating, @Nullable String comment) {
        RepoError forced = forced(ReviewError.NOT_COMPLETED, ReviewError.ALREADY_REVIEWED, ReviewError.INVALID_RATING,
                ReviewError.NOT_FOUND, ReviewError.NOT_ALLOWED, ReviewError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(ReviewError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        JSONObject vehicle = store.find(VEHICLES, getLong(booking, "vehicle_id"));
        String actionsColumn;
        String action;
        if (reviewerId == getLong(booking, "driver_id")) {
            actionsColumn = "_driver_actions";
            action = DriverAction.REVIEW.name();
        } else if (reviewerId == getLong(vehicle, "owner_id")) {
            actionsColumn = "_owner_actions";
            action = OwnerAction.REVIEW.name();
        } else {
            return Result.error(ReviewError.NOT_ALLOWED, "User " + reviewerId + " is not part of booking " + bookingId);
        }
        String text = comment == null ? null : comment.trim();
        JSONObject review = new JSONObject();
        set(review, "booking_id", bookingId);
        set(review, "reviewer_id", reviewerId);
        set(review, "rating", rating);
        set(review, "comment", text == null || text.isEmpty() ? null : text);
        set(review, "created_at", store.now());
        long id = store.insert(REVIEWS, review);
        List<String> actions = getStringList(booking, actionsColumn);
        actions.remove(action);
        set(booking, actionsColumn, new JSONArray(actions));
        return Result.success(id);
    }

    @Override
    public Result<RatingSummary> getRatingSummary(long userId) {
        RepoError forced = forced(ReviewError.NOT_FOUND, ReviewError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(USERS, userId) == null) {
            return Result.error(ReviewError.NOT_FOUND, "User " + userId + " not found");
        }
        return Result.success(FakeRatings.of(store, userId));
    }

    @Override
    public List<ReviewItem> getReviewsForBooking(long bookingId) {
        List<ReviewItem> result = new ArrayList<>();
        for (JSONObject review : store.where(REVIEWS, "booking_id", bookingId)) {
            JSONObject reviewer = store.find(USERS, getLong(review, "reviewer_id"));
            ReviewItem item = new ReviewItem();
            item.setId(getLong(review, "id"));
            item.setReviewerId(getLong(review, "reviewer_id"));
            item.setReviewerName(FakeVehicles.shortName(reviewer));
            item.setReviewerRole(Role.valueOf(getString(reviewer, "role")));
            item.setRating(getInt(review, "rating"));
            item.setComment(getString(review, "comment"));
            item.setCreatedAt(getLong(review, "created_at"));
            result.add(item);
        }
        result.sort((a, b) -> a.getCreatedAt() != b.getCreatedAt()
                ? Long.compare(a.getCreatedAt(), b.getCreatedAt())
                : Long.compare(a.getId(), b.getId()));
        return result;
    }

    private static RepoError forced(ReviewError first, ReviewError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
