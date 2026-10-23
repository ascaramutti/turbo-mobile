package com.turbo.mobile.data.repository.fake;

import org.json.JSONObject;

final class FakeNotifications {

    private FakeNotifications() {
    }

    static void add(FakeDataStore store, long userId, String title, String message) {
        JSONObject row = new JSONObject();
        FakeDataStore.set(row, "user_id", userId);
        FakeDataStore.set(row, "title", title);
        FakeDataStore.set(row, "message", message);
        FakeDataStore.set(row, "is_read", 0);
        FakeDataStore.set(row, "created_at", store.now());
        store.insert(FakeDataStore.NOTIFICATIONS, row);
    }
}
