package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "room_item_media")
public class RoomItemMedia {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String propertyId;

    @NonNull
    public String runId;

    @NonNull
    public String mode;

    @NonNull
    public String runLabel;

    @NonNull
    public String roomId;

    @NonNull
    public String itemId;

    @NonNull
    public String mediaPath;

    public String note;
    public long timestamp;

    @NonNull
    public String mediaSha256;

    public RoomItemMedia(
            @NonNull String propertyId,
            @NonNull String runId,
            @NonNull String mode,
            @NonNull String runLabel,
            @NonNull String roomId,
            @NonNull String itemId,
            @NonNull String mediaPath,
            String note,
            long timestamp,
            @NonNull String mediaSha256
    ) {
        this.propertyId = propertyId;
        this.runId = runId;
        this.mode = mode;
        this.runLabel = runLabel;
        this.roomId = roomId;
        this.itemId = itemId;
        this.mediaPath = mediaPath;
        this.note = note;
        this.timestamp = timestamp;
        this.mediaSha256 = mediaSha256;
    }
}
