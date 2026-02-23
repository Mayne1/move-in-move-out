package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.InspectionRunEntity;
import com.mayneline.moveinmoveout.data.PropertyEntity;
import com.mayneline.moveinmoveout.engine.ChecklistEngineService;
import com.mayneline.moveinmoveout.engine.ComparisonService;
import com.mayneline.moveinmoveout.engine.ReportGenerator;
import com.mayneline.moveinmoveout.model.ComparisonResult;
import com.mayneline.moveinmoveout.model.ComparisonResultItem;
import com.mayneline.moveinmoveout.model.ComparisonRow;
import com.mayneline.moveinmoveout.ui.ComparisonReportAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ComparisonReportActivity extends AppCompatActivity {
    private RecyclerView recyclerComparison;
    private TextView textEmpty;
    private TextView textSummary;

    private AppDatabase db;
    private ComparisonService comparisonService;
    private ReportGenerator reportGenerator;

    private PropertyEntity property;
    private InspectionRunEntity moveInRun;
    private InspectionRunEntity moveOutRun;
    private ComparisonResult comparisonResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_report);

        db = AppDatabase.getInstance(this);
        comparisonService = new ComparisonService(db);
        reportGenerator = new ReportGenerator();

        recyclerComparison = findViewById(R.id.recyclerComparison);
        textEmpty = findViewById(R.id.textEmpty);
        textSummary = findViewById(R.id.textSummary);

        recyclerComparison.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.buttonGeneratePdf).setOnClickListener(v -> generatePdf());

        loadComparisonRows();
    }

    private void loadComparisonRows() {
        String propertyId = getIntent().getStringExtra("propertyId");
        property = propertyId == null ? db.mediaDao().getLatestProperty() : db.mediaDao().getPropertyById(propertyId);

        if (property == null) {
            textSummary.setText("No property found.");
            recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>()));
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        moveInRun = db.mediaDao().getLatestFinalizedRun(property.propertyId, "MOVE_IN");
        moveOutRun = db.mediaDao().getLatestFinalizedRun(property.propertyId, "MOVE_OUT");

        if (moveInRun == null || moveOutRun == null) {
            textSummary.setText("Finalize both MOVE_IN and MOVE_OUT runs to compare.");
            recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>()));
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        comparisonResult = comparisonService.compareRuns(moveInRun, moveOutRun);
        List<ComparisonRow> rows = new ArrayList<>();
        for (ComparisonResultItem item : comparisonResult.getItems()) {
            rows.add(new ComparisonRow(
                    ChecklistEngineService.displayRoomName(item.getRoomId()),
                    ChecklistEngineService.displayItemName(item.getItemId()),
                    item.getMoveInMediaPath(),
                    item.getMoveOutMediaPath(),
                    item.getStatus().name()
            ));
        }

        textSummary.setText(
                "Property: " + property.address
                        + "\nMOVE_IN " + moveInRun.runLabel + " vs MOVE_OUT " + moveOutRun.runLabel
                        + "\nMissing: " + comparisonResult.getMissingCount()
                        + " | No Change: " + comparisonResult.getNoChangeCount()
                        + " | Needs Review: " + comparisonResult.getNeedsReviewCount()
        );
        recyclerComparison.setAdapter(new ComparisonReportAdapter(rows));
        textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void generatePdf() {
        if (property == null || moveInRun == null || moveOutRun == null || comparisonResult == null) {
            Toast.makeText(this, "No finalized comparison available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = reportGenerator.generatePdf(this, property, moveInRun, moveOutRun, comparisonResult);
            Toast.makeText(this, "PDF saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception exception) {
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
