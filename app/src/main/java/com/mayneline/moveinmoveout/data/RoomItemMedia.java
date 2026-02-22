package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "room_item_media")
public class RoomItemMedia {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String mode;

    @NonNull
    public String room;

    @NonNull
    public String item;

    @NonNull
    public String filePath;

    public long timestamp;

    public RoomItemMedia(@NonNull String mode, @NonNull String room, @NonNull String item, @NonNull String filePath, long timestamp) {
        this.mode = mode;
        this.room = room;
        this.item = item;
        this.filePath = filePath;
        this.timestamp = timestamp;
    }
}
