package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.PropertyProfile;
import com.mayneline.moveinmoveout.data.PropertyRoom;
import com.mayneline.moveinmoveout.data.RoomItem;
import com.mayneline.moveinmoveout.engine.PropertyChecklistGenerator;
import com.mayneline.moveinmoveout.firebase.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class PropertyProfileActivity extends AppCompatActivity {
    private EditText editAddressLine1;
    private EditText editCity;
    private EditText editState;
    private EditText editZip;
    private EditText editBedrooms;
    private EditText editBathrooms;
    private Spinner spinnerGarageType;
    private Switch switchGarage;
    private Switch switchYard;
    private Switch switchSprinklers;
    private TextView textPropertyStatus;

    private AppDatabase db;
    private String mode;
    private FirebaseRepository firebaseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_profile);

        db = AppDatabase.getInstance(this);
        firebaseRepository = new FirebaseRepository();
        mode = getIntent().getStringExtra("mode");
        if (mode == null || mode.trim().isEmpty()) {
            mode = "MOVE_IN";
        }

        editAddressLine1 = findViewById(R.id.editAddressLine1);
        editCity = findViewById(R.id.editCity);
        editState = findViewById(R.id.editState);
        editZip = findViewById(R.id.editZip);
        editBedrooms = findViewById(R.id.editBedrooms);
        editBathrooms = findViewById(R.id.editBathrooms);
        spinnerGarageType = findViewById(R.id.spinnerGarageType);
        switchGarage = findViewById(R.id.switchGarage);
        switchYard = findViewById(R.id.switchYard);
        switchSprinklers = findViewById(R.id.switchSprinklers);
        textPropertyStatus = findViewById(R.id.textPropertyStatus);

        ArrayAdapter<String> garageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"ATTACHED", "DETACHED", "NONE"}
        );
        garageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGarageType.setAdapter(garageAdapter);

        switchGarage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerGarageType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                spinnerGarageType.setSelection(2);
            }
        });
        switchYard.setOnCheckedChangeListener((buttonView, isChecked) ->
                switchSprinklers.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        spinnerGarageType.setVisibility(View.GONE);
        switchSprinklers.setVisibility(View.GONE);

        findViewById(R.id.buttonSaveGenerate).setOnClickListener(v -> saveAndGenerate());
    }

    private void saveAndGenerate() {
        String address = safe(editAddressLine1.getText().toString());
        if (address.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show();
            return;
        }

        int bedrooms = parseInt(editBedrooms.getText().toString(), 1);
        String bathrooms = normalizeBathrooms(editBathrooms.getText().toString());
        boolean hasGarage = switchGarage.isChecked();
        String garageType = hasGarage ? String.valueOf(spinnerGarageType.getSelectedItem()) : "NONE";
        boolean hasYard = switchYard.isChecked();
        boolean hasSprinklers = hasYard && switchSprinklers.isChecked();

        PropertyProfile profile = new PropertyProfile(
                address,
                safe(editCity.getText().toString()),
                safe(editState.getText().toString()),
                safe(editZip.getText().toString()),
                bedrooms,
                bathrooms,
                hasGarage,
                garageType,
                hasYard,
                hasSprinklers,
                System.currentTimeMillis()
        );

        long propertyId = db.propertyDao().insertProperty(profile);
        List<String> roomNames = PropertyChecklistGenerator.generateRooms(profile);

        List<PropertyRoom> roomsToInsert = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String roomName : roomNames) {
            roomsToInsert.add(new PropertyRoom(propertyId, roomName, now));
        }
        List<Long> roomIds = db.propertyRoomDao().insertRooms(roomsToInsert);

        List<RoomItem> itemsToInsert = new ArrayList<>();
        for (int i = 0; i < roomNames.size(); i++) {
            String roomName = roomNames.get(i);
            long roomId = roomIds.get(i);
            for (String itemName : PropertyChecklistGenerator.generateItems(roomName, hasSprinklers)) {
                itemsToInsert.add(new RoomItem(roomId, itemName, now));
            }
        }
        db.roomItemDao().insertItems(itemsToInsert);

        textPropertyStatus.setText("Saved locally. Creating cloud session...");
        createCloudSessionAndLaunch(propertyId, profile);
    }

    private void createCloudSessionAndLaunch(long localPropertyId, PropertyProfile profile) {
        FirebaseRepository.PropertyInput input = new FirebaseRepository.PropertyInput();
        input.addressLine = profile.addressLine1;
        input.unit = "";
        input.city = profile.city;
        input.state = profile.state;
        input.zip = profile.zip;

        firebaseRepository.createProperty(input, new FirebaseRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String remotePropertyId) {
                firebaseRepository.createInspection(remotePropertyId, mode, new FirebaseRepository.RepoCallback<String>() {
                    @Override
                    public void onSuccess(String inspectionId) {
                        runOnUiThread(() -> {
                            textPropertyStatus.setText("Cloud session ready.");
                            Intent intent = new Intent(PropertyProfileActivity.this, InspectionWizardActivity.class);
                            intent.putExtra("propertyId", localPropertyId);
                            intent.putExtra("mode", mode);
                            intent.putExtra("firestorePropertyId", remotePropertyId);
                            intent.putExtra("inspectionId", inspectionId);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> {
                            textPropertyStatus.setText("Could not create inspection session.");
                            Toast.makeText(PropertyProfileActivity.this, "Failed to create inspection session in cloud", Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    textPropertyStatus.setText("Could not create property in cloud.");
                    Toast.makeText(PropertyProfileActivity.this, "Failed to create property in cloud", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String normalizeBathrooms(String value) {
        String raw = safe(value);
        return raw.isEmpty() ? "1" : raw;
    }

    private int parseInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
