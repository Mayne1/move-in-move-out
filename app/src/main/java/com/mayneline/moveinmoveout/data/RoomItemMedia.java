package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
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
    @NonNull
    public String mediaType;
    @NonNull
    public String tag;
    public long videoTimestampMs;

    public Long propertyProfileId;
    public Long propertyRoomId;
    public Long roomItemId;

    // Legacy fields kept for backwards compatibility with pre-MVP flows.
    public String propertyId;
    public String runId;
    public String runLabel;
    public String roomId;
    public String itemId;
    public String mediaPath;

    public String note;
    public String mediaSha256;
    public String sha256;
    public long fileBytes;
    public String mimeType;
    public String capturedAtIso;
    public int pendingUpload;
    public String firestorePropertyId;
    public String firestoreInspectionId;
    public String firestoreStoragePath;
    public String firestoreDownloadUrl;
    public String uploadError;

    public RoomItemMedia() {
        this.mediaType = "PHOTO";
        this.tag = "WALKTHROUGH";
        this.videoTimestampMs = 0L;
        this.pendingUpload = 0;
    }

    @Ignore
    public RoomItemMedia(@NonNull String mode, @NonNull String room, @NonNull String item, @NonNull String filePath, long timestamp) {
        this();
        this.mode = mode;
        this.room = room;
        this.item = item;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.roomId = room;
        this.itemId = item;
        this.mediaPath = filePath;
    }

    @Ignore
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
        this(mode, roomId, itemId, mediaPath, timestamp);
        this.propertyId = propertyId;
        this.runId = runId;
        this.runLabel = runLabel;
        this.note = note;
        this.mediaSha256 = mediaSha256;
    }

    @Ignore
    public RoomItemMedia(
            @NonNull String mode,
            @NonNull String room,
            @NonNull String item,
            @NonNull String filePath,
            long timestamp,
            @NonNull String mediaType,
            @NonNull String tag,
            String note,
            long videoTimestampMs
    ) {
        this(mode, room, item, filePath, timestamp);
        this.mediaType = mediaType;
        this.tag = tag;
        this.note = note;
        this.videoTimestampMs = videoTimestampMs;
    }

    public void applyPropertyLinks(Long propertyProfileId, Long propertyRoomId, Long roomItemId) {
        this.propertyProfileId = propertyProfileId;
        this.propertyRoomId = propertyRoomId;
        this.roomItemId = roomItemId;
    }
}
