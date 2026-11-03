package com.turbo.mobile.ui.auth;

import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.util.SessionManager;

public class AuthViewModel {

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
}
