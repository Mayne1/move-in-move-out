package com.mayneline.moveinmoveout;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.ChecklistItemEntity;
import com.mayneline.moveinmoveout.data.RoomItemMedia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CaptureActivity extends AppCompatActivity {
    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String MODE_MOVE_OUT = "MOVE_OUT";

    private TextView textCaptureContext;
    private TextView textCaptureTip;
    private List<ChecklistItemEntity> checklistItems = new ArrayList<>();
    private int currentIndex = 0;
    private long propertyId = -1L;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        textCaptureContext = findViewById(R.id.textCaptureContext);
        textCaptureTip = findViewById(R.id.textCaptureTip);
        mode = getIntent().getStringExtra("mode");
        propertyId = getIntent().getLongExtra("propertyId", -1L);
        loadChecklistContext();
        renderContext();

        findViewById(R.id.buttonPhoto).setOnClickListener(v ->
                captureAndStoreMedia("PHOTO"));

        findViewById(R.id.buttonVideo).setOnClickListener(v ->
                captureAndStoreMedia("VIDEO"));

        findViewById(R.id.buttonPreviousItem).setOnClickListener(v -> goPrevious());
        findViewById(R.id.buttonNextItem).setOnClickListener(v -> goNext(false));
    }

    private void loadChecklistContext() {
        if (propertyId <= 0) {
            return;
        }

        checklistItems = AppDatabase.getInstance(this).mediaDao().getChecklistItemsForProperty(propertyId);
        if (checklistItems == null || checklistItems.isEmpty()) {
            checklistItems = new ArrayList<>();
            return;
        }

        long checklistItemId = getIntent().getLongExtra("checklistItemId", -1L);
        currentIndex = 0;
        if (checklistItemId > 0) {
            for (int i = 0; i < checklistItems.size(); i++) {
                if (checklistItems.get(i).id == checklistItemId) {
                    currentIndex = i;
                    break;
                }
            }
        }
    }

    private void renderContext() {
        ChecklistItemEntity current = currentItem();
        if (current != null && !isBlank(mode)) {
            textCaptureContext.setText(
                    "Mode: " + mode
                            + "\nProperty: " + propertyId
                            + "\nRoom -> Item: " + current.roomName + " -> " + current.itemName
                            + "\nProgress: " + (currentIndex + 1) + "/" + checklistItems.size()
            );
            textCaptureTip.setText("Tip: Capture a clear angle for " + current.roomName + " - " + current.itemName + ".");
            findViewById(R.id.buttonPreviousItem).setEnabled(currentIndex > 0);
            findViewById(R.id.buttonNextItem).setEnabled(currentIndex < checklistItems.size() - 1);
            return;
        }

        String room = getIntent().getStringExtra("room");
        String item = getIntent().getStringExtra("item");
        if (isBlank(mode) || isBlank(room) || isBlank(item)) {
            textCaptureContext.setText("No context provided.");
            textCaptureTip.setText("Tip: Generate a checklist from setup before capture.");
            findViewById(R.id.buttonPreviousItem).setEnabled(false);
            findViewById(R.id.buttonNextItem).setEnabled(false);
            return;
        }

        textCaptureContext.setText("Mode: " + mode + "\nRoom -> Item: " + room + " -> " + item);
        textCaptureTip.setText("Tip: Take at least one clear photo for each item.");
        findViewById(R.id.buttonPreviousItem).setEnabled(false);
        findViewById(R.id.buttonNextItem).setEnabled(false);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void captureAndStoreMedia(String captureType) {
        ChecklistItemEntity checklistItem = currentItem();
        String room = checklistItem != null ? checklistItem.roomName : getIntent().getStringExtra("room");
        String item = checklistItem != null ? checklistItem.itemName : getIntent().getStringExtra("item");
        Long checklistItemId = checklistItem != null ? checklistItem.id : null;
        Long capturePropertyId = propertyId > 0 ? propertyId : null;
        if (isBlank(mode) || isBlank(room) || isBlank(item)) {
            Toast.makeText(this, "Missing context. Cannot save media.", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        File mediaFile = new File(getOrCreateCaptureDir(),
                captureType.toLowerCase() + "_" + sanitize(mode) + "_" + sanitize(room) + "_"
                        + sanitize(item) + "_" + timestamp + ".jpg");

        try {
            writePlaceholderImage(mediaFile, mode, room, item, captureType, timestamp);
            RoomItemMedia media = new RoomItemMedia(
                    mode,
                    room,
                    item,
                    mediaFile.getAbsolutePath(),
                    timestamp,
                    capturePropertyId,
                    checklistItemId
            );
            AppDatabase.getInstance(this).mediaDao().insert(media);
            Toast.makeText(this, captureType + " saved", Toast.LENGTH_SHORT).show();
            goNext(true);
        } catch (IOException ioException) {
            Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
        }
    }

    private ChecklistItemEntity currentItem() {
        if (checklistItems == null || checklistItems.isEmpty()) {
            return null;
        }
        if (currentIndex < 0 || currentIndex >= checklistItems.size()) {
            return null;
        }
        return checklistItems.get(currentIndex);
    }

    private void goPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            renderContext();
        }
    }

    private void goNext(boolean autoAdvance) {
        if (checklistItems == null || checklistItems.isEmpty()) {
            return;
        }
        if (currentIndex < checklistItems.size() - 1) {
            currentIndex++;
            renderContext();
        } else if (autoAdvance) {
            Toast.makeText(this, "Checklist complete for " + modeLabel(mode), Toast.LENGTH_SHORT).show();
        }
    }

    private String modeLabel(String value) {
        if (MODE_MOVE_OUT.equals(value)) {
            return "Move Out";
        }
        if (MODE_MOVE_IN.equals(value)) {
            return "Move In";
        }
        return "current mode";
    }

    private File getOrCreateCaptureDir() {
        File capturesDir = new File(getFilesDir(), "captures");
        if (!capturesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            capturesDir.mkdirs();
        }
        return capturesDir;
    }

    private void writePlaceholderImage(
            File outFile,
            String mode,
            String room,
            String item,
            String captureType,
            long timestamp
    ) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(960, 720, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(captureType.equals("PHOTO") ? Color.parseColor("#DFF4FF") : Color.parseColor("#FFEFD6"));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);

        paint.setColor(Color.parseColor("#1B1B1B"));
        paint.setTextSize(42f);
        canvas.drawText("Move In Move Out", 48, 100, paint);
        paint.setTextSize(36f);
        canvas.drawText("Type: " + captureType, 48, 180, paint);
        canvas.drawText("Mode: " + mode, 48, 250, paint);
        canvas.drawText("Room: " + room, 48, 320, paint);
        canvas.drawText("Item: " + item, 48, 390, paint);
        canvas.drawText("Timestamp: " + timestamp, 48, 460, paint);

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
