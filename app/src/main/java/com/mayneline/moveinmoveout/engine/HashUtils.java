package com.mayneline.moveinmoveout.engine;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public final class HashUtils {
    private HashUtils() {
    }

    public static String sha256String(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes("UTF-8"));
            return bytesToHex(bytes);
        } catch (Exception exception) {
            return "";
        }
    }

    public static String sha256File(String path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            File file = new File(path);
            if (!file.exists()) {
                return "";
            }
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return bytesToHex(digest.digest());
        } catch (Exception exception) {
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
