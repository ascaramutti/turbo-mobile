package com.turbo.mobile.data.repository.fake;

import org.json.JSONObject;

import java.util.List;

final class FakeVehicles {

    private FakeVehicles() {
    }

    static String title(JSONObject vehicle) {
        return vehicle == null ? null
                : FakeDataStore.getString(vehicle, "make") + " " + FakeDataStore.getString(vehicle, "model") + " "
                + FakeDataStore.getInt(vehicle, "year");
    }

    static String thumbnail(FakeDataStore store, long vehicleId) {
        List<JSONObject> photos = store.where(FakeDataStore.VEHICLE_PHOTOS, "vehicle_id", vehicleId);
        return photos.isEmpty() ? null : FakeDataStore.getString(photos.get(0), "file_path");
    }

    static String zoneName(FakeDataStore store, JSONObject vehicle) {
        JSONObject zone = store.find(FakeDataStore.ZONES, FakeDataStore.getLong(vehicle, "zone_id"));
        return zone == null ? null : FakeDataStore.getString(zone, "name");
    }

    static String shortName(JSONObject user) {
        if (user == null) {
            return null;
        }
        String lastName = FakeDataStore.getString(user, "last_name");
        return FakeDataStore.getString(user, "first_name")
                + (lastName == null || lastName.isEmpty() ? "" : " " + lastName.charAt(0) + ".");
    }
}
