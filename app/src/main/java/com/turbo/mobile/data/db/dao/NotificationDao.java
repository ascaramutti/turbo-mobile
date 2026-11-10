package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    long insert(NotificationEntity notification);

    @Query("SELECT * FROM notifications WHERE id = :id")
    NotificationEntity findById(long id);

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC, id DESC")
    List<NotificationEntity> getAll(long userId);

    @Update
    void update(NotificationEntity notification);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    int getUnreadCount(long userId);

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    void markAllAsRead(long userId);

    @Query("DELETE FROM notifications WHERE id = :id")
    void delete(long id);
}
