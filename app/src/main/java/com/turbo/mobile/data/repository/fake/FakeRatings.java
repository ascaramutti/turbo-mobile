package com.turbo.mobile.data.repository.fake;

import com.turbo.mobile.data.model.RatingSummary;

import org.json.JSONObject;

final class FakeRatings {

    private FakeRatings() {
    }

    static RatingSummary of(FakeDataStore store, long userId) {
        int sum = 0;
        int count = 0;
        for (JSONObject review : store.rows(FakeDataStore.REVIEWS)) {
            JSONObject booking = store.find(FakeDataStore.BOOKINGS, FakeDataStore.getLong(review, "booking_id"));
            JSONObject vehicle = store.find(FakeDataStore.VEHICLES, FakeDataStore.getLong(booking, "vehicle_id"));
            long driverId = FakeDataStore.getLong(booking, "driver_id");
            long ownerId = FakeDataStore.getLong(vehicle, "owner_id");
            long reviewee = FakeDataStore.getLong(review, "reviewer_id") == driverId ? ownerId : driverId;
            if (reviewee == userId) {
                sum += FakeDataStore.getInt(review, "rating");
                count++;
            }
        }
        RatingSummary summary = new RatingSummary();
        summary.setAverage(count == 0 ? null : (double) sum / count);
        summary.setCount(count);
        return summary;
    }
}
