package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.mayneline.moveinmoveout.session.SessionManager;

public class SignUpActivity extends AppCompatActivity {
    private EditText editEmail;
    private EditText editPassword;
    private EditText editConfirmPassword;

    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);

        editEmail = findViewById(R.id.editSignUpEmail);
        editPassword = findViewById(R.id.editSignUpPassword);
        editConfirmPassword = findViewById(R.id.editSignUpConfirmPassword);

        findViewById(R.id.buttonCreateAccount).setOnClickListener(v -> signUp());
    }

    private void signUp() {
        String email = safe(editEmail.getText().toString());
        String password = safe(editPassword.getText().toString());
        String confirmPassword = safe(editConfirmPassword.getText().toString());

        if (!validate(email, password, confirmPassword)) {
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    sessionManager.updateUser(result.getUser(), "SignUpActivity#signUpSuccess");
                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, IntentActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Sign up failed", Toast.LENGTH_SHORT).show());
    }

    private boolean validate(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
