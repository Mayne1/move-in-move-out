package com.mayneline.moveinmoveout.engine;

import com.mayneline.moveinmoveout.data.PropertyProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PropertyChecklistGenerator {
    private PropertyChecklistGenerator() {
    }

    public static List<String> generateRooms(PropertyProfile profile) {
        List<String> rooms = new ArrayList<>();
        rooms.add("Entry Hall");
        rooms.add("Living Room");
        rooms.add("Kitchen");
        rooms.add("Bathroom");
        rooms.add("Bedroom 1");

        for (int i = 2; i <= Math.max(1, profile.bedrooms); i++) {
            rooms.add("Bedroom " + i);
        }

        rooms.add("Hallway");
        rooms.add("Laundry");

        if (profile.hasGarage) {
            rooms.add("Garage");
        }
        if (profile.hasYard) {
            rooms.add("Yard");
        }
        return rooms;
    }

    public static List<String> generateItems(String roomName, boolean hasSprinklers) {
        if ("Kitchen".equals(roomName)) {
            return Arrays.asList(
                    "Walls", "Ceiling", "Floor", "Doors", "Windows", "Lights/Fixtures",
                    "Cabinets", "Countertops", "Sink", "Appliances"
            );
        }
        if ("Bathroom".equals(roomName)) {
            return Arrays.asList(
                    "Walls", "Ceiling", "Floor", "Doors", "Windows", "Lights/Fixtures",
                    "Toilet", "Sink/Vanity", "Tub/Shower", "Mirror/Fan"
            );
        }
        if ("Garage".equals(roomName)) {
            return Arrays.asList("Door", "Floor", "Walls", "Opener");
        }
        if ("Yard".equals(roomName)) {
            List<String> yardItems = new ArrayList<>(Arrays.asList("Lawn", "Fence/Gate"));
            if (hasSprinklers) {
                yardItems.add("Sprinklers");
            }
            return yardItems;
        }

        return Arrays.asList("Walls", "Ceiling", "Floor", "Doors", "Windows", "Lights/Fixtures");
    }
}
