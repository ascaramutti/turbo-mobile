package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.turbo.mobile.data.db.entity.ZoneEntity;

import java.util.List;

@Dao
public interface ZoneDao {

    @Insert
    long insert(ZoneEntity zone);

    @Query("SELECT * FROM zones WHERE id = :id")
    ZoneEntity findById(long id);

    @Query("SELECT * FROM zones ORDER BY name")
    List<ZoneEntity> getAll();
}
