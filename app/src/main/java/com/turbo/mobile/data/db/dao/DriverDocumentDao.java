package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.DriverDocumentEntity;
import com.turbo.mobile.data.model.DocumentType;

import java.util.List;

@Dao
public interface DriverDocumentDao {

    @Insert
    long insert(DriverDocumentEntity document);

    @Update
    void update(DriverDocumentEntity document);

    @Query("SELECT * FROM driver_documents WHERE id = :id")
    DriverDocumentEntity findById(long id);

    @Query("SELECT * FROM driver_documents WHERE driver_id = :ownerId AND document_type = :type")
    DriverDocumentEntity find(long ownerId, DocumentType type);

    @Query("SELECT * FROM driver_documents WHERE driver_id = :ownerId ORDER BY document_type")
    List<DriverDocumentEntity> getAll(long ownerId);
}
