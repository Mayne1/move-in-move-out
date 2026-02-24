package com.mayneline.moveinmoveout.engine;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class EvidenceFileUtil {
    private EvidenceFileUtil() {
    }

    public static String detectMimeType(String path, String fallback) {
        String result = java.net.URLConnection.guessContentTypeFromName(path);
        if (result == null || result.trim().isEmpty()) {
            return fallback;
        }
        return result;
    }

    public static String isoTimestamp(long timeMs) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(timeMs));
    }

    public static long sizeBytes(String path) {
        if (path == null || path.isEmpty()) {
            return 0L;
        }
        File file = new File(path);
        return file.exists() ? file.length() : 0L;
    }
}
