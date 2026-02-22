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
import com.mayneline.moveinmoveout.data.ChecklistItemEntity;
import com.mayneline.moveinmoveout.data.PropertyEntity;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.model.ComparisonRow;
import com.mayneline.moveinmoveout.ui.ComparisonReportAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComparisonReportActivity extends AppCompatActivity {
    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String MODE_MOVE_OUT = "MOVE_OUT";

    private RecyclerView recyclerComparison;
    private TextView textEmpty;
    private TextView textSummary;
    private TextView textSeedDebug;
    private long propertyId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_report);

        recyclerComparison = findViewById(R.id.recyclerComparison);
        textEmpty = findViewById(R.id.textEmpty);
        textSummary = findViewById(R.id.textSummary);
        textSeedDebug = findViewById(R.id.textSeedDebug);

        recyclerComparison.setLayoutManager(new LinearLayoutManager(this));
        propertyId = getIntent().getLongExtra("propertyId", -1L);
        findViewById(R.id.buttonSeedDemo).setOnClickListener(v -> seedDemoPair());
        loadComparisonRows();
    }

    private void loadComparisonRows() {
        AppDatabase db = AppDatabase.getInstance(this);
        PropertyEntity property = resolveProperty(db);
        if (property == null) {
            textSummary.setText("No property checklist found.");
            recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>()));
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<ChecklistItemEntity> checklistItems = db.mediaDao().getChecklistItemsForProperty(propertyId);
        List<ComparisonRow> rows = new ArrayList<>();
        int moveInCount = 0;
        int moveOutCount = 0;

        for (ChecklistItemEntity checklistItem : checklistItems) {
            RoomItemMedia moveIn = db.mediaDao().getLatestMediaForChecklistItem(MODE_MOVE_IN, propertyId, checklistItem.id);
            RoomItemMedia moveOut = db.mediaDao().getLatestMediaForChecklistItem(MODE_MOVE_OUT, propertyId, checklistItem.id);

            if (moveIn != null) {
                moveInCount++;
            }
            if (moveOut != null) {
                moveOutCount++;
            }

            String moveInPath = moveIn != null ? moveIn.filePath : "";
            String moveOutPath = moveOut != null ? moveOut.filePath : "";
            String status = deriveStatus(moveInPath, moveOutPath);

            rows.add(new ComparisonRow(
                    checklistItem.roomName,
                    checklistItem.itemName,
                    moveInPath,
                    moveOutPath,
                    status
            ));
        }

        textSummary.setText(
                "Property #" + property.id
                        + " | " + property.addressLine
                        + "\nMove In captured: " + moveInCount + " | Move Out captured: " + moveOutCount
        );
        recyclerComparison.setAdapter(new ComparisonReportAdapter(rows));
        textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private PropertyEntity resolveProperty(AppDatabase db) {
        if (propertyId > 0) {
            PropertyEntity latest = db.mediaDao().getLatestProperty();
            if (latest != null && latest.id == propertyId) {
                return latest;
            }

            List<ChecklistItemEntity> items = db.mediaDao().getChecklistItemsForProperty(propertyId);
            if (items != null && !items.isEmpty()) {
                PropertyEntity fake = new PropertyEntity("Property " + propertyId, 0, 0, 0L);
                fake.id = propertyId;
                return fake;
            }
        }

        PropertyEntity latest = db.mediaDao().getLatestProperty();
        if (latest != null) {
            propertyId = latest.id;
        }
        return latest;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void seedDemoPair() {
        AppDatabase db = AppDatabase.getInstance(this);
        long now = System.currentTimeMillis();
        String room = normalizeValue(" Demo Room ");
        String item = normalizeValue(" Demo Item ");

        PropertyEntity property = new PropertyEntity("Demo Address", 1, 1, now);
        long seededPropertyId = db.mediaDao().insertProperty(property);
        ChecklistItemEntity checklistItem = new ChecklistItemEntity(seededPropertyId, room, item, 0);
        db.mediaDao().insertChecklistItems(java.util.Collections.singletonList(checklistItem));
        List<ChecklistItemEntity> seededItems = db.mediaDao().getChecklistItemsForProperty(seededPropertyId);
        if (seededItems == null || seededItems.isEmpty()) {
            Toast.makeText(this, "Failed to seed checklist", Toast.LENGTH_SHORT).show();
            return;
        }

        ChecklistItemEntity insertedItem = seededItems.get(0);
        String baseName = sanitize(room) + "_" + sanitize(item) + "_" + now;
        File captureDir = getOrCreateCaptureDir();
        File moveInFile = new File(captureDir, "seed_in_" + baseName + ".jpg");
        File moveOutFile = new File(captureDir, "seed_out_" + baseName + ".jpg");

        try {
            writeSeedImage(moveInFile, MODE_MOVE_IN);
            writeSeedImage(moveOutFile, MODE_MOVE_OUT);
            db.mediaDao().insert(new RoomItemMedia(MODE_MOVE_IN, room, item, moveInFile.getAbsolutePath(), now, seededPropertyId, insertedItem.id));
            db.mediaDao().insert(new RoomItemMedia(MODE_MOVE_OUT, room, item, moveOutFile.getAbsolutePath(), now + 1, seededPropertyId, insertedItem.id));

            List<RoomItemMedia> moveInForKey = db.mediaDao().getMediaForChecklistItem(MODE_MOVE_IN, seededPropertyId, insertedItem.id);
            List<RoomItemMedia> moveOutForKey = db.mediaDao().getMediaForChecklistItem(MODE_MOVE_OUT, seededPropertyId, insertedItem.id);
            boolean moveInInserted = moveInForKey != null && !moveInForKey.isEmpty();
            boolean moveOutInserted = moveOutForKey != null && !moveOutForKey.isEmpty();
            int moveInTotal = db.mediaDao().getAllMoveInForProperty(seededPropertyId).size();
            int moveOutTotal = db.mediaDao().getAllMoveOutForProperty(seededPropertyId).size();

            String debugText = "Seed result:\n"
                    + "MOVE_IN inserted: " + (moveInInserted ? "YES" : "NO") + "\n"
                    + "MOVE_OUT inserted: " + (moveOutInserted ? "YES" : "NO") + "\n"
                    + "room: " + room + "\n"
                    + "item: " + item + "\n"
                    + "MOVE_IN path: " + moveInFile.getAbsolutePath() + "\n"
                    + "MOVE_OUT path: " + moveOutFile.getAbsolutePath() + "\n"
                    + "total counts after insert: Move In = " + moveInTotal + ", Move Out = " + moveOutTotal;
            textSeedDebug.setText(debugText);
            textSeedDebug.setVisibility(View.VISIBLE);

            propertyId = seededPropertyId;
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
        paint.setColor(MODE_MOVE_IN.equals(mode) ? Color.parseColor("#D7F5D7") : Color.parseColor("#FDE7D7"));
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

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.US);
    }
}
