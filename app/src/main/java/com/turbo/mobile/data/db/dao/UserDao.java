package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity findById(long id);

    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity findByEmail(String email);

    @Query("SELECT * FROM users WHERE role != 'ADMIN' AND (:role IS NULL OR role = :role) "
            + "AND (LOWER(first_name || ' ' || last_name) LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%') "
            + "ORDER BY first_name COLLATE NOCASE, last_name COLLATE NOCASE")
    List<UserEntity> search(String query, String role);
}
