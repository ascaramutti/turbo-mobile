package com.turbo.mobile.data.repository.fake;

import com.turbo.mobile.data.model.BookingStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

final class FakeBookingUpdates {

    private FakeBookingUpdates() {
    }

    static void close(FakeDataStore store, JSONObject booking, BookingStatus status, String reason) {
        long bookingId = FakeDataStore.getLong(booking, "id");
        FakeDataStore.set(booking, "status", status.name());
        FakeDataStore.set(booking, "status_reason", reason);
        FakeDataStore.set(booking, "_driver_actions", new JSONArray());
        FakeDataStore.set(booking, "_owner_actions", new JSONArray());
        FakeDataStore.set(booking, "_overlapping_pending_ids", new JSONArray());
        for (JSONObject other : store.rows(FakeDataStore.BOOKINGS)) {
            List<Long> overlapping = FakeDataStore.getLongList(other, "_overlapping_pending_ids");
            if (overlapping.remove(Long.valueOf(bookingId))) {
                FakeDataStore.set(other, "_overlapping_pending_ids", new JSONArray(overlapping));
            }
        }
    }
}
