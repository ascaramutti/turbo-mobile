package com.turbo.mobile.ui.notifications;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.NotificationError;
import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.NotificationRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

/**
 * ViewModel for Notifications screen (W-03).
 */
public class NotificationsViewModel {

    private final NotificationRepository notificationRepository;
    private final SessionManager sessionManager;

    public NotificationsViewModel(NotificationRepository notificationRepository, SessionManager sessionManager) {
        this.notificationRepository = notificationRepository;
        this.sessionManager = sessionManager;
    }

    public List<NotificationItem> getNotifications() {
        return notificationRepository.getNotifications(sessionManager.getUserId());
    }

    public Result<Void> markAsRead(long notificationId) {
        return notificationRepository.markAsRead(sessionManager.getUserId(), notificationId);
    }

    public Result<Void> markAllAsRead() {
        return notificationRepository.markAllAsRead(sessionManager.getUserId());
    }

    public Result<Void> delete(long notificationId) {
        return notificationRepository.delete(sessionManager.getUserId(), notificationId);
    }

    public int getErrorMessage(RepoError code) {
        if (code == NotificationError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == NotificationError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
