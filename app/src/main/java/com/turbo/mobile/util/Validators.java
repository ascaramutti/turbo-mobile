package com.turbo.mobile.util;

import android.util.Patterns;

public final class Validators {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int PHONE_DIGITS = 10;

    private Validators() {
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isEmail(String email) {
        return !isBlank(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isPhone(String phone) {
        return !isBlank(phone) && phone.trim().matches("[0-9 ()+-]+") && phone.replaceAll("\\D", "").length() == PHONE_DIGITS;
    }

    public static boolean isPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
