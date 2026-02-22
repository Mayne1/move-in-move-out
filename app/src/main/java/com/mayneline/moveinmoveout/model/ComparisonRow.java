package com.mayneline.moveinmoveout.model;

public class ComparisonRow {
    private final String room;
    private final String item;
    private final String moveInPath;
    private final String moveOutPath;
    private final String status;

    public ComparisonRow(String room, String item, String moveInPath, String moveOutPath, String status) {
        this.room = room;
        this.item = item;
        this.moveInPath = moveInPath;
        this.moveOutPath = moveOutPath;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
}
