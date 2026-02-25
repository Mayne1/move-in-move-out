package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mayneline.moveinmoveout.session.SessionManager;

public class AccountActivity extends AppCompatActivity {
    private TextView textAccountEmail;
    private TextView textAccountRole;

    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);

        textAccountEmail = findViewById(R.id.textAccountEmail);
        textAccountRole = findViewById(R.id.textAccountRole);

        findViewById(R.id.buttonSignOut).setOnClickListener(v -> signOut());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            sessionManager.clearSession("AccountActivity#loadProfileSignedOut");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        textAccountEmail.setText("Email: " + (user.getEmail() == null ? "" : user.getEmail()));
        sessionManager.updateUser(user, "AccountActivity#loadProfile");

        String cachedRole = sessionManager.getRole("AccountActivity#cached");
        if (cachedRole == null || cachedRole.isEmpty()) {
            textAccountRole.setText("Role: Loading...");
        } else {
            textAccountRole.setText("Role: " + cachedRole + " (Loading...)");
        }

        sessionManager.loadSession("AccountActivity", role -> runOnUiThread(() -> {
            if (role != null && !role.trim().isEmpty()) {
                textAccountRole.setText("Role: " + role);
            } else if (auth.getCurrentUser() == null) {
                textAccountRole.setText("Role: Not set");
            } else {
                String keepCached = sessionManager.getRole("AccountActivity#postLoadFallback");
                textAccountRole.setText("Role: " + (keepCached == null || keepCached.isEmpty() ? "Not set" : keepCached));
            }
        }));
    }

    private void signOut() {
        auth.signOut();
        sessionManager.clearSession("AccountActivity#signOut");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
