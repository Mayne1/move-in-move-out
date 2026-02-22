package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
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

    @Nullable
    public Long propertyId;

    @Nullable
    public Long checklistItemId;

    @Ignore
    public RoomItemMedia(@NonNull String mode, @NonNull String room, @NonNull String item, @NonNull String filePath, long timestamp) {
        this(mode, room, item, filePath, timestamp, null, null);
    }

    public RoomItemMedia(
            @NonNull String mode,
            @NonNull String room,
            @NonNull String item,
            @NonNull String filePath,
            long timestamp,
            @Nullable Long propertyId,
            @Nullable Long checklistItemId
    ) {
        this.mode = mode;
        this.room = room;
        this.item = item;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.propertyId = propertyId;
        this.checklistItemId = checklistItemId;
    }
}
