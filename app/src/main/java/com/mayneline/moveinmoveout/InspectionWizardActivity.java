package com.mayneline.moveinmoveout;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.PropertyRoom;
import com.mayneline.moveinmoveout.data.RoomItem;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;
import com.mayneline.moveinmoveout.engine.HashUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class InspectionWizardActivity extends AppCompatActivity {
    private static final String MEDIA_PHOTO = "PHOTO";
    private static final String MEDIA_VIDEO = "VIDEO";

    private static final String TAG_WALKTHROUGH = "WALKTHROUGH";
    private static final String TAG_DETAIL = "DETAIL";
    private static final String TAG_REPAIR = "REPAIR";

    private final List<Step> steps = new ArrayList<>();
    private int currentIndex = 0;

    private long propertyId;
    private String mode;

    private String pendingMediaType = MEDIA_PHOTO;
    private String pendingTag = TAG_WALKTHROUGH;

    private AppDatabase db;

    private TextView textWizardMeta;
    private TextView textWizardProgress;
    private TextView textCurrentRoom;
    private TextView textCurrentItem;
    private View panelAppendEvidence;
    private View buttonNext;
    private View buttonSkip;

    private ActivityResultLauncher<Intent> photoLauncher;
    private ActivityResultLauncher<Intent> videoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection_wizard);

        db = AppDatabase.getInstance(this);

        propertyId = getIntent().getLongExtra("propertyId", -1L);
        mode = getIntent().getStringExtra("mode");
        if (mode == null || mode.trim().isEmpty()) {
            mode = getString(R.string.wizard_default_mode);
        }

        textWizardMeta = findViewById(R.id.textWizardMeta);
        textWizardProgress = findViewById(R.id.textWizardProgress);
        textCurrentRoom = findViewById(R.id.textCurrentRoom);
        textCurrentItem = findViewById(R.id.textCurrentItem);
        panelAppendEvidence = findViewById(R.id.panelAppendEvidence);
        buttonNext = findViewById(R.id.buttonNext);
        buttonSkip = findViewById(R.id.buttonSkip);

        photoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Object data = result.getData().getExtras() == null ? null : result.getData().getExtras().get("data");
                if (data instanceof Bitmap) {
                    savePhoto((Bitmap) data);
                } else {
                    Toast.makeText(this, getString(R.string.toast_photo_capture_failed), Toast.LENGTH_SHORT).show();
                }
            }
        });

        videoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                saveVideo(result.getData().getData());
            }
        });

        findViewById(R.id.buttonTakePhoto).setOnClickListener(v -> {
            pendingMediaType = MEDIA_PHOTO;
            pendingTag = TAG_WALKTHROUGH;
            launchPhoto();
        });
        findViewById(R.id.buttonTakeVideo).setOnClickListener(v -> {
            pendingMediaType = MEDIA_VIDEO;
            pendingTag = TAG_WALKTHROUGH;
            launchVideo();
        });
        findViewById(R.id.buttonAddDetailPhoto).setOnClickListener(v -> {
            pendingMediaType = MEDIA_PHOTO;
            pendingTag = TAG_DETAIL;
            launchPhoto();
        });
        findViewById(R.id.buttonAddRepairPhoto).setOnClickListener(v -> {
            pendingMediaType = MEDIA_PHOTO;
            pendingTag = TAG_REPAIR;
            launchPhoto();
        });
        findViewById(R.id.buttonAddNote).setOnClickListener(v -> showAddNoteDialog());

        buttonSkip.setOnClickListener(v -> advance());
        buttonNext.setOnClickListener(v -> advance());
        findViewById(R.id.buttonGenerateReport).setOnClickListener(v -> openReport());

        loadSteps();
        refreshUi();
    }

    private void loadSteps() {
        steps.clear();
        if (propertyId <= 0) {
            return;
        }

        List<PropertyRoom> rooms = db.propertyRoomDao().getRoomsForProperty(propertyId);
        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            PropertyRoom room = rooms.get(roomIndex);
            List<RoomItem> items = db.roomItemDao().getItemsForRoom(room.id);
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                steps.add(new Step(room, items.get(itemIndex), roomIndex + 1, rooms.size(), itemIndex + 1, items.size()));
            }
        }
    }

    private void refreshUi() {
        textWizardMeta.setText(getString(R.string.wizard_meta, propertyId, mode));

        boolean complete = steps.isEmpty() || currentIndex >= steps.size();
        findViewById(R.id.buttonGenerateReport).setVisibility(complete ? View.VISIBLE : View.GONE);

        if (complete) {
            panelAppendEvidence.setVisibility(View.GONE);
            buttonNext.setEnabled(false);
            buttonSkip.setEnabled(false);
            textWizardProgress.setText(getString(R.string.wizard_complete));
            textCurrentRoom.setText(getString(R.string.wizard_all_items_processed));
            textCurrentItem.setText(getString(R.string.wizard_ready_report));
            return;
        }

        Step step = steps.get(currentIndex);
        boolean hasRequiredEvidence = hasWalkthroughEvidence(step);
        panelAppendEvidence.setVisibility(hasRequiredEvidence ? View.VISIBLE : View.GONE);
        buttonNext.setEnabled(hasRequiredEvidence);
        buttonSkip.setEnabled(hasRequiredEvidence);

        textWizardProgress.setText(getString(
                R.string.wizard_progress,
                step.roomPosition,
                step.roomTotal,
                step.itemPosition,
                step.itemTotal
        ));
        textCurrentRoom.setText(step.room.name);
        textCurrentItem.setText(step.item.name);
    }

    private boolean hasWalkthroughEvidence(Step step) {
        List<RoomItemMedia> timeline = db.mediaDao().getAllMediaForRoomItemMode(propertyId, step.item.id, mode);
        for (RoomItemMedia media : timeline) {
            if (TAG_WALKTHROUGH.equals(media.tag) && bestPath(media).length() > 0) {
                return true;
            }
        }
        return false;
    }

    private void launchPhoto() {
        if (isComplete()) {
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoLauncher.launch(intent);
    }

    private void launchVideo() {
        if (isComplete()) {
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoLauncher.launch(intent);
    }

    private void savePhoto(Bitmap bitmap) {
        Step step = currentStep();
        if (step == null) {
            return;
        }

        long ts = System.currentTimeMillis();
        File outFile = buildCaptureFile(step, ts, "jpg");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            insertMedia(step, outFile.getAbsolutePath(), ts, MEDIA_PHOTO, pendingTag, null, 0L);
            Toast.makeText(this, getString(R.string.toast_photo_saved), Toast.LENGTH_SHORT).show();
            refreshUi();
        } catch (Exception exception) {
            Toast.makeText(this, getString(R.string.toast_photo_save_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveVideo(Uri uri) {
        Step step = currentStep();
        if (step == null) {
            return;
        }

        long ts = System.currentTimeMillis();
        File outFile = buildCaptureFile(step, ts, "mp4");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) {
                Toast.makeText(this, getString(R.string.toast_video_capture_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            insertMedia(step, outFile.getAbsolutePath(), ts, MEDIA_VIDEO, pendingTag, null, 0L);
            Toast.makeText(this, getString(R.string.toast_video_saved), Toast.LENGTH_SHORT).show();
            refreshUi();
        } catch (Exception exception) {
            Toast.makeText(this, getString(R.string.toast_video_save_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddNoteDialog() {
        Step step = currentStep();
        if (step == null) {
            return;
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint(getString(R.string.wizard_note_hint));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.wizard_add_note))
                .setView(input)
                .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                    String note = input.getText() == null ? "" : input.getText().toString().trim();
                    if (note.isEmpty()) {
                        return;
                    }
                    saveNoteForCurrentStep(step, note);
                })
                .setNegativeButton(getString(android.R.string.cancel), null)
                .show();
    }

    private void saveNoteForCurrentStep(Step step, String note) {
        RoomItemMedia latest = db.mediaDao().getLatestMediaForRoomItemMode(propertyId, step.item.id, mode);
        if (latest != null) {
            latest.note = note;
            db.mediaDao().updateMedia(latest);
            Toast.makeText(this, getString(R.string.wizard_note_saved), Toast.LENGTH_SHORT).show();
            return;
        }

        long ts = System.currentTimeMillis();
        insertMedia(step, "", ts, MEDIA_PHOTO, TAG_DETAIL, note, 0L);
        Toast.makeText(this, getString(R.string.wizard_note_saved), Toast.LENGTH_SHORT).show();
        refreshUi();
    }

    private void insertMedia(
            Step step,
            String filePath,
            long ts,
            String mediaType,
            String tag,
            String note,
            long videoTimestampMs
    ) {
        RoomItemMedia media = new RoomItemMedia(mode, step.room.name, step.item.name, filePath, ts, mediaType, tag, note, videoTimestampMs);
        media.applyPropertyLinks(propertyId, step.room.id, step.item.id);
        media.propertyId = String.valueOf(propertyId);
        media.roomId = step.room.name;
        media.itemId = step.item.name;
        media.mediaPath = filePath;
        media.sha256 = HashUtils.sha256File(filePath);
        media.mediaSha256 = media.sha256;
        media.fileBytes = EvidenceFileUtil.sizeBytes(filePath);
        media.mimeType = EvidenceFileUtil.detectMimeType(filePath, MEDIA_VIDEO.equals(mediaType) ? "video/mp4" : "image/jpeg");
        media.capturedAtIso = EvidenceFileUtil.isoTimestamp(ts);
        db.mediaDao().insert(media);
    }

    private File buildCaptureFile(Step step, long timestamp, String extension) {
        File dir = new File(getFilesDir(), "captures/" + propertyId + "/" + mode);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        String fileName = step.room.id + "_" + step.item.id + "_" + timestamp + "." + extension;
        return new File(dir, fileName);
    }

    private void advance() {
        Step step = currentStep();
        if (step == null) {
            return;
        }

        if (!hasWalkthroughEvidence(step)) {
            Toast.makeText(this, getString(R.string.wizard_capture_required), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isComplete()) {
            currentIndex++;
        }
        refreshUi();
    }

    private void openReport() {
        Intent intent = new Intent(this, ComparisonReportActivity.class);
        intent.putExtra("propertyProfileId", propertyId);
        startActivity(intent);
    }

    private boolean isComplete() {
        return steps.isEmpty() || currentIndex >= steps.size();
    }

    private Step currentStep() {
        if (isComplete()) {
            return null;
        }
        return steps.get(currentIndex);
    }

    private String bestPath(RoomItemMedia media) {
        if (media == null) {
            return "";
        }
        if (media.filePath != null && !media.filePath.isEmpty()) {
            return media.filePath;
        }
        return media.mediaPath == null ? "" : media.mediaPath;
    }

    private static class Step {
        final PropertyRoom room;
        final RoomItem item;
        final int roomPosition;
        final int roomTotal;
        final int itemPosition;
        final int itemTotal;

        Step(PropertyRoom room, RoomItem item, int roomPosition, int roomTotal, int itemPosition, int itemTotal) {
            this.room = room;
            this.item = item;
            this.roomPosition = roomPosition;
            this.roomTotal = roomTotal;
            this.itemPosition = itemPosition;
            this.itemTotal = itemTotal;
        }
    }
}
