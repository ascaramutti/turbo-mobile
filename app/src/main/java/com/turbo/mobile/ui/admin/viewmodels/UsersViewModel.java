package com.turbo.mobile.ui.admin.viewmodels;

import androidx.annotation.Nullable;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserError;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

/**
 * ViewModel for Admin users management screen (W-27).
 */
public class UsersViewModel {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public UsersViewModel(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    public List<UserListItem> search(@Nullable String query, @Nullable Role role) {
        return userRepository.getUsers(query, role);
    }

    public Result<DeactivationResult> setActive(long userId, boolean active) {
        return userRepository.setActive(sessionManager.getUserId(), userId, active);
    }

    public int getErrorMessage(RepoError code) {
        if (code == UserError.CANNOT_CHANGE_ADMIN) {
            return R.string.users_error_admin;
        } else if (code == UserError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == UserError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
