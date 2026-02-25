package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mayneline.moveinmoveout.session.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private EditText editEmail;
    private EditText editPassword;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sessionManager = SessionManager.getInstance(this);

        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);

        findViewById(R.id.buttonLogin).setOnClickListener(v -> signIn());
        findViewById(R.id.buttonSignUp).setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            routeAfterAuth(currentUser);
        }
    }

    private void signIn() {
        String email = safe(editEmail.getText().toString());
        String password = safe(editPassword.getText().toString());
        if (!validate(email, password)) {
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> routeAfterAuth(result.getUser()))
                .addOnFailureListener(e -> Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show());
    }

    private void routeAfterAuth(FirebaseUser user) {
        if (user == null) {
            return;
        }
        sessionManager.updateUser(user, "LoginActivity#routeAfterAuth");

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> routeFromUserDoc(snapshot))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void routeFromUserDoc(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists() || !snapshot.contains("role")) {
            startActivity(new Intent(this, RoleSelectActivity.class));
            finish();
            return;
        }
        String role = snapshot.getString("role");
        sessionManager.setRole(role, "LoginActivity#routeFromUserDoc");

        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private boolean validate(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
