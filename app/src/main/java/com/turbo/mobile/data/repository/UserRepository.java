package com.turbo.mobile.data.repository;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.RegistrationForm;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.data.model.UserProfile;

import java.util.List;

public interface UserRepository {

    Result<SessionUser> authenticate(String email, String password);

    Result<SessionUser> register(RegistrationForm form);

    Result<UserProfile> getProfile(long userId);

    Result<Void> updateProfile(long userId, String firstName, String lastName, String phone, @Nullable Uri newPhoto);

    List<UserListItem> getUsers(@Nullable String query, @Nullable Role role);

    Result<DeactivationResult> setActive(long adminId, long userId, boolean active);
}
