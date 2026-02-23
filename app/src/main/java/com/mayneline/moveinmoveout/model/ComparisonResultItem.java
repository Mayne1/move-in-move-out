package com.mayneline.moveinmoveout.model;

public class ComparisonResultItem {
    private final String roomId;
    private final String itemId;
    private final String moveInMediaPath;
    private final String moveOutMediaPath;
    private final ComparisonStatus status;

    public ComparisonResultItem(String roomId, String itemId, String moveInMediaPath, String moveOutMediaPath, ComparisonStatus status) {
        this.roomId = roomId;
        this.itemId = itemId;
        this.moveInMediaPath = moveInMediaPath;
        this.moveOutMediaPath = moveOutMediaPath;
        this.status = status;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getMoveInMediaPath() {
        return moveInMediaPath;
    }

    public String getMoveOutMediaPath() {
        return moveOutMediaPath;
    }

    public ComparisonStatus getStatus() {
        return status;
    }
}
