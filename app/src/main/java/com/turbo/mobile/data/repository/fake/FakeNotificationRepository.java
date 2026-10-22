package com.turbo.mobile.data.repository.fake;

import com.turbo.mobile.data.model.NotificationError;
import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.NotificationRepository;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.NOTIFICATIONS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getBoolean;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakeNotificationRepository implements NotificationRepository {

    private final FakeDataStore store;

    public FakeNotificationRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public List<NotificationItem> getNotifications(long userId) {
        List<NotificationItem> result = new ArrayList<>();
        for (JSONObject row : store.where(NOTIFICATIONS, "user_id", userId)) {
            NotificationItem item = new NotificationItem();
            item.setId(getLong(row, "id"));
            item.setTitle(getString(row, "title"));
            item.setMessage(getString(row, "message"));
            item.setRead(getBoolean(row, "is_read"));
            item.setCreatedAt(getLong(row, "created_at"));
            result.add(item);
        }
        result.sort((a, b) -> a.getCreatedAt() != b.getCreatedAt()
                ? Long.compare(b.getCreatedAt(), a.getCreatedAt())
                : Long.compare(b.getId(), a.getId()));
        return result;
    }

    @Override
    public int getUnreadCount(long userId) {
        int count = 0;
        for (JSONObject row : store.where(NOTIFICATIONS, "user_id", userId)) {
            if (!getBoolean(row, "is_read")) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Result<Void> markAsRead(long userId, long notificationId) {
        RepoError forced = forced(NotificationError.NOT_FOUND, NotificationError.NOT_ALLOWED, NotificationError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject row = store.find(NOTIFICATIONS, notificationId);
        if (row == null) {
            return Result.error(NotificationError.NOT_FOUND, "Notification " + notificationId + " not found");
        }
        set(row, "is_read", 1);
        return Result.success(null);
    }

    @Override
    public Result<Void> markAllAsRead(long userId) {
        RepoError forced = forced(NotificationError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        for (JSONObject row : store.where(NOTIFICATIONS, "user_id", userId)) {
            set(row, "is_read", 1);
        }
        return Result.success(null);
    }

    @Override
    public Result<Void> delete(long userId, long notificationId) {
        RepoError forced = forced(NotificationError.NOT_FOUND, NotificationError.NOT_ALLOWED, NotificationError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(NOTIFICATIONS, notificationId) == null) {
            return Result.error(NotificationError.NOT_FOUND, "Notification " + notificationId + " not found");
        }
        store.delete(NOTIFICATIONS, notificationId);
        return Result.success(null);
    }

    private static RepoError forced(NotificationError first, NotificationError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
