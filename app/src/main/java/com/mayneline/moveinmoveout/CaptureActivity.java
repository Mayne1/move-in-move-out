package com.mayneline.moveinmoveout;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.ChecklistStructureEntity;
import com.mayneline.moveinmoveout.data.InspectionRunEntity;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.engine.ChecklistEngineService;
import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;
import com.mayneline.moveinmoveout.engine.HashUtils;
import com.mayneline.moveinmoveout.engine.RunService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CaptureActivity extends AppCompatActivity {
    private TextView textCaptureContext;
    private TextView textCaptureTip;
    private TextView textCompletion;
    private EditText editNote;
    private View buttonPhoto;
    private View buttonVideo;
    private View buttonFinalizeRun;

    private AppDatabase db;
    private RunService runService;

    private String propertyId;
    private String runId;
    private InspectionRunEntity run;
    private List<ChecklistStructureEntity> checklist = new ArrayList<>();
    private int currentIndex = 0;

    private ActivityResultLauncher<Intent> photoLauncher;
    private ActivityResultLauncher<Intent> videoLauncher;

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
        buttonPhoto = findViewById(R.id.buttonPhoto);
        buttonVideo = findViewById(R.id.buttonVideo);
        buttonFinalizeRun = findViewById(R.id.buttonFinalizeRun);

        findViewById(R.id.buttonPreviousItem).setVisibility(View.GONE);
        findViewById(R.id.buttonNextItem).setVisibility(View.GONE);

        propertyId = getIntent().getStringExtra("propertyId");
        runId = getIntent().getStringExtra("runId");

        photoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Object data = result.getData().getExtras() == null ? null : result.getData().getExtras().get("data");
                if (data instanceof Bitmap) {
                    persistPhoto((Bitmap) data);
                } else {
                    Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        videoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                persistVideo(result.getData().getData());
            } else {
                Toast.makeText(this, "Video capture failed", Toast.LENGTH_SHORT).show();
            }
        });

        loadRunState();

        buttonPhoto.setOnClickListener(v -> launchPhoto());
        buttonVideo.setOnClickListener(v -> launchVideo());
        buttonFinalizeRun.setOnClickListener(v -> showFinalizeDialog());
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
        buttonPhoto.setEnabled(!locked);
        buttonVideo.setEnabled(!locked);
        buttonFinalizeRun.setEnabled(!locked);

        if (locked) {
            textCaptureTip.setText("Run is finalized and locked.");
        } else if (!isCurrentItemCaptured()) {
            textCaptureTip.setText("Linear flow: capture this item to proceed.");
        } else {
            textCaptureTip.setText("Item captured. Continue with the next required item.");
        }
    }

    private void launchPhoto() {
        if (!prepareEditableRun()) {
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoLauncher.launch(intent);
    }

    private void launchVideo() {
        if (!prepareEditableRun()) {
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoLauncher.launch(intent);
    }

    private void persistPhoto(Bitmap bitmap) {
        ChecklistStructureEntity current = checklist.get(currentIndex);
        long timestamp = System.currentTimeMillis();
        File mediaFile = new File(
                getOrCreateCaptureDir(),
                sanitize(propertyId) + "_" + sanitize(runId) + "_" + sanitize(current.roomId)
                        + "_" + sanitize(current.itemId) + "_photo_" + timestamp + ".jpg"
        );

        try (FileOutputStream outputStream = new FileOutputStream(mediaFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            persistMediaRow(current, mediaFile.getAbsolutePath(), timestamp, "PHOTO", "image/jpeg");
            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
            moveNext();
        } catch (Exception exception) {
            Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
        }
    }

    private void persistVideo(Uri uri) {
        ChecklistStructureEntity current = checklist.get(currentIndex);
        long timestamp = System.currentTimeMillis();
        File mediaFile = new File(
                getOrCreateCaptureDir(),
                sanitize(propertyId) + "_" + sanitize(runId) + "_" + sanitize(current.roomId)
                        + "_" + sanitize(current.itemId) + "_video_" + timestamp + ".mp4"
        );

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(mediaFile)) {
            if (inputStream == null) {
                Toast.makeText(this, "Video capture failed", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            persistMediaRow(current, mediaFile.getAbsolutePath(), timestamp, "VIDEO", "video/mp4");
            Toast.makeText(this, "Video saved", Toast.LENGTH_SHORT).show();
            moveNext();
        } catch (Exception exception) {
            Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
        }
    }

    private void persistMediaRow(ChecklistStructureEntity current, String path, long timestamp, String mediaType, String fallbackMime) {
        String mediaHash = HashUtils.sha256File(path);
        RoomItemMedia media = new RoomItemMedia(
                propertyId,
                runId,
                run.mode,
                run.runLabel,
                current.roomId,
                current.itemId,
                path,
                safeText(editNote.getText().toString()),
                timestamp,
                mediaHash
        );
        media.mediaType = mediaType;
        media.tag = "WALKTHROUGH";
        media.sha256 = mediaHash;
        media.fileBytes = EvidenceFileUtil.sizeBytes(path);
        media.mimeType = EvidenceFileUtil.detectMimeType(path, fallbackMime);
        media.capturedAtIso = EvidenceFileUtil.isoTimestamp(timestamp);
        db.mediaDao().insertMedia(media);
        editNote.setText("");
    }

    private boolean prepareEditableRun() {
        try {
            run = runService.requireEditableRun(runId);
            return true;
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            refreshUi();
            return false;
        }
    }

    private void moveNext() {
        if (currentIndex < checklist.size() - 1) {
            currentIndex++;
            refreshUi();
        } else {
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
        buttonPhoto.setEnabled(false);
        buttonVideo.setEnabled(false);
        buttonFinalizeRun.setEnabled(false);
    }

    private File getOrCreateCaptureDir() {
        File capturesDir = new File(getFilesDir(), "captures");
        if (!capturesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            capturesDir.mkdirs();
        }
        return capturesDir;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isCurrentItemCaptured() {
        if (checklist.isEmpty() || runId == null || runId.isEmpty()) {
            return false;
        }
        ChecklistStructureEntity current = checklist.get(currentIndex);
        RoomItemMedia media = db.mediaDao().getLatestMediaForRunItem(runId, current.roomId, current.itemId);
        return media != null && media.mediaPath != null && !media.mediaPath.isEmpty();
    }
}
