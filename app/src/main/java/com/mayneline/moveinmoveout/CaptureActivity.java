package com.mayneline.moveinmoveout;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.ChecklistStructureEntity;
import com.mayneline.moveinmoveout.data.InspectionRunEntity;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.engine.ChecklistEngineService;
import com.mayneline.moveinmoveout.engine.HashUtils;
import com.mayneline.moveinmoveout.engine.RunService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CaptureActivity extends AppCompatActivity {
    private TextView textCaptureContext;
    private TextView textCaptureTip;
    private TextView textCompletion;
    private EditText editNote;

    private AppDatabase db;
    private RunService runService;

    private String propertyId;
    private String runId;
    private InspectionRunEntity run;
    private List<ChecklistStructureEntity> checklist = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        db = AppDatabase.getInstance(this);
        runService = new RunService(db);

        textCaptureContext = findViewById(R.id.textCaptureContext);
        textCaptureTip = findViewById(R.id.textCaptureTip);
        textCompletion = findViewById(R.id.textCompletion);
        editNote = findViewById(R.id.editNote);

        propertyId = getIntent().getStringExtra("propertyId");
        runId = getIntent().getStringExtra("runId");

        loadRunState();

        findViewById(R.id.buttonPhoto).setOnClickListener(v -> captureAndStoreMedia("PHOTO"));
        findViewById(R.id.buttonVideo).setOnClickListener(v -> captureAndStoreMedia("VIDEO"));
        findViewById(R.id.buttonPreviousItem).setOnClickListener(v -> movePrevious());
        findViewById(R.id.buttonNextItem).setOnClickListener(v -> moveNext(false));
        findViewById(R.id.buttonFinalizeRun).setOnClickListener(v -> showFinalizeDialog());
    }

    private void loadRunState() {
        if (propertyId == null || propertyId.isEmpty() || runId == null || runId.isEmpty()) {
            textCaptureContext.setText("Missing run context.");
            disableCaptureActions();
            return;
        }

        run = db.mediaDao().getRunById(runId);
        checklist = db.mediaDao().getChecklistStructureForProperty(propertyId);
        if (run == null || checklist == null || checklist.isEmpty()) {
            textCaptureContext.setText("Run or checklist not found.");
            disableCaptureActions();
            return;
        }

        currentIndex = firstIncompleteIndex();
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        refreshUi();
    }

    private int firstIncompleteIndex() {
        for (int i = 0; i < checklist.size(); i++) {
            ChecklistStructureEntity row = checklist.get(i);
            RoomItemMedia media = db.mediaDao().getLatestMediaForRunItem(runId, row.roomId, row.itemId);
            if (media == null) {
                return i;
            }
        }
        return checklist.isEmpty() ? -1 : checklist.size() - 1;
    }

    private void refreshUi() {
        if (run == null || checklist.isEmpty()) {
            disableCaptureActions();
            return;
        }

        ChecklistStructureEntity current = checklist.get(currentIndex);
        int completion = runService.completionPercent(runId);

        textCaptureContext.setText(
                "Run: " + run.runLabel + " (" + run.mode + ")"
                        + "\nProperty: " + propertyId
                        + "\nRoom -> Item: " + ChecklistEngineService.displayRoomName(current.roomId)
                        + " -> " + ChecklistEngineService.displayItemName(current.itemId)
                        + "\nStep: " + (currentIndex + 1) + " / " + checklist.size()
        );
        textCaptureTip.setText("Capture media for this checklist item before moving on.");
        textCompletion.setText("Completion: " + completion + "%");

        boolean locked = run.finalized;
        findViewById(R.id.buttonPreviousItem).setEnabled(!locked && currentIndex > 0);
        findViewById(R.id.buttonNextItem).setEnabled(!locked && currentIndex < checklist.size() - 1);
        findViewById(R.id.buttonPhoto).setEnabled(!locked);
        findViewById(R.id.buttonVideo).setEnabled(!locked);
        findViewById(R.id.buttonFinalizeRun).setEnabled(!locked);

        if (locked) {
            textCaptureTip.setText("Run is finalized and locked.");
        }
    }

    private void captureAndStoreMedia(String captureType) {
        try {
            run = runService.requireEditableRun(runId);
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            refreshUi();
            return;
        }

        ChecklistStructureEntity current = checklist.get(currentIndex);
        long timestamp = System.currentTimeMillis();
        File mediaFile = new File(
                getOrCreateCaptureDir(),
                captureType.toLowerCase() + "_" + sanitize(run.runLabel) + "_" + sanitize(current.roomId)
                        + "_" + sanitize(current.itemId) + "_" + timestamp + ".jpg"
        );

        try {
            writePlaceholderImage(
                    mediaFile,
                    run.mode,
                    current.roomId,
                    current.itemId,
                    captureType,
                    timestamp
            );

            String mediaHash = HashUtils.sha256File(mediaFile.getAbsolutePath());
            RoomItemMedia media = new RoomItemMedia(
                    propertyId,
                    runId,
                    run.mode,
                    run.runLabel,
                    current.roomId,
                    current.itemId,
                    mediaFile.getAbsolutePath(),
                    safeText(editNote.getText().toString()),
                    timestamp,
                    mediaHash
            );
            db.mediaDao().insertMedia(media);
            editNote.setText("");
            Toast.makeText(this, captureType + " saved", Toast.LENGTH_SHORT).show();
            moveNext(true);
        } catch (IOException exception) {
            Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
        }
    }

    private void movePrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            refreshUi();
        }
    }

    private void moveNext(boolean afterCapture) {
        if (currentIndex < checklist.size() - 1) {
            currentIndex++;
            refreshUi();
        } else if (afterCapture) {
            refreshUi();
            Toast.makeText(this, "Checklist reached end. Finalize when ready.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFinalizeDialog() {
        if (run == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setHint("Enter initials");

        new AlertDialog.Builder(this)
                .setTitle("Finalize Run " + run.runLabel)
                .setMessage("Finalizing locks this run permanently.")
                .setView(input)
                .setPositiveButton("Finalize", (dialog, which) -> finalizeRun(input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finalizeRun(String initials) {
        try {
            runService.finalizeRun(runId, initials);
            run = db.mediaDao().getRunById(runId);
            refreshUi();
            Toast.makeText(this, "Run finalized", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void disableCaptureActions() {
        findViewById(R.id.buttonPreviousItem).setEnabled(false);
        findViewById(R.id.buttonNextItem).setEnabled(false);
        findViewById(R.id.buttonPhoto).setEnabled(false);
        findViewById(R.id.buttonVideo).setEnabled(false);
        findViewById(R.id.buttonFinalizeRun).setEnabled(false);
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
            String roomId,
            String itemId,
            String captureType,
            long timestamp
    ) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(960, 720, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor("PHOTO".equals(captureType) ? Color.parseColor("#DFF4FF") : Color.parseColor("#FFEFD6"));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);

        paint.setColor(Color.parseColor("#1B1B1B"));
        paint.setTextSize(42f);
        canvas.drawText("Move In Move Out", 48, 100, paint);
        paint.setTextSize(34f);
        canvas.drawText("Type: " + captureType, 48, 170, paint);
        canvas.drawText("Mode: " + mode, 48, 240, paint);
        canvas.drawText("Room: " + ChecklistEngineService.displayRoomName(roomId), 48, 310, paint);
        canvas.drawText("Item: " + ChecklistEngineService.displayItemName(itemId), 48, 380, paint);
        canvas.drawText("Timestamp: " + timestamp, 48, 450, paint);

        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        } finally {
            bitmap.recycle();
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
