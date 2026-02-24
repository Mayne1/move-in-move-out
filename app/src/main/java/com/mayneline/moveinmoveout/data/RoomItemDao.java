package com.mayneline.moveinmoveout.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RoomItemDao {
    @Insert
    List<Long> insertItems(List<RoomItem> items);

    @Query("SELECT * FROM room_items WHERE propertyRoomId = :propertyRoomId ORDER BY id ASC")
    List<RoomItem> getItemsForRoom(long propertyRoomId);

    @Query("SELECT ri.* FROM room_items ri INNER JOIN property_rooms pr ON pr.id = ri.propertyRoomId WHERE pr.propertyId = :propertyId ORDER BY pr.id ASC, ri.id ASC")
    List<RoomItem> getItemsForProperty(long propertyId);
}
