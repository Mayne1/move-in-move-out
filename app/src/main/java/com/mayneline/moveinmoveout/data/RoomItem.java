package com.mayneline.moveinmoveout.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "room_items", indices = {@Index("propertyRoomId")})
public class RoomItem {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long propertyRoomId;
    public String name;
    public long createdAt;

    public RoomItem(long propertyRoomId, String name, long createdAt) {
        this.propertyRoomId = propertyRoomId;
        this.name = name;
        this.createdAt = createdAt;
    }
}
