package com.mayneline.moveinmoveout.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PropertyRoomDao {
    @Insert
    List<Long> insertRooms(List<PropertyRoom> rooms);

    @Query("SELECT * FROM property_rooms WHERE propertyId = :propertyId ORDER BY id ASC")
    List<PropertyRoom> getRoomsForProperty(long propertyId);
}
