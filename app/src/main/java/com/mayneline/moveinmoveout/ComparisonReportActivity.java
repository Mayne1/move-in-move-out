package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.PropertyProfile;
import com.mayneline.moveinmoveout.data.PropertyRoom;
import com.mayneline.moveinmoveout.data.RoomItem;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;
import com.mayneline.moveinmoveout.engine.HashUtils;
import com.mayneline.moveinmoveout.model.ComparisonRow;
import com.mayneline.moveinmoveout.report.EvidenceManifestWriter;
import com.mayneline.moveinmoveout.report.PdfReportExporter;
import com.mayneline.moveinmoveout.report.ReportComparisonItem;
import com.mayneline.moveinmoveout.ui.ComparisonReportAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ComparisonReportActivity extends AppCompatActivity {
    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String MODE_MOVE_OUT = "MOVE_OUT";
    private static final String STATUS_MISSING = "Media Missing";
    private static final String STATUS_NO_CHANGE = "No Change";
    private static final String STATUS_NEEDS_REVIEW = "Needs Review";

    private static final int FILTER_ALL = 0;
    private static final int FILTER_MISSING = 1;
    private static final int FILTER_NEEDS_REVIEW = 2;
    private static final int FILTER_NO_CHANGE = 3;

    private static final int SCOPE_WHOLE_PROPERTY = 0;
    private static final int SCOPE_SELECTED_ROOMS = 1;
    private static final int SCOPE_SELECTED_ITEMS = 2;

    private RecyclerView recyclerComparison;
    private android.widget.TextView textEmpty;
    private android.widget.TextView textSummary;
    private android.widget.TextView textScope;

    private AppDatabase db;
    private PropertyProfile property;

    private int activeFilter = FILTER_ALL;
    private int activeScope = SCOPE_WHOLE_PROPERTY;
    private final Set<Long> selectedRoomIds = new LinkedHashSet<>();
    private final Set<Long> selectedItemIds = new LinkedHashSet<>();
    private List<ComparisonRow> allRows = new ArrayList<>();

    private File lastExportedPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_report);

        db = AppDatabase.getInstance(this);

        recyclerComparison = findViewById(R.id.recyclerComparison);
        textEmpty = findViewById(R.id.textEmpty);
        textSummary = findViewById(R.id.textSummary);
        textScope = findViewById(R.id.textScope);

        recyclerComparison.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.buttonRefreshComparison).setOnClickListener(v -> loadComparisonRows());
        findViewById(R.id.buttonSeedDemoPair).setOnClickListener(v -> seedDemoPair());
        findViewById(R.id.buttonFilterAll).setOnClickListener(v -> setFilter(FILTER_ALL));
        findViewById(R.id.buttonFilterMissing).setOnClickListener(v -> setFilter(FILTER_MISSING));
        findViewById(R.id.buttonFilterNeedsReview).setOnClickListener(v -> setFilter(FILTER_NEEDS_REVIEW));
        findViewById(R.id.buttonFilterNoChange).setOnClickListener(v -> setFilter(FILTER_NO_CHANGE));
        findViewById(R.id.buttonSelectScope).setOnClickListener(v -> showScopeModeDialog());
        findViewById(R.id.buttonExportPdf).setOnClickListener(v -> exportAndShareReport());

        loadComparisonRows();
    }

    private void loadComparisonRows() {
        long propertyId = getIntent().getLongExtra("propertyProfileId", -1L);
        property = propertyId > 0 ? db.propertyDao().getPropertyById(propertyId) : db.propertyDao().getLatestProperty();

        if (property == null) {
            textSummary.setText("No property found.");
            recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>()));
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<PropertyRoom> rooms = db.propertyRoomDao().getRoomsForProperty(property.id);
        List<ComparisonRow> rows = new ArrayList<>();

        int missingCount = 0;
        int noChangeCount = 0;
        int needsReviewCount = 0;

        for (PropertyRoom room : rooms) {
            List<RoomItem> items = db.roomItemDao().getItemsForRoom(room.id);
            for (RoomItem item : items) {
                RoomItemMedia moveIn = resolvePrimaryMedia(property.id, room, item, MODE_MOVE_IN);
                RoomItemMedia moveOut = resolvePrimaryMedia(property.id, room, item, MODE_MOVE_OUT);

                String status = statusFor(moveIn, moveOut);
                if (STATUS_MISSING.equals(status)) {
                    missingCount++;
                } else if (STATUS_NO_CHANGE.equals(status)) {
                    noChangeCount++;
                } else {
                    needsReviewCount++;
                }

                rows.add(new ComparisonRow(
                        room.id,
                        item.id,
                        room.name,
                        item.name,
                        moveIn == null ? "" : bestPath(moveIn),
                        moveOut == null ? "" : bestPath(moveOut),
                        mediaType(moveIn),
                        mediaType(moveOut),
                        captureTime(moveIn),
                        captureTime(moveOut),
                        mediaHash(moveIn),
                        mediaHash(moveOut),
                        fileBytes(moveIn),
                        fileBytes(moveOut),
                        status
                ));
            }
        }

        int moveInCount = db.mediaDao().getAllMoveInForProperty(property.id).size();
        int moveOutCount = db.mediaDao().getAllMoveOutForProperty(property.id).size();

        textSummary.setText(
                "Property: " + property.addressLine1 + ", " + property.city + ", " + property.state + " " + property.zip
                        + "\nMove-In entries: " + moveInCount
                        + " | Move-Out entries: " + moveOutCount
                        + "\nMissing: " + missingCount + " | No Change: " + noChangeCount + " | Needs Review: " + needsReviewCount
        );

        allRows = rows;
        applyFilter();
        updateScopeLabel();
    }

    private void setFilter(int filter) {
        activeFilter = filter;
        applyFilter();
    }

    private void applyFilter() {
        List<ComparisonRow> filtered = new ArrayList<>();
        for (ComparisonRow row : allRows) {
            if (activeFilter == FILTER_MISSING && !STATUS_MISSING.equals(row.getStatus())) {
                continue;
            }
            if (activeFilter == FILTER_NEEDS_REVIEW && !STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
                continue;
            }
            if (activeFilter == FILTER_NO_CHANGE && !STATUS_NO_CHANGE.equals(row.getStatus())) {
                continue;
            }
            filtered.add(row);
        }
        recyclerComparison.setAdapter(new ComparisonReportAdapter(filtered));
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private RoomItemMedia resolvePrimaryMedia(long propertyId, PropertyRoom room, RoomItem item, String mode) {
        RoomItemMedia media = db.mediaDao().getPrimaryMediaForRoomItemMode(propertyId, item.id, mode);
        if (media != null) {
            return media;
        }
        media = db.mediaDao().getPrimaryForRoomItemTextMode(propertyId, mode, room.name, item.name);
        if (media != null) {
            return media;
        }
        return db.mediaDao().getPrimaryForRoomItemTextModeFallback(propertyId, String.valueOf(propertyId), mode, room.name, item.name);
    }

    private String statusFor(RoomItemMedia moveIn, RoomItemMedia moveOut) {
        if (moveIn == null || moveOut == null) {
            return STATUS_MISSING;
        }

        String hashIn = mediaHash(moveIn);
        String hashOut = mediaHash(moveOut);
        if (!hashIn.isEmpty() && hashIn.equals(hashOut)) {
            return STATUS_NO_CHANGE;
        }
        return STATUS_NEEDS_REVIEW;
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

    private String mediaHash(RoomItemMedia media) {
        if (media == null) {
            return "";
        }
        if (media.sha256 != null && !media.sha256.isEmpty()) {
            return media.sha256;
        }
        if (media.mediaSha256 != null && !media.mediaSha256.isEmpty()) {
            return media.mediaSha256;
        }
        String path = bestPath(media);
        return path.isEmpty() ? "" : HashUtils.sha256File(path);
    }

    private long fileBytes(RoomItemMedia media) {
        if (media == null) {
            return 0L;
        }
        if (media.fileBytes > 0) {
            return media.fileBytes;
        }
        return EvidenceFileUtil.sizeBytes(bestPath(media));
    }

    private String captureTime(RoomItemMedia media) {
        if (media == null) {
            return "";
        }
        if (media.capturedAtIso != null && !media.capturedAtIso.isEmpty()) {
            return media.capturedAtIso;
        }
        return media.timestamp > 0 ? EvidenceFileUtil.isoTimestamp(media.timestamp) : "";
    }

    private String mediaType(RoomItemMedia media) {
        if (media == null || media.mediaType == null || media.mediaType.isEmpty()) {
            return "PHOTO";
        }
        return media.mediaType.toUpperCase();
    }

    private void showScopeModeDialog() {
        String[] options = {"Whole property", "Select rooms", "Select items"};
        new AlertDialog.Builder(this)
                .setTitle("Report scope")
                .setSingleChoiceItems(options, activeScope, (dialog, which) -> {
                    activeScope = which;
                    dialog.dismiss();
                    if (activeScope == SCOPE_SELECTED_ROOMS) {
                        chooseRooms();
                    } else if (activeScope == SCOPE_SELECTED_ITEMS) {
                        chooseItems();
                    } else {
                        selectedRoomIds.clear();
                        selectedItemIds.clear();
                        updateScopeLabel();
                    }
                })
                .show();
    }

    private void chooseRooms() {
        if (property == null) {
            return;
        }
        List<PropertyRoom> rooms = db.propertyRoomDao().getRoomsForProperty(property.id);
        String[] labels = new String[rooms.size()];
        boolean[] checked = new boolean[rooms.size()];

        for (int i = 0; i < rooms.size(); i++) {
            labels[i] = rooms.get(i).name;
            checked[i] = selectedRoomIds.contains(rooms.get(i).id);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select rooms")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    long id = rooms.get(which).id;
                    if (isChecked) {
                        selectedRoomIds.add(id);
                    } else {
                        selectedRoomIds.remove(id);
                    }
                })
                .setPositiveButton("Done", (dialog, which) -> updateScopeLabel())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void chooseItems() {
        if (property == null) {
            return;
        }
        List<RoomItem> items = db.roomItemDao().getItemsForProperty(property.id);
        String[] labels = new String[items.size()];
        boolean[] checked = new boolean[items.size()];

        for (int i = 0; i < items.size(); i++) {
            labels[i] = items.get(i).name + " (#" + items.get(i).id + ")";
            checked[i] = selectedItemIds.contains(items.get(i).id);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select items")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    long id = items.get(which).id;
                    if (isChecked) {
                        selectedItemIds.add(id);
                    } else {
                        selectedItemIds.remove(id);
                    }
                })
                .setPositiveButton("Done", (dialog, which) -> updateScopeLabel())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateScopeLabel() {
        if (activeScope == SCOPE_WHOLE_PROPERTY) {
            textScope.setText("Scope: Whole property");
        } else if (activeScope == SCOPE_SELECTED_ROOMS) {
            textScope.setText("Scope: " + selectedRoomIds.size() + " room(s)");
        } else {
            textScope.setText("Scope: " + selectedItemIds.size() + " item(s)");
        }
    }

    private void exportAndShareReport() {
        if (property == null) {
            Toast.makeText(this, "No property available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ReportComparisonItem> exportItems = new ArrayList<>();
        int missing = 0;
        int needsReview = 0;
        int noChange = 0;

        for (ComparisonRow row : allRows) {
            if (!isIncludedByScope(row)) {
                continue;
            }

            ReportComparisonItem item = new ReportComparisonItem();
            item.roomId = row.getRoomId();
            item.itemId = row.getItemId();
            item.roomName = row.getRoom();
            item.itemName = row.getItem();
            item.status = row.getStatus();
            item.moveInPath = row.getMoveInPath();
            item.moveInCapturedAt = row.getMoveInCapturedAt();
            item.moveInSha256 = row.getMoveInSha256();
            item.moveInFileBytes = row.getMoveInFileBytes();
            item.moveOutPath = row.getMoveOutPath();
            item.moveOutCapturedAt = row.getMoveOutCapturedAt();
            item.moveOutSha256 = row.getMoveOutSha256();
            item.moveOutFileBytes = row.getMoveOutFileBytes();
            exportItems.add(item);

            if (STATUS_MISSING.equals(item.status)) {
                missing++;
            } else if (STATUS_NO_CHANGE.equals(item.status)) {
                noChange++;
            } else {
                needsReview++;
            }
        }

        if (exportItems.isEmpty()) {
            Toast.makeText(this, "No rows in current scope", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDir = new File(getFilesDir(), "exports");
        try {
            PdfReportExporter pdfExporter = new PdfReportExporter();
            lastExportedPdf = pdfExporter.export(this, property, exportItems, missing, needsReview, noChange, outputDir);

            EvidenceManifestWriter manifestWriter = new EvidenceManifestWriter();
            File manifestFile = manifestWriter.writeManifest(this, property, exportItems, outputDir);

            Toast.makeText(this, "Exported: " + lastExportedPdf.getName() + " and " + manifestFile.getName(), Toast.LENGTH_LONG).show();
            shareFile(lastExportedPdf);
        } catch (Exception exception) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isIncludedByScope(ComparisonRow row) {
        if (activeScope == SCOPE_WHOLE_PROPERTY) {
            return true;
        }
        if (activeScope == SCOPE_SELECTED_ROOMS) {
            return selectedRoomIds.isEmpty() || selectedRoomIds.contains(row.getRoomId());
        }
        return selectedItemIds.isEmpty() || selectedItemIds.contains(row.getItemId());
    }

    private void shareFile(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Export file missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share report"));
    }

    private void seedDemoPair() {
        if (property == null) {
            Toast.makeText(this, "No property available to seed", Toast.LENGTH_SHORT).show();
            return;
        }

        List<PropertyRoom> rooms = db.propertyRoomDao().getRoomsForProperty(property.id);
        if (rooms.isEmpty()) {
            Toast.makeText(this, "No generated rooms found", Toast.LENGTH_SHORT).show();
            return;
        }

        PropertyRoom room = rooms.get(0);
        List<RoomItem> items = db.roomItemDao().getItemsForRoom(room.id);
        if (items.isEmpty()) {
            Toast.makeText(this, "No generated items found", Toast.LENGTH_SHORT).show();
            return;
        }

        RoomItem item = items.get(0);
        long now = System.currentTimeMillis();

        File moveInFile = createDemoImageFile(property.id, MODE_MOVE_IN, room.id, item.id, now, "Move In");
        File moveOutFile = createDemoImageFile(property.id, MODE_MOVE_OUT, room.id, item.id, now + 1, "Move Out");

        if (moveInFile == null || moveOutFile == null) {
            Toast.makeText(this, "Failed to seed demo files", Toast.LENGTH_SHORT).show();
            return;
        }

        RoomItemMedia moveIn = new RoomItemMedia(MODE_MOVE_IN, room.name, item.name, moveInFile.getAbsolutePath(), now, "PHOTO", "WALKTHROUGH", null, 0L);
        moveIn.applyPropertyLinks(property.id, room.id, item.id);
        moveIn.propertyId = String.valueOf(property.id);
        moveIn.roomId = room.name;
        moveIn.itemId = item.name;
        moveIn.mediaPath = moveInFile.getAbsolutePath();
        moveIn.sha256 = HashUtils.sha256File(moveIn.mediaPath);
        moveIn.mediaSha256 = moveIn.sha256;
        moveIn.fileBytes = EvidenceFileUtil.sizeBytes(moveIn.mediaPath);
        moveIn.mimeType = "image/jpeg";
        moveIn.capturedAtIso = EvidenceFileUtil.isoTimestamp(now);

        RoomItemMedia moveOut = new RoomItemMedia(MODE_MOVE_OUT, room.name, item.name, moveOutFile.getAbsolutePath(), now + 1, "PHOTO", "WALKTHROUGH", null, 0L);
        moveOut.applyPropertyLinks(property.id, room.id, item.id);
        moveOut.propertyId = String.valueOf(property.id);
        moveOut.roomId = room.name;
        moveOut.itemId = item.name;
        moveOut.mediaPath = moveOutFile.getAbsolutePath();
        moveOut.sha256 = HashUtils.sha256File(moveOut.mediaPath);
        moveOut.mediaSha256 = moveOut.sha256;
        moveOut.fileBytes = EvidenceFileUtil.sizeBytes(moveOut.mediaPath);
        moveOut.mimeType = "image/jpeg";
        moveOut.capturedAtIso = EvidenceFileUtil.isoTimestamp(now + 1);

        db.mediaDao().insert(moveIn);
        db.mediaDao().insert(moveOut);

        Toast.makeText(this, "Seeded demo move-in/out pair", Toast.LENGTH_SHORT).show();
        loadComparisonRows();
    }

    private File createDemoImageFile(long propertyId, String mode, long roomId, long itemId, long timestamp, String label) {
        try {
            File dir = new File(getFilesDir(), "captures/" + propertyId + "/" + mode);
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }

            File outFile = new File(dir, roomId + "_" + itemId + "_demo_" + timestamp + ".jpg");

            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(960, 540, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setColor("MOVE_IN".equals(mode) ? android.graphics.Color.parseColor("#DFF4FF") : android.graphics.Color.parseColor("#FFE7D6"));
            canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
            paint.setColor(android.graphics.Color.parseColor("#202020"));
            paint.setTextSize(42f);
            canvas.drawText(label + " Demo", 40, 120, paint);
            paint.setTextSize(30f);
            canvas.drawText("Room: " + roomId + " Item: " + itemId, 40, 190, paint);
            canvas.drawText("Property: " + propertyId, 40, 250, paint);

            try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outFile)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream);
            }
            bitmap.recycle();
            return outFile;
        } catch (Exception exception) {
            return null;
        }
    }
}
