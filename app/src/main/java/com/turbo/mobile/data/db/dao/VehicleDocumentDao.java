package com.turbo.mobile.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.turbo.mobile.data.db.entity.VehicleDocumentEntity;
import com.turbo.mobile.data.model.DocumentType;

import java.util.List;

@Dao
public interface VehicleDocumentDao {

    @Insert
    long insert(VehicleDocumentEntity document);

    @Update
    void update(VehicleDocumentEntity document);

    @Query("SELECT * FROM vehicle_documents WHERE id = :id")
    VehicleDocumentEntity findById(long id);

    @Query("SELECT * FROM vehicle_documents WHERE vehicle_id = :ownerId AND document_type = :type")
    VehicleDocumentEntity find(long ownerId, DocumentType type);

    @Query("SELECT * FROM vehicle_documents WHERE vehicle_id = :ownerId ORDER BY document_type")
    List<VehicleDocumentEntity> getAll(long ownerId);
}
