package com.mayneline.moveinmoveout.report;

import android.content.Context;
import android.os.Build;

import com.mayneline.moveinmoveout.BuildConfig;
import com.mayneline.moveinmoveout.data.PropertyProfile;
import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EvidenceManifestWriter {
    public File writeManifest(Context context, PropertyProfile property, List<ReportComparisonItem> items, File outputDir) throws Exception {
        if (!outputDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdirs();
        }

        JSONObject root = new JSONObject();
        root.put("generatedAt", EvidenceFileUtil.isoTimestamp(System.currentTimeMillis()));
        root.put("applicationId", BuildConfig.APPLICATION_ID);
        root.put("versionName", BuildConfig.VERSION_NAME);
        root.put("versionCode", BuildConfig.VERSION_CODE);
        root.put("deviceModel", Build.MODEL);
        root.put("androidVersion", Build.VERSION.RELEASE);

        JSONObject propertyJson = new JSONObject();
        propertyJson.put("id", property.id);
        propertyJson.put("addressLine1", property.addressLine1);
        propertyJson.put("city", property.city);
        propertyJson.put("state", property.state);
        propertyJson.put("zip", property.zip);
        root.put("property", propertyJson);

        JSONArray itemArray = new JSONArray();
        for (ReportComparisonItem item : items) {
            JSONObject row = new JSONObject();
            row.put("room", item.roomName);
            row.put("item", item.itemName);
            row.put("status", item.status);

            JSONObject moveIn = new JSONObject();
            moveIn.put("path", safe(item.moveInPath));
            moveIn.put("sha256", safe(item.moveInSha256));
            moveIn.put("timestamp", safe(item.moveInCapturedAt));
            moveIn.put("fileBytes", item.moveInFileBytes);

            JSONObject moveOut = new JSONObject();
            moveOut.put("path", safe(item.moveOutPath));
            moveOut.put("sha256", safe(item.moveOutSha256));
            moveOut.put("timestamp", safe(item.moveOutCapturedAt));
            moveOut.put("fileBytes", item.moveOutFileBytes);

            row.put("moveIn", moveIn);
            row.put("moveOut", moveOut);
            itemArray.put(row);
        }
        root.put("items", itemArray);

        File manifestFile = new File(outputDir, "evidence_manifest_" + property.id + "_" + System.currentTimeMillis() + ".json");
        try (FileOutputStream outputStream = new FileOutputStream(manifestFile)) {
            outputStream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return manifestFile;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
