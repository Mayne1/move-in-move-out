package com.mayneline.moveinmoveout.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface PropertyDao {
    @Insert
    long insertProperty(PropertyProfile profile);

    @Query("SELECT * FROM property_profiles ORDER BY createdAt DESC LIMIT 1")
    PropertyProfile getLatestProperty();

    @Query("SELECT * FROM property_profiles WHERE id = :id LIMIT 1")
    PropertyProfile getPropertyById(long id);
}
