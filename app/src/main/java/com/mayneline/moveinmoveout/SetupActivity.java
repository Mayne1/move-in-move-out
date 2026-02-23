package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.PropertyEntity;
import com.mayneline.moveinmoveout.engine.ChecklistEngineService;
import com.mayneline.moveinmoveout.engine.RunService;
import com.mayneline.moveinmoveout.model.InspectionMode;

import java.util.UUID;

public class SetupActivity extends AppCompatActivity {
    private EditText editAddress;
    private EditText editUnitNumber;
    private EditText editBedrooms;
    private EditText editBathrooms;
    private CheckBox checkGarage;
    private CheckBox checkBasement;
    private CheckBox checkYard;
    private TextView textSetupStatus;

    private String currentPropertyId;
    private InspectionMode preferredMode = InspectionMode.MOVE_IN;

    private AppDatabase db;
    private ChecklistEngineService checklistEngine;
    private RunService runService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        db = AppDatabase.getInstance(this);
        checklistEngine = new ChecklistEngineService();
        runService = new RunService(db);

        editAddress = findViewById(R.id.editAddress);
        editUnitNumber = findViewById(R.id.editUnitNumber);
        editBedrooms = findViewById(R.id.editBedrooms);
        editBathrooms = findViewById(R.id.editBathrooms);
        checkGarage = findViewById(R.id.checkGarage);
        checkBasement = findViewById(R.id.checkBasement);
        checkYard = findViewById(R.id.checkYard);
        textSetupStatus = findViewById(R.id.textSetupStatus);

        String preferred = getIntent().getStringExtra("mode");
        if (InspectionMode.MOVE_OUT.name().equals(preferred)) {
            preferredMode = InspectionMode.MOVE_OUT;
        }

        findViewById(R.id.buttonGenerateChecklist).setOnClickListener(v -> generatePropertyAndChecklist());
        findViewById(R.id.buttonStartMoveIn).setOnClickListener(v -> startRun(InspectionMode.MOVE_IN, false));
        findViewById(R.id.buttonStartMoveOut).setOnClickListener(v -> startRun(InspectionMode.MOVE_OUT, false));
        findViewById(R.id.buttonStartNewRun).setOnClickListener(v -> startRun(preferredMode, true));

        updateStatus("Setup ready. Preferred mode: " + preferredMode.name());
    }

    private void generatePropertyAndChecklist() {
        String address = safeText(editAddress.getText().toString());
        if (address.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String propertyId = UUID.randomUUID().toString();
        PropertyEntity property = new PropertyEntity(
                propertyId,
                address,
                safeText(editUnitNumber.getText().toString()),
                parsePositiveInt(editBedrooms.getText().toString(), 1),
                parsePositiveInt(editBathrooms.getText().toString(), 1),
                checkGarage.isChecked(),
                checkBasement.isChecked(),
                checkYard.isChecked(),
                System.currentTimeMillis()
        );

        db.mediaDao().insertProperty(property);
        db.mediaDao().deleteChecklistStructureForProperty(property.propertyId);
        db.mediaDao().insertChecklistStructure(checklistEngine.generateStructure(property));

        currentPropertyId = property.propertyId;
        updateStatus("Property created. Checklist generated: " + db.mediaDao().countChecklistStructureForProperty(property.propertyId) + " items.");
        Toast.makeText(this, "Checklist generated", Toast.LENGTH_SHORT).show();
    }

    private void startRun(InspectionMode mode, boolean forceNew) {
        String propertyId = resolvePropertyId();
        if (propertyId == null) {
            Toast.makeText(this, "Generate checklist first", Toast.LENGTH_SHORT).show();
            return;
        }

        int structureCount = db.mediaDao().countChecklistStructureForProperty(propertyId);
        if (structureCount == 0) {
            Toast.makeText(this, "Checklist not found for property", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String runId;
            String runLabel;
            if (forceNew) {
                runId = runService.startNewRun(propertyId, mode).runId;
                runLabel = runService.requireEditableRun(runId).runLabel;
            } else {
                runId = runService.getOrCreateActiveRun(propertyId, mode).runId;
                runLabel = runService.requireEditableRun(runId).runLabel;
            }

            Intent intent = new Intent(this, CaptureActivity.class);
            intent.putExtra("propertyId", propertyId);
            intent.putExtra("runId", runId);
            intent.putExtra("mode", mode.name());
            startActivity(intent);
            updateStatus("Started " + mode.name() + " run " + runLabel + ".");
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String resolvePropertyId() {
        if (currentPropertyId != null && !currentPropertyId.isEmpty()) {
            return currentPropertyId;
        }
        PropertyEntity latest = db.mediaDao().getLatestProperty();
        if (latest != null) {
            currentPropertyId = latest.propertyId;
        }
        return currentPropertyId;
    }

    private int parsePositiveInt(String raw, int fallback) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void updateStatus(String text) {
        textSetupStatus.setText(text);
    }
}
