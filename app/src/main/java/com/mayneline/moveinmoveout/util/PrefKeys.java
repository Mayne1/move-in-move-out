package com.mayneline.moveinmoveout.util;

public final class PrefKeys {
    private PrefKeys() {
    }

    public static String makeCheckedKey(String mode, String room, String item) {
        return "checked|" + mode + "|" + room + "|" + item;
    }

    public static String makeCountKey(String mode, String room, String item) {
        return "count|" + mode + "|" + room + "|" + item;
    }
}
