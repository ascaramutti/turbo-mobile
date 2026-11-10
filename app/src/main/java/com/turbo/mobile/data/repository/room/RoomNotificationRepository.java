package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.model.NotificationError;
import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.List;

public class RoomNotificationRepository implements NotificationRepository {

    private final AppDatabase db;

    public RoomNotificationRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    @Override
    public List<NotificationItem> getNotifications(long userId) {
        List<NotificationItem> items = new ArrayList<>();
        for (NotificationEntity notification : db.notificationDao().getAll(userId)) {
            NotificationItem item = new NotificationItem();
            item.setId(notification.id);
            item.setTitle(notification.title);
            item.setMessage(notification.message);
            item.setRead(notification.isRead);
            item.setCreatedAt(notification.createdAt);
            items.add(item);
        }
        return items;
    }

    @Override
    public int getUnreadCount(long userId) {
        return db.notificationDao().getUnreadCount(userId);
    }

    @Override
    public Result<Void> markAsRead(long userId, long notificationId) {
        try {
            NotificationEntity notification = db.notificationDao().findById(notificationId);
            if (notification == null) {
                return Result.error(NotificationError.NOT_FOUND, "Notification " + notificationId + " not found");
            }
            if (notification.userId != userId) {
                return Result.error(NotificationError.NOT_ALLOWED, "The notification belongs to another user");
            }
            notification.isRead = true;
            db.notificationDao().update(notification);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomNotificationRepository", "Unexpected database error", e);
            return Result.error(NotificationError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> markAllAsRead(long userId) {
        try {
            db.notificationDao().markAllAsRead(userId);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomNotificationRepository", "Unexpected database error", e);
            return Result.error(NotificationError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<Void> delete(long userId, long notificationId) {
        try {
            NotificationEntity notification = db.notificationDao().findById(notificationId);
            if (notification == null) {
                return Result.error(NotificationError.NOT_FOUND, "Notification " + notificationId + " not found");
            }
            if (notification.userId != userId) {
                return Result.error(NotificationError.NOT_ALLOWED, "The notification belongs to another user");
            }
            db.notificationDao().delete(notificationId);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomNotificationRepository", "Unexpected database error", e);
            return Result.error(NotificationError.UNEXPECTED, e.getMessage());
        }
    }
}
