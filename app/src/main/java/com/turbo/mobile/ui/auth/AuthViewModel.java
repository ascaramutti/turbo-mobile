package com.turbo.mobile.ui.auth;

import android.util.Patterns;

import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.util.FormErrors;
import com.turbo.mobile.util.SessionManager;

import java.util.regex.Pattern;

public class AuthViewModel {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public AuthViewModel(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public Role getRole() {
        return sessionManager.getRole();
    }

    public String getInitials() {
        Result<UserProfile> result = userRepository.getProfile(sessionManager.getUserId());
        if (result instanceof Result.Success) {
            UserProfile profile = ((Result.Success<UserProfile>) result).data;
            return profile.getFirstName().substring(0, 1) + profile.getLastName().substring(0, 1);
        }
        return "";
    }

    public FormErrors validateLogin(String email, String password) {
        FormErrors errors = new FormErrors();
        if (email == null || email.trim().isEmpty()) {
            errors.add("email", "Email is required");
        } else if (!isEmailValid(email.trim())) {
            errors.add("email", "Invalid email format");
        }

        if (password == null || password.trim().isEmpty()) {
            errors.add("password", "Password is required");
        }
        return errors;
    }

    public Result<SessionUser> login(String email, String password) {
        String cleanEmail = email != null ? email.trim() : "";
        Result<SessionUser> result = userRepository.authenticate(cleanEmail, password != null ? password : "");
        if (result instanceof Result.Success) {
            SessionUser user = ((Result.Success<SessionUser>) result).data;
            sessionManager.saveSession(user.getId(), user.getRole());
        }
        return result;
    }

    private boolean isEmailValid(String email) {
        try {
            if (Patterns.EMAIL_ADDRESS != null) {
                return Patterns.EMAIL_ADDRESS.matcher(email).matches();
            }
        } catch (Throwable ignored) {
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
}