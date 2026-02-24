package com.mayneline.moveinmoveout.model;

public class ComparisonRow {
    private final long roomId;
    private final long itemId;
    private final String room;
    private final String item;
    private final String moveInPath;
    private final String moveOutPath;
    private final String moveInMediaType;
    private final String moveOutMediaType;
    private final String moveInCapturedAt;
    private final String moveOutCapturedAt;
    private final String moveInSha256;
    private final String moveOutSha256;
    private final long moveInFileBytes;
    private final long moveOutFileBytes;
    private final String status;
    private boolean expanded;

    public ComparisonRow(String room, String item, String moveInPath, String moveOutPath, String status) {
        this(0L, 0L, room, item, moveInPath, moveOutPath, "PHOTO", "PHOTO", "", "", "", "", 0L, 0L, status);
    }

    public ComparisonRow(
            long roomId,
            long itemId,
            String room,
            String item,
            String moveInPath,
            String moveOutPath,
            String moveInMediaType,
            String moveOutMediaType,
            String moveInCapturedAt,
            String moveOutCapturedAt,
            String moveInSha256,
            String moveOutSha256,
            long moveInFileBytes,
            long moveOutFileBytes,
            String status
    ) {
        this.roomId = roomId;
        this.itemId = itemId;
        this.room = room;
        this.item = item;
        this.moveInPath = moveInPath;
        this.moveOutPath = moveOutPath;
        this.moveInMediaType = moveInMediaType;
        this.moveOutMediaType = moveOutMediaType;
        this.moveInCapturedAt = moveInCapturedAt;
        this.moveOutCapturedAt = moveOutCapturedAt;
        this.moveInSha256 = moveInSha256;
        this.moveOutSha256 = moveOutSha256;
        this.moveInFileBytes = moveInFileBytes;
        this.moveOutFileBytes = moveOutFileBytes;
        this.status = status;
        this.expanded = false;
    }

    public long getRoomId() {
        return roomId;
    }

    public long getItemId() {
        return itemId;
    }

    public String getRoom() {
        return room;
    }

    public String getItem() {
        return item;
    }

    public String getMoveInPath() {
        return moveInPath;
    }

    public String getMoveOutPath() {
        return moveOutPath;
    }

    public String getMoveInMediaType() {
        return moveInMediaType;
    }

    public String getMoveOutMediaType() {
        return moveOutMediaType;
    }

    public String getMoveInCapturedAt() {
        return moveInCapturedAt;
    }

    public String getMoveOutCapturedAt() {
        return moveOutCapturedAt;
    }

    public String getMoveInSha256() {
        return moveInSha256;
    }

    public String getMoveOutSha256() {
        return moveOutSha256;
    }

    public long getMoveInFileBytes() {
        return moveInFileBytes;
    }

    public long getMoveOutFileBytes() {
        return moveOutFileBytes;
    }

    public String getStatus() {
        return status;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void toggleExpanded() {
        expanded = !expanded;
    }
}
