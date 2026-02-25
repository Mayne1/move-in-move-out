package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.engine.ComparisonService;
import com.mayneline.moveinmoveout.model.ComparisonRow;
import com.mayneline.moveinmoveout.report.PdfReportExporter;
import com.mayneline.moveinmoveout.report.ReportComparisonItem;
import com.mayneline.moveinmoveout.ui.ComparisonReportAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComparisonReportActivity extends AppCompatActivity {
    private static final String STATUS_MEDIA_MISSING = "MEDIA_MISSING";
    private static final String STATUS_NO_CHANGE = "NO_CHANGE";
    private static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";

    private static final int FILTER_ALL = 0;
    private static final int FILTER_MISSING = 1;
    private static final int FILTER_NEEDS_REVIEW = 2;
    private static final int FILTER_NO_CHANGE = 3;

    private static final int SCOPE_WHOLE_PROPERTY = 0;
    private static final int SCOPE_SELECTED_ROOMS = 1;
    private static final int SCOPE_SELECTED_ITEMS = 2;

    private RecyclerView recyclerComparison;
    private TextView textEmpty;
    private TextView textSummary;
    private TextView textScope;
    private EditText editRoomFilter;
    private EditText editItemKeywordFilter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int activeFilter = FILTER_ALL;
    private int activeScope = SCOPE_WHOLE_PROPERTY;
    private final Set<String> selectedRooms = new LinkedHashSet<>();
    private final Set<String> selectedItems = new LinkedHashSet<>();
    private List<ComparisonRow> allRows = new ArrayList<>();
    private List<ComparisonRow> filteredRows = new ArrayList<>();

    private String propertyAddress = "";
    private String moveInInspectionTime = "";
    private String moveOutInspectionTime = "";
    private String firestorePropertyId = "";
    private int totalMoveInEntries;
    private int totalMoveOutEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_report);

        recyclerComparison = findViewById(R.id.recyclerComparison);
        textEmpty = findViewById(R.id.textEmpty);
        textSummary = findViewById(R.id.textSummary);
        textScope = findViewById(R.id.textScope);
        editRoomFilter = findViewById(R.id.editRoomFilter);
        editItemKeywordFilter = findViewById(R.id.editItemKeywordFilter);

        recyclerComparison.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.buttonRefreshComparison).setOnClickListener(v -> loadComparisonRows());
        findViewById(R.id.buttonApplyTextFilters).setOnClickListener(v -> {
            applyFilter();
            updateSummaryText();
        });
        findViewById(R.id.buttonClearTextFilters).setOnClickListener(v -> {
            editRoomFilter.setText("");
            editItemKeywordFilter.setText("");
            applyFilter();
            updateSummaryText();
        });
        findViewById(R.id.buttonSeedDemoPair).setVisibility(View.GONE);
        findViewById(R.id.buttonFilterAll).setOnClickListener(v -> setFilter(FILTER_ALL));
        findViewById(R.id.buttonFilterMissing).setOnClickListener(v -> setFilter(FILTER_MISSING));
        findViewById(R.id.buttonFilterNeedsReview).setOnClickListener(v -> setFilter(FILTER_NEEDS_REVIEW));
        findViewById(R.id.buttonFilterNoChange).setOnClickListener(v -> setFilter(FILTER_NO_CHANGE));
        findViewById(R.id.buttonFilterChangeDetected).setVisibility(View.GONE);
        findViewById(R.id.buttonSelectScope).setOnClickListener(v -> showScopeModeDialog());
        findViewById(R.id.buttonExportPdf).setOnClickListener(v -> exportAndShareReport());

        loadComparisonRows();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void loadComparisonRows() {
        firestorePropertyId = safe(getIntent().getStringExtra("firestorePropertyId"));
        if (firestorePropertyId.isEmpty()) {
            textSummary.setText(getString(R.string.comparison_open_from_properties));
            recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>(), this::openDetails, this::openRepairTimeline));
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        textSummary.setText(getString(R.string.comparison_loading));
        executor.execute(() -> {
            try {
                ComparisonService service = new ComparisonService();
                File cacheRoot = new File(getFilesDir(), "cloud_media_cache");
                ComparisonService.FirestoreComparisonData data = service.loadPropertyComparison(firestorePropertyId, cacheRoot);

                List<ComparisonRow> rows = new ArrayList<>();
                for (ComparisonService.FirestoreComparisonRow row : data.rows) {
                    rows.add(new ComparisonRow(
                            0L,
                            0L,
                            row.roomName,
                            row.itemName,
                            row.moveInPath,
                            row.moveOutPath,
                            "PHOTO",
                            "PHOTO",
                            row.moveInCapturedAt,
                            row.moveOutCapturedAt,
                            row.moveInSha256,
                            row.moveOutSha256,
                            row.moveInFileBytes,
                            row.moveOutFileBytes,
                            row.similarityScore,
                            row.status,
                            row.explanation
                    ));
                }

                runOnUiThread(() -> {
                    propertyAddress = data.propertyAddress;
                    moveInInspectionTime = data.moveInInspectionCreatedAt;
                    moveOutInspectionTime = data.moveOutInspectionCreatedAt;
                    totalMoveInEntries = data.totalMoveInEntries;
                    totalMoveOutEntries = data.totalMoveOutEntries;
                    allRows = rows;
                    applyFilter();
                    updateScopeLabel();
                    updateSummaryText();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    textSummary.setText(getString(R.string.comparison_load_failed));
                    recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>(), this::openDetails, this::openRepairTimeline));
                    textEmpty.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void openRepairTimeline(ComparisonRow row) {
        Intent intent = new Intent(this, TimelineActivity.class);
        intent.putExtra("firestorePropertyId", firestorePropertyId);
        intent.putExtra("propertyAddress", propertyAddress);
        intent.putExtra("roomName", row.getRoom());
        intent.putExtra("itemName", row.getItem());
        startActivity(intent);
    }

    private void openDetails(ComparisonRow row) {
        Intent intent = new Intent(this, ComparisonDetailsActivity.class);
        intent.putExtra("room", row.getRoom());
        intent.putExtra("item", row.getItem());
        intent.putExtra("moveInPath", row.getMoveInPath());
        intent.putExtra("moveOutPath", row.getMoveOutPath());
        intent.putExtra("status", row.getStatus());
        intent.putExtra("explanation", row.getExplanation());
        intent.putExtra("similarity", row.getSimilarityScore());
        startActivity(intent);
    }

    private void setFilter(int filter) {
        activeFilter = filter;
        applyFilter();
        updateSummaryText();
    }

    private void applyFilter() {
        List<ComparisonRow> filtered = new ArrayList<>();
        for (ComparisonRow row : allRows) {
            if (!isIncludedByScope(row)) {
                continue;
            }
            if (!matchesTextFilters(row)) {
                continue;
            }
            if (activeFilter == FILTER_MISSING && !STATUS_MEDIA_MISSING.equals(row.getStatus())) {
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
        filteredRows = filtered;
        recyclerComparison.setAdapter(new ComparisonReportAdapter(filtered, this::openDetails, this::openRepairTimeline));
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateSummaryText() {
        int missing = 0;
        int noChange = 0;
        int needsReview = 0;

        for (ComparisonRow row : filteredRows) {
            if (STATUS_MEDIA_MISSING.equals(row.getStatus())) {
                missing++;
            } else if (STATUS_NO_CHANGE.equals(row.getStatus())) {
                noChange++;
            } else {
                needsReview++;
            }
        }

        textSummary.setText(getString(
                R.string.comparison_summary_text,
                propertyAddress,
                totalMoveInEntries,
                totalMoveOutEntries,
                missing,
                noChange,
                needsReview,
                filteredRows.size()
        ));
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
                        selectedRooms.clear();
                        selectedItems.clear();
                        updateScopeLabel();
                        applyFilter();
                        updateSummaryText();
                    }
                })
                .show();
    }

    private void chooseRooms() {
        List<String> roomOptions = new ArrayList<>();
        for (ComparisonRow row : allRows) {
            if (!roomOptions.contains(row.getRoom())) {
                roomOptions.add(row.getRoom());
            }
        }

        String[] labels = roomOptions.toArray(new String[0]);
        boolean[] checked = new boolean[labels.length];
        for (int i = 0; i < labels.length; i++) {
            checked[i] = selectedRooms.contains(labels[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select rooms")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedRooms.add(labels[which]);
                    } else {
                        selectedRooms.remove(labels[which]);
                    }
                })
                .setPositiveButton("Done", (dialog, which) -> {
                    updateScopeLabel();
                    applyFilter();
                    updateSummaryText();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void chooseItems() {
        List<String> itemOptions = new ArrayList<>();
        for (ComparisonRow row : allRows) {
            String key = row.getRoom() + " - " + row.getItem();
            if (!itemOptions.contains(key)) {
                itemOptions.add(key);
            }
        }

        String[] labels = itemOptions.toArray(new String[0]);
        boolean[] checked = new boolean[labels.length];
        for (int i = 0; i < labels.length; i++) {
            checked[i] = selectedItems.contains(labels[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select items")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedItems.add(labels[which]);
                    } else {
                        selectedItems.remove(labels[which]);
                    }
                })
                .setPositiveButton("Done", (dialog, which) -> {
                    updateScopeLabel();
                    applyFilter();
                    updateSummaryText();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateScopeLabel() {
        if (activeScope == SCOPE_WHOLE_PROPERTY) {
            textScope.setText(getString(R.string.scope_whole_property));
        } else if (activeScope == SCOPE_SELECTED_ROOMS) {
            textScope.setText(getString(R.string.scope_room_count, selectedRooms.size()));
        } else {
            textScope.setText(getString(R.string.scope_item_count, selectedItems.size()));
        }
    }

    private void exportAndShareReport() {
        if (filteredRows.isEmpty()) {
            Toast.makeText(this, "No rows available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ReportComparisonItem> exportItems = new ArrayList<>();
        int missing = 0;
        int needsReview = 0;
        int noChange = 0;

        for (ComparisonRow row : filteredRows) {

            ReportComparisonItem item = new ReportComparisonItem();
            item.roomId = row.getRoomId();
            item.itemId = row.getItemId();
            item.roomName = row.getRoom();
            item.itemName = row.getItem();
            item.status = row.getStatus();
            item.similarityScore = row.getSimilarityScore();
            item.explanation = row.getExplanation();
            item.moveInPath = row.getMoveInPath();
            item.moveInCapturedAt = row.getMoveInCapturedAt();
            item.moveInSha256 = row.getMoveInSha256();
            item.moveInFileBytes = row.getMoveInFileBytes();
            item.moveOutPath = row.getMoveOutPath();
            item.moveOutCapturedAt = row.getMoveOutCapturedAt();
            item.moveOutSha256 = row.getMoveOutSha256();
            item.moveOutFileBytes = row.getMoveOutFileBytes();
            exportItems.add(item);

            if (STATUS_MEDIA_MISSING.equals(item.status)) {
                missing++;
            } else if (STATUS_NO_CHANGE.equals(item.status)) {
                noChange++;
            } else {
                needsReview++;
            }
        }

        if (exportItems.isEmpty()) {
            Toast.makeText(this, "No rows in current filter/scope", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDir = new File(getFilesDir(), "exports");
        try {
            PdfReportExporter pdfExporter = new PdfReportExporter();
            File pdfFile = pdfExporter.export(
                    this,
                    propertyAddress,
                    moveInInspectionTime,
                    moveOutInspectionTime,
                    exportItems,
                    totalMoveInEntries,
                    totalMoveOutEntries,
                    missing,
                    needsReview,
                    noChange,
                    outputDir
            );

            Toast.makeText(this, "Exported: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
            shareFile(pdfFile);
        } catch (Exception exception) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isIncludedByScope(ComparisonRow row) {
        if (activeScope == SCOPE_WHOLE_PROPERTY) {
            return true;
        }
        if (activeScope == SCOPE_SELECTED_ROOMS) {
            return selectedRooms.isEmpty() || selectedRooms.contains(row.getRoom());
        }
        String key = row.getRoom() + " - " + row.getItem();
        return selectedItems.isEmpty() || selectedItems.contains(key);
    }

    private boolean isIncludedByActiveFilter(ComparisonRow row) {
        if (activeFilter == FILTER_ALL) {
            return true;
        }
        if (activeFilter == FILTER_MISSING) {
            return STATUS_MEDIA_MISSING.equals(row.getStatus());
        }
        if (activeFilter == FILTER_NEEDS_REVIEW) {
            return STATUS_NEEDS_REVIEW.equals(row.getStatus());
        }
        return STATUS_NO_CHANGE.equals(row.getStatus());
    }

    private boolean matchesTextFilters(ComparisonRow row) {
        String roomFilter = safe(editRoomFilter.getText() == null ? "" : editRoomFilter.getText().toString()).toLowerCase();
        String itemFilter = safe(editItemKeywordFilter.getText() == null ? "" : editItemKeywordFilter.getText().toString()).toLowerCase();

        if (!TextUtils.isEmpty(roomFilter)) {
            String roomName = safe(row.getRoom()).toLowerCase();
            if (!roomName.contains(roomFilter)) {
                return false;
            }
        }

        if (!TextUtils.isEmpty(itemFilter)) {
            String itemName = safe(row.getItem()).toLowerCase();
            if (!itemName.contains(itemFilter)) {
                return false;
            }
        }

        return true;
    }

    private void shareFile(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Export file missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".file_provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share report"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
