package com.mayneline.moveinmoveout;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.model.ComparisonRow;
import com.mayneline.moveinmoveout.ui.ComparisonReportAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComparisonReportActivity extends AppCompatActivity {
    private RecyclerView recyclerComparison;
    private TextView textEmpty;
    private TextView textSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_report);

        recyclerComparison = findViewById(R.id.recyclerComparison);
        textEmpty = findViewById(R.id.textEmpty);
        textSummary = findViewById(R.id.textSummary);

        recyclerComparison.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.buttonSeedDemo).setOnClickListener(v -> seedDemoPair());
        loadComparisonRows();
    }

    private void loadComparisonRows() {
        AppDatabase db = AppDatabase.getInstance(this);
        List<RoomItemMedia> moveInEntries = db.mediaDao().getAllMoveIn();
        List<RoomItemMedia> moveOutEntries = db.mediaDao().getAllMoveOut();

        Map<String, RoomItemMedia> latestMoveIn = toLatestByRoomItem(moveInEntries);
        Map<String, RoomItemMedia> latestMoveOut = toLatestByRoomItem(moveOutEntries);

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(latestMoveIn.keySet());
        allKeys.addAll(latestMoveOut.keySet());

        List<String> sortedKeys = new ArrayList<>(allKeys);
        Collections.sort(sortedKeys);

        List<ComparisonRow> rows = new ArrayList<>();
        for (String key : sortedKeys) {
            RoomItemMedia moveIn = latestMoveIn.get(key);
            RoomItemMedia moveOut = latestMoveOut.get(key);

            String room = moveIn != null ? moveIn.room : moveOut.room;
            String item = moveIn != null ? moveIn.item : moveOut.item;
            String moveInPath = moveIn != null ? moveIn.filePath : "";
            String moveOutPath = moveOut != null ? moveOut.filePath : "";
            String status = deriveStatus(moveInPath, moveOutPath);

            rows.add(new ComparisonRow(room, item, moveInPath, moveOutPath, status));
        }

        textSummary.setText("Move In entries: " + moveInEntries.size() + " | Move Out entries: " + moveOutEntries.size());
        recyclerComparison.setAdapter(new ComparisonReportAdapter(rows));

        if (rows.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            textEmpty.setVisibility(View.GONE);
        }
    }

    private Map<String, RoomItemMedia> toLatestByRoomItem(List<RoomItemMedia> entries) {
        Map<String, RoomItemMedia> map = new HashMap<>();
        for (RoomItemMedia entry : entries) {
            String key = key(entry.room, entry.item);
            RoomItemMedia current = map.get(key);
            if (current == null || entry.timestamp > current.timestamp) {
                map.put(key, entry);
            }
        }
        return map;
    }

    private String deriveStatus(String moveInPath, String moveOutPath) {
        if (isBlank(moveInPath) || isBlank(moveOutPath)) {
            return "Media Missing";
        }

        if (filesAreEqual(moveInPath, moveOutPath)) {
            return "No Change";
        }

        return "Needs Review";
    }

    private boolean filesAreEqual(String firstPath, String secondPath) {
        File first = new File(firstPath);
        File second = new File(secondPath);
        if (!first.exists() || !second.exists()) {
            return false;
        }

        if (first.length() != second.length()) {
            return false;
        }

        try (BufferedInputStream firstInput = new BufferedInputStream(new FileInputStream(first));
             BufferedInputStream secondInput = new BufferedInputStream(new FileInputStream(second))) {
            int firstByte;
            while ((firstByte = firstInput.read()) != -1) {
                int secondByte = secondInput.read();
                if (firstByte != secondByte) {
                    return false;
                }
            }
            return secondInput.read() == -1;
        } catch (IOException ignored) {
            return false;
        }
    }

    private String key(String room, String item) {
        return room + "||" + item;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void seedDemoPair() {
        AppDatabase db = AppDatabase.getInstance(this);
        long now = System.currentTimeMillis();
        String room = "Demo Room";
        String item = "Demo Item";
        String baseName = sanitize(room) + "_" + sanitize(item) + "_" + now;
        File captureDir = getOrCreateCaptureDir();
        File moveInFile = new File(captureDir, "seed_in_" + baseName + ".jpg");
        File moveOutFile = new File(captureDir, "seed_out_" + baseName + ".jpg");

        try {
            writeSeedImage(moveInFile, "MOVE_IN");
            writeSeedImage(moveOutFile, "MOVE_OUT");
            db.mediaDao().insert(new RoomItemMedia("MOVE_IN", room, item, moveInFile.getAbsolutePath(), now));
            db.mediaDao().insert(new RoomItemMedia("MOVE_OUT", room, item, moveOutFile.getAbsolutePath(), now + 1));
            loadComparisonRows();
            Toast.makeText(this, "Demo pair added", Toast.LENGTH_SHORT).show();
        } catch (IOException exception) {
            Toast.makeText(this, "Failed to seed demo pair", Toast.LENGTH_SHORT).show();
        }
    }

    private File getOrCreateCaptureDir() {
        File capturesDir = new File(getFilesDir(), "captures");
        if (!capturesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            capturesDir.mkdirs();
        }
        return capturesDir;
    }

    private void writeSeedImage(File outFile, String mode) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(720, 480, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor("MOVE_IN".equals(mode) ? Color.parseColor("#D7F5D7") : Color.parseColor("#FDE7D7"));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
        paint.setColor(Color.parseColor("#1B1B1B"));
        paint.setTextSize(48f);
        canvas.drawText(mode + " Demo", 40, 140, paint);
        paint.setTextSize(32f);
        canvas.drawText("Generated for report testing", 40, 220, paint);
        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        } finally {
            bitmap.recycle();
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
}
