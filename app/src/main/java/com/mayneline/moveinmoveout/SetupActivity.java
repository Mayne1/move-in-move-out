package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.ChecklistItemEntity;
import com.mayneline.moveinmoveout.data.PropertyEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String MODE_MOVE_OUT = "MOVE_OUT";

    private EditText editAddress;
    private EditText editBedrooms;
    private EditText editBathrooms;
    private CheckBox checkExterior;
    private TextView textSetupStatus;
    private long currentPropertyId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        editAddress = findViewById(R.id.editAddress);
        editBedrooms = findViewById(R.id.editBedrooms);
        editBathrooms = findViewById(R.id.editBathrooms);
        checkExterior = findViewById(R.id.checkExteriorYard);
        textSetupStatus = findViewById(R.id.textSetupStatus);

        findViewById(R.id.buttonGenerateChecklist).setOnClickListener(v -> generateChecklist());
        findViewById(R.id.buttonStartMoveIn).setOnClickListener(v -> startCaptureFlow(MODE_MOVE_IN));
        findViewById(R.id.buttonStartMoveOut).setOnClickListener(v -> startCaptureFlow(MODE_MOVE_OUT));

        String preferredMode = getIntent().getStringExtra("mode");
        if (MODE_MOVE_OUT.equals(preferredMode)) {
            textSetupStatus.setText("Setup ready. Preferred mode: Move Out");
        } else {
            textSetupStatus.setText("Setup ready. Preferred mode: Move In");
        }
    }

    private void generateChecklist() {
        String address = safeText(editAddress.getText().toString());
        if (address.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show();
            return;
        }

        int bedrooms = parsePositiveInt(editBedrooms.getText().toString(), 1);
        int bathrooms = parsePositiveInt(editBathrooms.getText().toString(), 1);

        AppDatabase db = AppDatabase.getInstance(this);
        long createdAt = System.currentTimeMillis();
        PropertyEntity property = new PropertyEntity(address, bedrooms, bathrooms, createdAt);
        long propertyId = db.mediaDao().insertProperty(property);

        List<String> rooms = buildRooms(bedrooms, bathrooms, checkExterior.isChecked());
        List<ChecklistItemEntity> checklistItems = buildChecklistItems(propertyId, rooms);
        db.mediaDao().insertChecklistItems(checklistItems);

        currentPropertyId = propertyId;
        textSetupStatus.setText("Checklist generated for property #" + propertyId + " (" + checklistItems.size() + " items)");
        Toast.makeText(this, "Checklist generated", Toast.LENGTH_SHORT).show();
    }

    private void startCaptureFlow(String mode) {
        AppDatabase db = AppDatabase.getInstance(this);
        long propertyId = resolvePropertyId(db);
        if (propertyId <= 0) {
            Toast.makeText(this, "Generate checklist first", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ChecklistItemEntity> items = db.mediaDao().getChecklistItemsForProperty(propertyId);
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "Checklist is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("propertyId", propertyId);
        intent.putExtra("checklistItemId", items.get(0).id);
        startActivity(intent);
    }

    private long resolvePropertyId(AppDatabase db) {
        if (currentPropertyId > 0) {
            return currentPropertyId;
        }

        PropertyEntity latest = db.mediaDao().getLatestProperty();
        if (latest != null) {
            currentPropertyId = latest.id;
            return latest.id;
        }

        return -1L;
    }

    private List<String> buildRooms(int bedrooms, int bathrooms, boolean includeExterior) {
        List<String> rooms = new ArrayList<>();
        rooms.add("Entry Hall");
        rooms.add("Living Room");
        rooms.add("Kitchen");
        rooms.add("Hallway");

        for (int i = 1; i <= bedrooms; i++) {
            rooms.add("Bedroom " + i);
        }

        for (int i = 1; i <= bathrooms; i++) {
            rooms.add("Bathroom " + i);
        }

        if (includeExterior) {
            rooms.add("Exterior / Yard");
        }
        return rooms;
    }

    private List<ChecklistItemEntity> buildChecklistItems(long propertyId, List<String> rooms) {
        List<ChecklistItemEntity> items = new ArrayList<>();
        List<String> baseItems = Arrays.asList(
                "Walls",
                "Floor",
                "Ceiling",
                "Windows",
                "Doors",
                "Lights/Fixtures"
        );
        List<String> kitchenExtras = Arrays.asList(
                "Sink",
                "Countertops",
                "Cabinets",
                "Stove/Oven",
                "Fridge"
        );

        int order = 0;
        for (String room : rooms) {
            for (String itemName : baseItems) {
                items.add(new ChecklistItemEntity(propertyId, room, itemName, order++));
            }

            if ("Kitchen".equals(room)) {
                for (String itemName : kitchenExtras) {
                    items.add(new ChecklistItemEntity(propertyId, room, itemName, order++));
                }
            }
        }
        return items;
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
}
