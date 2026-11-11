package com.turbo.mobile.ui.profile;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.util.SessionManager;

public class ProfileViewModel {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public ProfileViewModel(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    public Result<UserProfile> loadProfile() {
        return userRepository.getProfile(sessionManager.getUserId());
    }

    public Result<Void> updateProfile(String firstName, String lastName, String phone, @Nullable Uri newPhoto) {
        return userRepository.updateProfile(sessionManager.getUserId(), firstName, lastName, phone, newPhoto);
    }

    public void logout() {
        sessionManager.clear();
    }
}
