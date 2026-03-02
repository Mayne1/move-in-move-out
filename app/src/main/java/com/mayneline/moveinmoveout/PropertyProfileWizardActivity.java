package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

public class PropertyProfileWizardActivity extends AppCompatActivity {
    private static final String FLOW_MOVE_IN = "move_in";
    private static final String FLOW_MOVE_OUT = "move_out";
    private static final String FLOW_TOUR_VIEWING = "tour_viewing";

    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String MODE_MOVE_OUT = "MOVE_OUT";

    private Spinner spinnerDwellingType;
    private EditText editAddressLine;
    private EditText editUnit;
    private EditText editCity;
    private EditText editState;
    private EditText editZip;
    private EditText editSquareFeet;
    private EditText editBedrooms;
    private EditText editBathrooms;

    private CheckBox checkKitchen;
    private CheckBox checkLivingRoom;
    private CheckBox checkDiningRoom;
    private CheckBox checkGarage;
    private CheckBox checkYard;
    private CheckBox checkBalcony;
    private CheckBox checkLaundry;

    private CheckBox checkPool;
    private CheckBox checkSprinklers;
    private CheckBox checkCentralAir;

    private CheckBox checkStoveOven;
    private CheckBox checkRefrigerator;
    private CheckBox checkMicrowave;
    private CheckBox checkDishwasher;
    private CheckBox checkWasher;
    private CheckBox checkDryer;

    private LinearLayout stepBasics;
    private LinearLayout stepRooms;
    private LinearLayout stepFeatures;
    private TextView textStep;

    private Button buttonBack;
    private Button buttonNext;
    private Button buttonSave;

    private int currentStep = 0;
    private String flowType = FLOW_MOVE_IN;

    private AppDatabase db;
    private FirebaseRepository firebaseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_profile_wizard);

        db = AppDatabase.getInstance(this);
        firebaseRepository = new FirebaseRepository();

        flowType = normalizeFlowType(getIntent().getStringExtra(IntentActivity.EXTRA_FLOW_TYPE));

        bindViews();
        setupDwellingTypeSpinner();
        showStep(0);
    }

    private void bindViews() {
        spinnerDwellingType = findViewById(R.id.spinnerDwellingType);
        editAddressLine = findViewById(R.id.editAddressLine);
        editUnit = findViewById(R.id.editUnit);
        editCity = findViewById(R.id.editCity);
        editState = findViewById(R.id.editState);
        editZip = findViewById(R.id.editZip);
        editSquareFeet = findViewById(R.id.editSquareFeet);
        editBedrooms = findViewById(R.id.editBedrooms);
        editBathrooms = findViewById(R.id.editBathrooms);

        checkKitchen = findViewById(R.id.checkKitchen);
        checkLivingRoom = findViewById(R.id.checkLivingRoom);
        checkDiningRoom = findViewById(R.id.checkDiningRoom);
        checkGarage = findViewById(R.id.checkGarage);
        checkYard = findViewById(R.id.checkYard);
        checkBalcony = findViewById(R.id.checkBalcony);
        checkLaundry = findViewById(R.id.checkLaundry);

        checkPool = findViewById(R.id.checkPool);
        checkSprinklers = findViewById(R.id.checkSprinklers);
        checkCentralAir = findViewById(R.id.checkCentralAir);

        checkStoveOven = findViewById(R.id.checkStoveOven);
        checkRefrigerator = findViewById(R.id.checkRefrigerator);
        checkMicrowave = findViewById(R.id.checkMicrowave);
        checkDishwasher = findViewById(R.id.checkDishwasher);
        checkWasher = findViewById(R.id.checkWasher);
        checkDryer = findViewById(R.id.checkDryer);

        stepBasics = findViewById(R.id.stepBasics);
        stepRooms = findViewById(R.id.stepRooms);
        stepFeatures = findViewById(R.id.stepFeatures);
        textStep = findViewById(R.id.textStep);

        TextView textFlow = findViewById(R.id.textFlowTypeValue);
        textFlow.setText(flowType);

        buttonBack = findViewById(R.id.buttonBackStep);
        buttonNext = findViewById(R.id.buttonNextStep);
        buttonSave = findViewById(R.id.buttonSaveProfile);

        buttonBack.setOnClickListener(v -> showStep(currentStep - 1));
        buttonNext.setOnClickListener(v -> showStep(currentStep + 1));
        buttonSave.setOnClickListener(v -> saveProfile());
    }

    private void setupDwellingTypeSpinner() {
        String[] dwellingTypes = new String[]{
                "Select dwelling type",
                "Apartment",
                "House",
                "Condo",
                "Duplex",
                "Townhouse",
                "Room",
                "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                dwellingTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDwellingType.setAdapter(adapter);
    }

    private void showStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex > 2) {
            return;
        }
        currentStep = stepIndex;

        stepBasics.setVisibility(currentStep == 0 ? View.VISIBLE : View.GONE);
        stepRooms.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        stepFeatures.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);

        textStep.setText("Step " + (currentStep + 1) + " of 3");
        buttonBack.setVisibility(currentStep == 0 ? View.GONE : View.VISIBLE);
        buttonNext.setVisibility(currentStep < 2 ? View.VISIBLE : View.GONE);
        buttonSave.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
    }

    private void saveProfile() {
        String dwellingType = selectedDwellingType();
        String addressLine = safe(editAddressLine.getText() == null ? null : editAddressLine.getText().toString());
        String city = safe(editCity.getText() == null ? null : editCity.getText().toString());
        String state = safe(editState.getText() == null ? null : editState.getText().toString());
        String zip = safe(editZip.getText() == null ? null : editZip.getText().toString());

        if (dwellingType.isEmpty()) {
            Toast.makeText(this, "Dwelling type is required", Toast.LENGTH_SHORT).show();
            showStep(0);
            return;
        }
        if (addressLine.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show();
            showStep(0);
            return;
        }
        if (city.isEmpty()) {
            Toast.makeText(this, "City is required", Toast.LENGTH_SHORT).show();
            showStep(0);
            return;
        }
        if (state.isEmpty()) {
            Toast.makeText(this, "State is required", Toast.LENGTH_SHORT).show();
            showStep(0);
            return;
        }
        if (zip.isEmpty()) {
            Toast.makeText(this, "ZIP is required", Toast.LENGTH_SHORT).show();
            showStep(0);
            return;
        }

        buttonSave.setEnabled(false);

        FirebaseRepository.PropertyProfileInput input = new FirebaseRepository.PropertyProfileInput();
        input.dwellingType = dwellingType;
        input.addressLine = addressLine;
        input.unit = safe(editUnit.getText() == null ? null : editUnit.getText().toString());
        input.city = city;
        input.state = state;
        input.zip = zip;
        input.squareFeet = parseOptionalInt(editSquareFeet.getText() == null ? null : editSquareFeet.getText().toString());

        input.bedrooms = parseInt(editBedrooms.getText() == null ? null : editBedrooms.getText().toString(), 0);
        input.bathrooms = parseInt(editBathrooms.getText() == null ? null : editBathrooms.getText().toString(), 0);
        input.hasKitchen = checkKitchen.isChecked();
        input.hasLivingRoom = checkLivingRoom.isChecked();
        input.hasDiningRoom = checkDiningRoom.isChecked();
        input.hasGarage = checkGarage.isChecked();
        input.hasYard = checkYard.isChecked();
        input.hasBalcony = checkBalcony.isChecked();
        input.hasLaundryRoom = checkLaundry.isChecked();

        input.hasPool = checkPool.isChecked();
        input.hasSprinklers = checkSprinklers.isChecked();
        input.hasCentralAir = checkCentralAir.isChecked();

        input.hasStoveOven = checkStoveOven.isChecked();
        input.hasRefrigerator = checkRefrigerator.isChecked();
        input.hasMicrowave = checkMicrowave.isChecked();
        input.hasDishwasher = checkDishwasher.isChecked();
        input.hasWasher = checkWasher.isChecked();
        input.hasDryer = checkDryer.isChecked();

        input.flowType = flowType;
        input.createdAtMs = System.currentTimeMillis();

        firebaseRepository.createPropertyProfile(input, new FirebaseRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String profileDocId) {
                runOnUiThread(() -> onProfileSaved(profileDocId));
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    buttonSave.setEnabled(true);
                    Toast.makeText(PropertyProfileWizardActivity.this, "Failed to save property profile", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void onProfileSaved(String profileDocId) {
        long localPropertyId = createLocalProperty();
        if (localPropertyId <= 0) {
            buttonSave.setEnabled(true);
            Toast.makeText(this, "Could not prepare local checklist", Toast.LENGTH_LONG).show();
            return;
        }

        String mode = toInspectionMode(flowType);
        createCloudSessionAndLaunch(localPropertyId, profileDocId, mode);
    }

    private long createLocalProperty() {
        String addressLine = safe(editAddressLine.getText() == null ? null : editAddressLine.getText().toString());
        String city = safe(editCity.getText() == null ? null : editCity.getText().toString());
        String state = safe(editState.getText() == null ? null : editState.getText().toString());
        String zip = safe(editZip.getText() == null ? null : editZip.getText().toString());

        int bedrooms = parseInt(editBedrooms.getText() == null ? null : editBedrooms.getText().toString(), 1);
        String bathrooms = String.valueOf(parseInt(editBathrooms.getText() == null ? null : editBathrooms.getText().toString(), 1));
        boolean hasGarage = checkGarage.isChecked();
        boolean hasYard = checkYard.isChecked();
        boolean hasSprinklers = checkSprinklers.isChecked();

        PropertyProfile profile = new PropertyProfile(
                addressLine,
                city,
                state,
                zip,
                bedrooms,
                bathrooms,
                hasGarage,
                hasGarage ? "ATTACHED" : "NONE",
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
        return propertyId;
    }

    private void createCloudSessionAndLaunch(long localPropertyId, String profileDocId, String mode) {
        FirebaseRepository.PropertyInput input = new FirebaseRepository.PropertyInput();
        input.addressLine = safe(editAddressLine.getText() == null ? null : editAddressLine.getText().toString());
        input.unit = safe(editUnit.getText() == null ? null : editUnit.getText().toString());
        input.city = safe(editCity.getText() == null ? null : editCity.getText().toString());
        input.state = safe(editState.getText() == null ? null : editState.getText().toString());
        input.zip = safe(editZip.getText() == null ? null : editZip.getText().toString());

        firebaseRepository.createProperty(input, new FirebaseRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String remotePropertyId) {
                firebaseRepository.createInspection(remotePropertyId, mode, new FirebaseRepository.RepoCallback<String>() {
                    @Override
                    public void onSuccess(String inspectionId) {
                        runOnUiThread(() -> launchInspection(localPropertyId, profileDocId, mode, remotePropertyId, inspectionId));
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> {
                            Toast.makeText(PropertyProfileWizardActivity.this, "Inspection cloud session unavailable", Toast.LENGTH_SHORT).show();
                            launchInspection(localPropertyId, profileDocId, mode, null, null);
                        });
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(PropertyProfileWizardActivity.this, "Cloud property session unavailable", Toast.LENGTH_SHORT).show();
                    launchInspection(localPropertyId, profileDocId, mode, null, null);
                });
            }
        });
    }

    private void launchInspection(long localPropertyId, String profileDocId, String mode, String remotePropertyId, String inspectionId) {
        Intent intent = new Intent(this, InspectionWizardActivity.class);
        intent.putExtra("propertyId", localPropertyId);
        intent.putExtra("mode", mode);
        intent.putExtra("flowType", flowType);
        intent.putExtra("propertyProfileDocId", profileDocId);

        if (!TextUtils.isEmpty(remotePropertyId) && !TextUtils.isEmpty(inspectionId)) {
            intent.putExtra("firestorePropertyId", remotePropertyId);
            intent.putExtra("inspectionId", inspectionId);
        }

        startActivity(intent);
        finish();
    }

    private String toInspectionMode(String flowTypeValue) {
        if (FLOW_MOVE_OUT.equals(flowTypeValue)) {
            return MODE_MOVE_OUT;
        }
        return MODE_MOVE_IN;
    }

    private String normalizeFlowType(String value) {
        if (FLOW_MOVE_OUT.equals(value)) {
            return FLOW_MOVE_OUT;
        }
        if (FLOW_TOUR_VIEWING.equals(value)) {
            return FLOW_TOUR_VIEWING;
        }
        return FLOW_MOVE_IN;
    }

    private String selectedDwellingType() {
        Object selected = spinnerDwellingType.getSelectedItem();
        if (selected == null) {
            return "";
        }
        String dwellingType = selected.toString().trim();
        if (dwellingType.equalsIgnoreCase("Select dwelling type")) {
            return "";
        }
        return dwellingType;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(safe(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Integer parseOptionalInt(String value) {
        String raw = safe(value);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
