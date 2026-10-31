package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    long insert(NotificationEntity notification);

    @Query("SELECT * FROM notifications WHERE id = :id")
    NotificationEntity findById(long id);

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    List<NotificationEntity> getAll(long userId);

    @Query("DELETE FROM notifications WHERE id = :id")
    void delete(long id);
}
