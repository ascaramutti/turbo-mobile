package com.turbo.mobile.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.turbo.mobile.util.ImageStorage;
import com.turbo.mobile.util.PasswordHasher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DatabaseSeeder extends RoomDatabase.Callback {

    private static final String[] TABLES = {
            "zones", "users", "driver_profiles", "driver_documents", "vehicles", "vehicle_documents",
            "vehicle_photos", "bookings", "booking_photos", "payments", "reviews", "notifications"
    };

    private static final Set<String> TIME_COLUMNS = new HashSet<>(Arrays.asList(
            "created_at", "uploaded_at", "reviewed_at", "available_until", "deleted_at", "start_time",
            "end_time", "started_at", "completed_at", "taken_at", "paid_at"));

    private static final Set<String> DATE_COLUMNS = new HashSet<>(Arrays.asList(
            "date_of_birth", "license_expiry", "study_permit_expiry"));

    private static final String PASSWORD_KEY = "_password";

    private final Context context;
    private final ImageStorage imageStorage;

    public DatabaseSeeder(Context context) {
        this.context = context.getApplicationContext();
        this.imageStorage = new ImageStorage(this.context);
    }

    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
        SeedTime seedTime = new SeedTime(System.currentTimeMillis(), ZoneId.systemDefault());
        for (String table : TABLES) {
            try {
                JSONArray rows = new JSONArray(readAsset("seed/" + table + ".json"));
                for (int i = 0; i < rows.length(); i++) {
                    db.insert(table, SQLiteDatabase.CONFLICT_ABORT, toValues(rows.getJSONObject(i), seedTime));
                }
            } catch (IOException | JSONException e) {
                throw new IllegalStateException("Cannot seed table " + table, e);
            }
        }
    }

    private ContentValues toValues(JSONObject row, SeedTime seedTime) throws JSONException, IOException {
        ContentValues values = new ContentValues();
        Iterator<String> keys = row.keys();
        while (keys.hasNext()) {
            String column = keys.next();
            if (column.startsWith("_")) {
                continue;
            }
            Object value = row.get(column);
            if (value == JSONObject.NULL) {
                values.putNull(column);
            } else if (TIME_COLUMNS.contains(column)) {
                values.put(column, seedTime.time((String) value));
            } else if (DATE_COLUMNS.contains(column)) {
                values.put(column, seedTime.date((String) value));
            } else if (value instanceof String && ((String) value).startsWith(ImageStorage.SEED_PHOTOS_PREFIX)) {
                values.put(column, imageStorage.saveAsset((String) value));
            } else if (value instanceof String) {
                values.put(column, (String) value);
            } else if (value instanceof Boolean) {
                values.put(column, (Boolean) value ? 1 : 0);
            } else if (value instanceof Double) {
                values.put(column, (Double) value);
            } else if (value instanceof Number) {
                values.put(column, ((Number) value).longValue());
            } else {
                throw new JSONException("Unsupported value in column " + column);
            }
        }
        if (row.has(PASSWORD_KEY)) {
            PasswordHasher.Hash hash = PasswordHasher.hash(row.getString(PASSWORD_KEY));
            values.put("password_hash", hash.hash);
            values.put("password_salt", hash.salt);
        }
        return values;
    }

    private String readAsset(String path) throws IOException {
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
