package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;
import com.mayneline.moveinmoveout.firebase.FirebaseRepository;
import com.mayneline.moveinmoveout.report.RepairAppendixPdfExporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimelineActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FirebaseRepository repository;

    private String firestorePropertyId;
    private String propertyAddress;
    private String roomName;
    private String itemName;
    private String repairInspectionId;

    private TimelineEntry moveInEntry;
    private TimelineEntry repairEntry;
    private TimelineEntry moveOutEntry;

    private TextView textTimelineMoveIn;
    private TextView textTimelineRepair;
    private TextView textTimelineMoveOut;
    private ImageView imageTimelineMoveIn;
    private ImageView imageTimelineRepair;
    private ImageView imageTimelineMoveOut;

    private ActivityResultLauncher<Intent> repairPhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        repository = new FirebaseRepository();
        firestorePropertyId = safe(getIntent().getStringExtra("firestorePropertyId"));
        propertyAddress = safe(getIntent().getStringExtra("propertyAddress"));
        roomName = safe(getIntent().getStringExtra("roomName"));
        itemName = safe(getIntent().getStringExtra("itemName"));

        TextView textTimelineProperty = findViewById(R.id.textTimelineProperty);
        TextView textTimelineTitle = findViewById(R.id.textTimelineTitle);
        textTimelineMoveIn = findViewById(R.id.textTimelineMoveIn);
        textTimelineRepair = findViewById(R.id.textTimelineRepair);
        textTimelineMoveOut = findViewById(R.id.textTimelineMoveOut);
        imageTimelineMoveIn = findViewById(R.id.imageTimelineMoveIn);
        imageTimelineRepair = findViewById(R.id.imageTimelineRepair);
        imageTimelineMoveOut = findViewById(R.id.imageTimelineMoveOut);

        textTimelineTitle.setText(getString(R.string.timeline_title_format, roomName, itemName));
        textTimelineProperty.setText(propertyAddress);

        repairPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Object data = result.getData().getExtras() == null ? null : result.getData().getExtras().get("data");
                if (data instanceof Bitmap) {
                    saveRepairEvidence((Bitmap) data);
                } else {
                    Toast.makeText(this, "Repair capture failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.buttonAddRepairEvidence).setOnClickListener(v -> ensureRepairSessionAndCapture());
        findViewById(R.id.buttonExportRepairAppendix).setOnClickListener(v -> exportRepairAppendix());

        loadTimeline();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void ensureRepairSessionAndCapture() {
        if (firestorePropertyId.isEmpty()) {
            Toast.makeText(this, "Property missing", Toast.LENGTH_SHORT).show();
            return;
        }
        if (repairInspectionId != null && !repairInspectionId.isEmpty()) {
            launchRepairCapture();
            return;
        }

        repository.getLatestInspectionForMode(firestorePropertyId, "REPAIR", new FirebaseRepository.RepoCallback<>() {
            @Override
            public void onSuccess(FirebaseRepository.InspectionSummary result) {
                if (result != null && result.inspectionId != null && !result.inspectionId.isEmpty()) {
                    repairInspectionId = result.inspectionId;
                    runOnUiThread(TimelineActivity.this::launchRepairCapture);
                    return;
                }
                repository.createInspection(firestorePropertyId, "REPAIR", new FirebaseRepository.RepoCallback<>() {
                    @Override
                    public void onSuccess(String result) {
                        repairInspectionId = result;
                        runOnUiThread(TimelineActivity.this::launchRepairCapture);
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        runOnUiThread(() -> Toast.makeText(TimelineActivity.this, "Could not create repair session", Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                runOnUiThread(() -> Toast.makeText(TimelineActivity.this, "Could not load repair session", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void launchRepairCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        repairPhotoLauncher.launch(intent);
    }

    private void saveRepairEvidence(Bitmap bitmap) {
        if (repairInspectionId == null || repairInspectionId.isEmpty()) {
            Toast.makeText(this, "Repair session not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        long ts = System.currentTimeMillis();
        File dir = new File(getFilesDir(), "captures/" + firestorePropertyId + "/REPAIR");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        File outFile = new File(dir, sanitize(roomName) + "_" + sanitize(itemName) + "_" + ts + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (Exception exception) {
            Toast.makeText(this, "Failed to save repair photo", Toast.LENGTH_SHORT).show();
            return;
        }

        String storagePath = "captures/" + firestorePropertyId + "/" + repairInspectionId + "/REPAIR/" + outFile.getName();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);
        storageRef.putFile(Uri.fromFile(outFile))
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    FirebaseRepository.MediaInput media = new FirebaseRepository.MediaInput();
                    media.roomName = roomName;
                    media.itemName = itemName;
                    media.type = "PHOTO";
                    media.storagePath = storagePath;
                    media.downloadUrl = uri.toString();
                    media.sha256 = com.mayneline.moveinmoveout.engine.HashUtils.sha256File(outFile.getAbsolutePath());
                    media.notes = "repair evidence";
                    media.tags = new ArrayList<>();
                    media.tags.add("REPAIR");

                    repository.addMediaRecord(firestorePropertyId, repairInspectionId, media, new FirebaseRepository.RepoCallback<>() {
                        @Override
                        public void onSuccess(String result) {
                            runOnUiThread(() -> {
                                Toast.makeText(TimelineActivity.this, "Repair evidence saved", Toast.LENGTH_SHORT).show();
                                loadTimeline();
                            });
                        }

                        @Override
                        public void onError(@NonNull Exception exception) {
                            runOnUiThread(() -> Toast.makeText(TimelineActivity.this, "Failed to save repair evidence", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    private void loadTimeline() {
        if (firestorePropertyId.isEmpty()) {
            return;
        }
        executor.execute(() -> {
            try {
                moveInEntry = loadLatestForMode("MOVE_IN");
                repairEntry = loadLatestForMode("REPAIR");
                moveOutEntry = loadLatestForMode("MOVE_OUT");

                runOnUiThread(this::renderTimeline);
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to load timeline", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private TimelineEntry loadLatestForMode(String mode) throws Exception {
        List<DocumentSnapshot> inspections = Tasks.await(FirebaseFirestore.getInstance()
                .collection("properties")
                .document(firestorePropertyId)
                .collection("inspections")
                .whereEqualTo("mode", mode)
                .get()).getDocuments();

        TimelineEntry best = null;
        for (DocumentSnapshot inspection : inspections) {
            List<DocumentSnapshot> mediaDocs = Tasks.await(inspection.getReference()
                    .collection("media")
                    .whereEqualTo("roomName", roomName)
                    .whereEqualTo("itemName", itemName)
                    .whereEqualTo("type", "PHOTO")
                    .get()).getDocuments();

            for (DocumentSnapshot mediaDoc : mediaDocs) {
                Timestamp ts = mediaDoc.getTimestamp("createdAt");
                long createdAt = ts == null ? 0L : ts.toDate().getTime();
                TimelineEntry entry = new TimelineEntry();
                entry.createdAt = createdAt;
                entry.timestampIso = createdAt > 0 ? EvidenceFileUtil.isoTimestamp(createdAt) : "";
                entry.note = safe(mediaDoc.getString("notes"));
                entry.path = cacheMediaFile(
                        inspection.getId(),
                        mediaDoc.getId(),
                        safe(mediaDoc.getString("storagePath")),
                        safe(mediaDoc.getString("downloadUrl"))
                );
                if (best == null || entry.createdAt > best.createdAt) {
                    best = entry;
                }
            }
        }
        return best;
    }

    private String cacheMediaFile(String inspectionId, String mediaId, String storagePath, String downloadUrl) {
        try {
            File dir = new File(getFilesDir(), "cloud_media_cache/" + firestorePropertyId + "/" + inspectionId);
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            File outFile = new File(dir, mediaId + ".jpg");
            if (outFile.exists() && outFile.length() > 0) {
                return outFile.getAbsolutePath();
            }

            if (!storagePath.isEmpty()) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);
                Tasks.await(storageRef.getFile(outFile));
                return outFile.getAbsolutePath();
            }

            if (!downloadUrl.isEmpty()) {
                HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.connect();
                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                return outFile.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void renderTimeline() {
        bindEntry(imageTimelineMoveIn, textTimelineMoveIn, moveInEntry);
        bindEntry(imageTimelineRepair, textTimelineRepair, repairEntry);
        bindEntry(imageTimelineMoveOut, textTimelineMoveOut, moveOutEntry);
    }

    private void bindEntry(ImageView imageView, TextView textView, TimelineEntry entry) {
        if (entry == null || entry.path == null || entry.path.isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            textView.setText(getString(R.string.timeline_no_evidence));
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(entry.path, options);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        String timestamp = (entry.timestampIso == null || entry.timestampIso.isEmpty()) ? "-" : entry.timestampIso;
        String noteSuffix = (entry.note == null || entry.note.isEmpty()) ? "" : getString(R.string.timeline_note_suffix, entry.note);
        textView.setText(getString(R.string.timeline_entry_text, timestamp, noteSuffix));
    }

    private void exportRepairAppendix() {
        try {
            RepairAppendixPdfExporter exporter = new RepairAppendixPdfExporter();
            RepairAppendixPdfExporter.TimelineEntry in = toPdfEntry(moveInEntry);
            RepairAppendixPdfExporter.TimelineEntry rep = toPdfEntry(repairEntry);
            RepairAppendixPdfExporter.TimelineEntry out = toPdfEntry(moveOutEntry);
            File outputDir = new File(getFilesDir(), "exports");
            File pdf = exporter.export(this, propertyAddress, roomName, itemName, in, rep, out, outputDir);
            Toast.makeText(this, "Exported " + pdf.getName(), Toast.LENGTH_SHORT).show();
            shareFile(pdf);
        } catch (Exception exception) {
            Toast.makeText(this, "Failed to export appendix", Toast.LENGTH_SHORT).show();
        }
    }

    private RepairAppendixPdfExporter.TimelineEntry toPdfEntry(TimelineEntry source) {
        if (source == null) {
            return null;
        }
        RepairAppendixPdfExporter.TimelineEntry entry = new RepairAppendixPdfExporter.TimelineEntry();
        entry.path = source.path;
        entry.timestampIso = source.timestampIso;
        entry.note = source.note;
        return entry;
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".file_provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share repair appendix"));
    }

    private String sanitize(String value) {
        return safe(value).replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class TimelineEntry {
        long createdAt;
        String path;
        String timestampIso;
        String note;
    }
}
