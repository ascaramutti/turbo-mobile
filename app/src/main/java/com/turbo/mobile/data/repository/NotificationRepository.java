package com.turbo.mobile.data.repository;

import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.data.model.Result;

import java.util.List;

public interface NotificationRepository {

    List<NotificationItem> getNotifications(long userId);

    int getUnreadCount(long userId);

    Result<Void> markAsRead(long userId, long notificationId);

    Result<Void> markAllAsRead(long userId);

    Result<Void> delete(long userId, long notificationId);
}
