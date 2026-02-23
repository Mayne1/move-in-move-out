package com.mayneline.moveinmoveout.model;

public class ChecklistItem {
    private final String propertyId;
    private final String roomId;
    private final String itemId;
    private final String mode;
    private final String runLabel;
    private final String mediaPath;
    private final String note;
    private final long timestamp;

    public ChecklistItem(
            String propertyId,
            String roomId,
            String itemId,
            String mode,
            String runLabel,
            String mediaPath,
            String note,
            long timestamp
    ) {
        this.propertyId = propertyId;
        this.roomId = roomId;
        this.itemId = itemId;
        this.mode = mode;
        this.runLabel = runLabel;
        this.mediaPath = mediaPath;
        this.note = note;
        this.timestamp = timestamp;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getMode() {
        return mode;
    }

    public String getRunLabel() {
        return runLabel;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public String getNote() {
        return note;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
