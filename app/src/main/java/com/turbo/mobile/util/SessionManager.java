package com.turbo.mobile.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.turbo.mobile.data.model.Role;

public class SessionManager {

    private static final String PREFS_NAME = "turbo_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, Role role) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role.name())
                .apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public Role getRole() {
        String role = prefs.getString(KEY_ROLE, null);
        return role == null ? null : Role.valueOf(role);
    }

    public boolean isLoggedIn() {
        return getUserId() != -1 && getRole() != null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
