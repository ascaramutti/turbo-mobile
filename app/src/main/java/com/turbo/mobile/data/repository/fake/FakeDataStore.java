package com.turbo.mobile.data.repository.fake;

import android.content.Context;

import com.turbo.mobile.data.db.SeedTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FakeDataStore {

    public static final String ZONES = "zones";
    public static final String USERS = "users";
    public static final String DRIVER_PROFILES = "driver_profiles";
    public static final String DRIVER_DOCUMENTS = "driver_documents";
    public static final String VEHICLES = "vehicles";
    public static final String VEHICLE_DOCUMENTS = "vehicle_documents";
    public static final String VEHICLE_PHOTOS = "vehicle_photos";
    public static final String BOOKINGS = "bookings";
    public static final String BOOKING_PHOTOS = "booking_photos";
    public static final String PAYMENTS = "payments";
    public static final String REVIEWS = "reviews";
    public static final String NOTIFICATIONS = "notifications";

    private static final String[] TABLES = {
            ZONES, USERS, DRIVER_PROFILES, DRIVER_DOCUMENTS, VEHICLES, VEHICLE_DOCUMENTS,
            VEHICLE_PHOTOS, BOOKINGS, BOOKING_PHOTOS, PAYMENTS, REVIEWS, NOTIFICATIONS
    };

    private static final Set<String> TIME_COLUMNS = new HashSet<>(Arrays.asList(
            "created_at", "uploaded_at", "reviewed_at", "available_until", "deleted_at", "start_time",
            "end_time", "started_at", "completed_at", "taken_at", "paid_at"));

    private static final Set<String> DATE_COLUMNS = new HashSet<>(Arrays.asList(
            "date_of_birth", "license_expiry", "study_permit_expiry"));

    private final Map<String, TreeMap<Long, JSONObject>> tables = new HashMap<>();

    public FakeDataStore(Context context) {
        SeedTime seedTime = new SeedTime(System.currentTimeMillis(), ZoneId.systemDefault());
        for (String table : TABLES) {
            TreeMap<Long, JSONObject> rows = new TreeMap<>();
            try {
                JSONArray array = new JSONArray(readAsset(context, "seed/" + table + ".json"));
                for (int i = 0; i < array.length(); i++) {
                    JSONObject row = array.getJSONObject(i);
                    resolveTimes(row, seedTime);
                    rows.put(row.getLong("id"), row);
                }
            } catch (IOException | JSONException e) {
                throw new IllegalStateException("Cannot load seed table " + table, e);
            }
            tables.put(table, rows);
        }
    }

    public List<JSONObject> rows(String table) {
        return new ArrayList<>(tables.get(table).values());
    }

    public List<JSONObject> where(String table, String column, long value) {
        List<JSONObject> result = new ArrayList<>();
        for (JSONObject row : tables.get(table).values()) {
            if (!row.isNull(column) && row.optLong(column) == value) {
                result.add(row);
            }
        }
        return result;
    }

    public JSONObject find(String table, long id) {
        return tables.get(table).get(id);
    }

    public long insert(String table, JSONObject row) {
        TreeMap<Long, JSONObject> rows = tables.get(table);
        long id = rows.isEmpty() ? 1 : rows.lastKey() + 1;
        set(row, "id", id);
        rows.put(id, row);
        return id;
    }

    public void delete(String table, long id) {
        tables.get(table).remove(id);
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public static String getString(JSONObject row, String column) {
        return row.isNull(column) ? null : row.optString(column);
    }

    public static long getLong(JSONObject row, String column) {
        return row.optLong(column);
    }

    public static Long getLongOrNull(JSONObject row, String column) {
        return row.isNull(column) ? null : row.optLong(column);
    }

    public static int getInt(JSONObject row, String column) {
        return row.optInt(column);
    }

    public static Double getDoubleOrNull(JSONObject row, String column) {
        return row.isNull(column) ? null : row.optDouble(column);
    }

    public static boolean getBoolean(JSONObject row, String column) {
        Object value = row.opt(column);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value instanceof Number && ((Number) value).intValue() != 0;
    }

    public static List<Long> getLongList(JSONObject row, String column) {
        List<Long> result = new ArrayList<>();
        JSONArray array = row.optJSONArray(column);
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                result.add(array.optLong(i));
            }
        }
        return result;
    }

    public static List<String> getStringList(JSONObject row, String column) {
        List<String> result = new ArrayList<>();
        JSONArray array = row.optJSONArray(column);
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                result.add(array.optString(i));
            }
        }
        return result;
    }

    public static void set(JSONObject row, String column, Object value) {
        try {
            row.put(column, value == null ? JSONObject.NULL : value);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot set " + column, e);
        }
    }

    private static void resolveTimes(JSONObject row, SeedTime seedTime) throws JSONException {
        Iterator<String> keys = row.keys();
        List<String> columns = new ArrayList<>();
        while (keys.hasNext()) {
            columns.add(keys.next());
        }
        for (String column : columns) {
            if (row.isNull(column)) {
                continue;
            }
            if (TIME_COLUMNS.contains(column)) {
                row.put(column, seedTime.time(row.getString(column)));
            } else if (DATE_COLUMNS.contains(column)) {
                row.put(column, seedTime.date(row.getString(column)));
            }
        }
    }

    private static String readAsset(Context context, String path) throws IOException {
        try (InputStream in = context.getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
