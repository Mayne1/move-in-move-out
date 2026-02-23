package com.mayneline.moveinmoveout.engine;

import com.mayneline.moveinmoveout.data.ChecklistStructureEntity;
import com.mayneline.moveinmoveout.data.PropertyEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChecklistEngineService {
    public static final String ITEM_WALLS = "WALLS";
    public static final String ITEM_FLOOR = "FLOOR";
    public static final String ITEM_CEILING = "CEILING";
    public static final String ITEM_WINDOWS = "WINDOWS";
    public static final String ITEM_DOORS = "DOORS";
    public static final String ITEM_FIXTURES = "FIXTURES";
    public static final String ITEM_APPLIANCES = "APPLIANCES";
    public static final String ITEM_CUSTOM_NOTES = "CUSTOM_NOTES";

    private static final List<String> COMMON_ITEMS = Arrays.asList(
            ITEM_WALLS,
            ITEM_FLOOR,
            ITEM_CEILING,
            ITEM_WINDOWS,
            ITEM_DOORS,
            ITEM_FIXTURES,
            ITEM_CUSTOM_NOTES
    );

    public List<ChecklistStructureEntity> generateStructure(PropertyEntity property) {
        List<String> roomIds = buildRoomIds(property);
        List<ChecklistStructureEntity> rows = new ArrayList<>();
        int sortOrder = 0;

        for (String roomId : roomIds) {
            for (String itemId : COMMON_ITEMS) {
                rows.add(new ChecklistStructureEntity(property.propertyId, roomId, itemId, sortOrder++));
            }
            if ("KITCHEN".equals(roomId)) {
                rows.add(new ChecklistStructureEntity(property.propertyId, roomId, ITEM_APPLIANCES, sortOrder++));
            }
        }
        return rows;
    }

    public List<String> buildRoomIds(PropertyEntity property) {
        List<String> roomIds = new ArrayList<>();
        roomIds.add("LIVING_ROOM");
        roomIds.add("KITCHEN");

        for (int i = 1; i <= Math.max(0, property.bedrooms); i++) {
            roomIds.add("BED_" + i);
        }

        for (int i = 1; i <= Math.max(0, property.bathrooms); i++) {
            roomIds.add("BATH_" + i);
        }

        if (property.hasGarage) {
            roomIds.add("GARAGE");
        }
        if (property.hasBasement) {
            roomIds.add("BASEMENT");
        }
        if (property.hasYard) {
            roomIds.add("YARD");
        }

        return roomIds;
    }

    public static String displayRoomName(String roomId) {
        if (roomId == null) {
            return "";
        }
        if (roomId.startsWith("BED_")) {
            return "Bedroom " + roomId.substring("BED_".length());
        }
        if (roomId.startsWith("BATH_")) {
            return "Bathroom " + roomId.substring("BATH_".length());
        }
        return roomId.replace('_', ' ');
    }

    public static String displayItemName(String itemId) {
        if (itemId == null) {
            return "";
        }
        return itemId.replace('_', ' ');
    }
}
