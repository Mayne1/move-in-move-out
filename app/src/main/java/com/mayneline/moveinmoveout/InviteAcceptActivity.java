package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mayneline.moveinmoveout.firebase.FirebaseRepository;

public class InviteAcceptActivity extends AppCompatActivity {
    private FirebaseRepository repository;
    private FirebaseAuth auth;

    private TextView textStatus;
    private Button buttonAccept;

    private String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_accept);

        repository = new FirebaseRepository();
        auth = FirebaseAuth.getInstance();

        textStatus = findViewById(R.id.textInviteAcceptStatus);
        buttonAccept = findViewById(R.id.buttonAcceptInvite);

        token = readTokenFromIntent(getIntent());
        if (TextUtils.isEmpty(token)) {
            textStatus.setText("Invalid invite token.");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            textStatus.setText("Sign in to accept this invite.");
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        loadInvite();

        buttonAccept.setOnClickListener(v -> acceptInvite());
    }

    private void loadInvite() {
        textStatus.setText("Checking invite...");
        repository.getShareByToken(token, new FirebaseRepository.RepoCallback<FirebaseRepository.PropertyShare>() {
            @Override
            public void onSuccess(FirebaseRepository.PropertyShare result) {
                runOnUiThread(() -> {
                    String currentEmail = auth.getCurrentUser() == null || auth.getCurrentUser().getEmail() == null
                            ? ""
                            : auth.getCurrentUser().getEmail().trim().toLowerCase();
                    String targetEmail = result.tenantEmailLower == null ? "" : result.tenantEmailLower.trim().toLowerCase();

                    if (!currentEmail.equals(targetEmail)) {
                        textStatus.setText("Invite is for a different email: " + targetEmail);
                        buttonAccept.setVisibility(View.GONE);
                        return;
                    }

                    if ("ACCEPTED".equalsIgnoreCase(result.status)
                            && auth.getCurrentUser() != null
                            && auth.getCurrentUser().getUid().equals(result.tenantUid)) {
                        textStatus.setText("Invite already accepted for this account.");
                        buttonAccept.setVisibility(View.GONE);
                        return;
                    }

                    if ("REVOKED".equalsIgnoreCase(result.status)) {
                        textStatus.setText("Invite was revoked.");
                        buttonAccept.setVisibility(View.GONE);
                        return;
                    }

                    textStatus.setText("Invite found for " + targetEmail + ". Tap Accept Invite.");
                    buttonAccept.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    textStatus.setText("Invite not found.");
                    buttonAccept.setVisibility(View.GONE);
                });
            }
        });
    }

    private void acceptInvite() {
        buttonAccept.setEnabled(false);
        repository.acceptShareByToken(token, new FirebaseRepository.RepoCallback<FirebaseRepository.PropertyShare>() {
            @Override
            public void onSuccess(FirebaseRepository.PropertyShare result) {
                runOnUiThread(() -> {
                    Toast.makeText(InviteAcceptActivity.this, "Invite accepted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(InviteAcceptActivity.this, PropertiesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    buttonAccept.setEnabled(true);
                    Toast.makeText(InviteAcceptActivity.this, "Failed to accept invite", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String readTokenFromIntent(Intent intent) {
        if (intent == null) {
            return "";
        }
        Uri data = intent.getData();
        if (data != null) {
            String tokenParam = data.getQueryParameter("token");
            if (!TextUtils.isEmpty(tokenParam)) {
                return tokenParam.trim();
            }
        }
        return safe(intent.getStringExtra("token"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
