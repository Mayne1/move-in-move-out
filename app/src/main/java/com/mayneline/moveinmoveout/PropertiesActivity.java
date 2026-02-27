package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mayneline.moveinmoveout.firebase.FirebaseRepository;
import com.mayneline.moveinmoveout.firebase.FirestoreProperty;
import com.mayneline.moveinmoveout.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PropertiesActivity extends AppCompatActivity {
    private FirebaseRepository repository;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private EditText editAddress;
    private EditText editUnit;
    private EditText editCity;
    private EditText editState;
    private EditText editZip;
    private TextView textPropertiesStatus;
    private RecyclerView recyclerProperties;

    private PropertiesAdapter adapter;
    private String currentRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_properties);

        repository = new FirebaseRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);

        editAddress = findViewById(R.id.editPropertyAddress);
        editUnit = findViewById(R.id.editPropertyUnit);
        editCity = findViewById(R.id.editPropertyCity);
        editState = findViewById(R.id.editPropertyState);
        editZip = findViewById(R.id.editPropertyZip);
        textPropertiesStatus = findViewById(R.id.textPropertiesStatus);
        recyclerProperties = findViewById(R.id.recyclerProperties);

        recyclerProperties.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PropertiesAdapter(this::openPropertyDetail);
        recyclerProperties.setAdapter(adapter);

        findViewById(R.id.buttonCreateProperty).setOnClickListener(v -> createProperty());

        loadProperties();
    }

    private void loadProperties() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            textPropertiesStatus.setText("Not signed in");
            return;
        }
        sessionManager.updateUser(user, "PropertiesActivity#loadProperties");

        String cachedRole = sessionManager.getRole("PropertiesActivity#cached");
        if (cachedRole == null || cachedRole.trim().isEmpty()) {
            textPropertiesStatus.setText("Loading role...");
        } else {
            currentRole = cachedRole;
            updateCreateFormVisibility();
        }

        sessionManager.loadSession("PropertiesActivity", role -> runOnUiThread(() -> {
            if (role == null || role.trim().isEmpty()) {
                textPropertiesStatus.setText("Role missing. Complete role selection first.");
                return;
            }
            currentRole = role;
            updateCreateFormVisibility();
            loadRowsForRole(user.getUid(), role);
        }));
    }

    private void loadRowsForRole(String uid, String role) {
        repository.listPropertiesForUser(uid, role, new FirebaseRepository.RepoCallback<List<FirestoreProperty>>() {
            @Override
            public void onSuccess(List<FirestoreProperty> result) {
                runOnUiThread(() -> {
                    adapter.setItems(result);
                    if (result.isEmpty()) {
                        if ("TENANT".equalsIgnoreCase(role)) {
                            textPropertiesStatus.setText("No shared properties yet.");
                        } else {
                            textPropertiesStatus.setText("No properties yet.");
                        }
                    } else {
                        textPropertiesStatus.setText("Loaded " + result.size() + " properties.");
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> textPropertiesStatus.setText("failed to load rows"));
            }
        });
    }

    private void updateCreateFormVisibility() {
        int createVisibility = "LANDLORD".equalsIgnoreCase(currentRole) ? View.VISIBLE : View.GONE;
        editAddress.setVisibility(createVisibility);
        editUnit.setVisibility(createVisibility);
        editCity.setVisibility(createVisibility);
        editState.setVisibility(createVisibility);
        editZip.setVisibility(createVisibility);
        findViewById(R.id.buttonCreateProperty).setVisibility(createVisibility);
    }

    private void openPropertyDetail(FirestoreProperty row) {
        Intent intent = new Intent(this, PropertyDetailActivity.class);
        intent.putExtra(PropertyDetailActivity.EXTRA_PROPERTY_ID, safe(row.propertyId));
        intent.putExtra(PropertyDetailActivity.EXTRA_ADDRESS_LINE, safe(row.addressLine));
        intent.putExtra(PropertyDetailActivity.EXTRA_UNIT, safe(row.unit));
        intent.putExtra(PropertyDetailActivity.EXTRA_CITY, safe(row.city));
        intent.putExtra(PropertyDetailActivity.EXTRA_STATE, safe(row.state));
        intent.putExtra(PropertyDetailActivity.EXTRA_ZIP, safe(row.zip));
        intent.putExtra(PropertyDetailActivity.EXTRA_ROLE, currentRole);
        startActivity(intent);
    }

    private void createProperty() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in to create a property", Toast.LENGTH_SHORT).show();
            return;
        }

        String address = safe(editAddress.getText().toString());
        String city = safe(editCity.getText().toString());
        String state = safe(editState.getText().toString());
        String zip = safe(editZip.getText().toString());

        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(city) || TextUtils.isEmpty(state) || TextUtils.isEmpty(zip)) {
            Toast.makeText(this, "Address, city, state, zip are required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseRepository.PropertyInput input = new FirebaseRepository.PropertyInput();
        input.addressLine = address;
        input.unit = safe(editUnit.getText().toString());
        input.city = city;
        input.state = state;
        input.zip = zip;

        repository.createProperty(input, new FirebaseRepository.RepoCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(
                            PropertiesActivity.this,
                            "Property submitted. Verification pending (20-48 hours).",
                            Toast.LENGTH_LONG
                    ).show();
                    clearCreateForm();
                    loadProperties();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(PropertiesActivity.this, "Failed to create property", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void clearCreateForm() {
        editAddress.setText("");
        editUnit.setText("");
        editCity.setText("");
        editState.setText("");
        editZip.setText("");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class PropertiesAdapter extends RecyclerView.Adapter<PropertiesAdapter.Holder> {
        interface OnPropertyClickListener {
            void onPropertyClick(FirestoreProperty row);
        }

        private final List<FirestoreProperty> items = new ArrayList<>();
        private final OnPropertyClickListener listener;

        PropertiesAdapter(OnPropertyClickListener listener) {
            this.listener = listener;
        }

        void setItems(List<FirestoreProperty> rows) {
            items.clear();
            if (rows != null) {
                items.addAll(rows);
            }
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            FirestoreProperty row = items.get(position);
            holder.title.setText((row.addressLine == null ? "" : row.addressLine) +
                    ((row.unit == null || row.unit.isEmpty()) ? "" : " Unit " + row.unit));
            holder.subtitle.setText((row.city == null ? "" : row.city) + ", " +
                    (row.state == null ? "" : row.state) + " " +
                    (row.zip == null ? "" : row.zip));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPropertyClick(row);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView subtitle;

            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
