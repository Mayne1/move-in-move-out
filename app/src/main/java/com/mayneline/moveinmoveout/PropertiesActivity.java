package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mayneline.moveinmoveout.firebase.FirebaseRepository;
import com.mayneline.moveinmoveout.firebase.FirestoreProperty;

import java.util.ArrayList;
import java.util.List;

public class PropertiesActivity extends AppCompatActivity {
    private FirebaseRepository repository;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private EditText editAddress;
    private EditText editUnit;
    private EditText editCity;
    private EditText editState;
    private EditText editZip;
    private TextView textPropertiesStatus;
    private RecyclerView recyclerProperties;

    private PropertiesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_properties);

        repository = new FirebaseRepository();
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editAddress = findViewById(R.id.editPropertyAddress);
        editUnit = findViewById(R.id.editPropertyUnit);
        editCity = findViewById(R.id.editPropertyCity);
        editState = findViewById(R.id.editPropertyState);
        editZip = findViewById(R.id.editPropertyZip);
        textPropertiesStatus = findViewById(R.id.textPropertiesStatus);
        recyclerProperties = findViewById(R.id.recyclerProperties);

        recyclerProperties.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PropertiesAdapter();
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

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot == null ? null : snapshot.getString("role");
                    if (role == null || role.trim().isEmpty()) {
                        textPropertiesStatus.setText("Role missing. Complete role selection first.");
                        return;
                    }
                    repository.listPropertiesForUser(user.getUid(), role, new FirebaseRepository.RepoCallback<List<FirestoreProperty>>() {
                        @Override
                        public void onSuccess(List<FirestoreProperty> result) {
                            runOnUiThread(() -> {
                                adapter.setItems(result);
                                if (result.isEmpty()) {
                                    if ("TENANT".equalsIgnoreCase(role)) {
                                        textPropertiesStatus.setText("No shared properties yet (tenant invites coming next).");
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
                            runOnUiThread(() -> textPropertiesStatus.setText("Failed to load properties."));
                        }
                    });
                })
                .addOnFailureListener(e -> textPropertiesStatus.setText("Failed to load user role."));
    }

    private void createProperty() {
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
                    Toast.makeText(PropertiesActivity.this, "Property created", Toast.LENGTH_SHORT).show();
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
        private final List<FirestoreProperty> items = new ArrayList<>();

        void setItems(List<FirestoreProperty> rows) {
            items.clear();
            if (rows != null) {
                items.addAll(rows);
            }
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
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
