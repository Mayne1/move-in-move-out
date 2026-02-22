package com.mayneline.moveinmoveout.model;

import java.util.List;

public class RoomSection {
    private final String roomName;
    private final List<InspectionItem> items;

    public RoomSection(String roomName, List<InspectionItem> items) {
        this.roomName = roomName;
        this.items = items;
    }

    public String getRoomName() {
        return roomName;
    }

    public List<InspectionItem> getItems() {
        return items;
    }
}
