package com.mayneline.moveinmoveout.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class InspectionChecklistFactory {

    private InspectionChecklistFactory() {
    }

    public static List<RoomSection> createBaselineSections() {
        List<RoomSection> sections = new ArrayList<>();
        sections.add(new RoomSection("Entry/Hall", createItems(
                "Door", "Locks", "Walls", "Floors", "Ceiling", "Lights")));
        sections.add(new RoomSection("Living Room", createItems(
                "Walls", "Floors", "Windows", "Blinds", "Outlets/Switches", "Lights")));
        sections.add(new RoomSection("Kitchen", createItems(
                "Counters", "Cabinets", "Sink/Plumbing", "Stove/Oven", "Fridge",
                "Dishwasher (if any)", "Floors", "Walls")));
        sections.add(new RoomSection("Bathroom", createItems(
                "Sink/Vanity", "Toilet", "Shower/Tub", "Tiles/Grout", "Mirror",
                "Vent/Fan", "Floors")));
        sections.add(new RoomSection("Bedroom", createItems(
                "Walls", "Floors", "Closet", "Windows/Blinds", "Lights", "Outlets/Switches")));
        sections.add(new RoomSection("Patio/Balcony", createItems(
                "Floor", "Railings", "Door", "Lights")));
        sections.add(new RoomSection("Garage", createItems(
                "Door", "Opener", "Walls", "Floor", "Lights")));
        return sections;
    }

    private static List<InspectionItem> createItems(String... labels) {
        List<InspectionItem> items = new ArrayList<>();
        for (String label : Arrays.asList(labels)) {
            items.add(new InspectionItem(label));
        }
        return items;
    }
}
