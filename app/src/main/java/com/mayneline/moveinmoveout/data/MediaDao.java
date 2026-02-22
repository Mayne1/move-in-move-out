package com.mayneline.moveinmoveout.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MediaDao {
    @Insert
    long insert(RoomItemMedia media);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_IN' AND room = :room AND item = :item ORDER BY timestamp DESC")
    List<RoomItemMedia> getMoveInMedia(String room, String item);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_OUT' AND room = :room AND item = :item ORDER BY timestamp DESC")
    List<RoomItemMedia> getMoveOutMedia(String room, String item);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_IN' ORDER BY room, item, timestamp DESC")
    List<RoomItemMedia> getAllMoveIn();

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_OUT' ORDER BY room, item, timestamp DESC")
    List<RoomItemMedia> getAllMoveOut();
}
