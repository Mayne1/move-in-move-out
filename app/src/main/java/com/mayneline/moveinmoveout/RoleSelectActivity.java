package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mayneline.moveinmoveout.session.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class RoleSelectActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sessionManager = SessionManager.getInstance(this);

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        findViewById(R.id.buttonRoleLandlord).setOnClickListener(v -> saveRole("LANDLORD"));
        findViewById(R.id.buttonRoleTenant).setOnClickListener(v -> saveRole("TENANT"));
    }

    private void saveRole(String role) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            sessionManager.clearSession("RoleSelectActivity#saveRoleSignedOut");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        sessionManager.updateUser(user, "RoleSelectActivity#saveRole");

        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail() == null ? "" : user.getEmail());
        data.put("role", role);
        data.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(unused -> {
                    sessionManager.setRole(role, "RoleSelectActivity#saveRoleSuccess");
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save role", Toast.LENGTH_SHORT).show());
    }
}
