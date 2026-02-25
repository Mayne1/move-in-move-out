package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {
    private TextView textAccountEmail;
    private TextView textAccountRole;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        textAccountEmail = findViewById(R.id.textAccountEmail);
        textAccountRole = findViewById(R.id.textAccountRole);

        findViewById(R.id.buttonSignOut).setOnClickListener(v -> signOut());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        textAccountEmail.setText("Email: " + (user.getEmail() == null ? "" : user.getEmail()));

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot != null ? snapshot.getString("role") : null;
                    textAccountRole.setText("Role: " + (role == null ? "Not set" : role));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load role", Toast.LENGTH_SHORT).show());
    }

    private void signOut() {
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
