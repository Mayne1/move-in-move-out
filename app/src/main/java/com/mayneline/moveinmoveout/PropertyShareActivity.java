package com.mayneline.moveinmoveout;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mayneline.moveinmoveout.firebase.FirebaseRepository;

public class PropertyShareActivity extends AppCompatActivity {
    public static final String EXTRA_PROPERTY_ID = "propertyId";
    public static final String EXTRA_ADDRESS_LABEL = "addressLabel";

    private FirebaseRepository repository;
    private String propertyId;
    private String latestToken = "";
    private String latestDeepLink = "";

    private EditText editTenantEmail;
    private ImageView imageQrInvite;
    private TextView textInviteLink;
    private TextView textInviteToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_share);

        repository = new FirebaseRepository();
        propertyId = safe(getIntent().getStringExtra(EXTRA_PROPERTY_ID));
        String addressLabel = safe(getIntent().getStringExtra(EXTRA_ADDRESS_LABEL));

        editTenantEmail = findViewById(R.id.editTenantEmail);
        imageQrInvite = findViewById(R.id.imageQrInvite);
        textInviteLink = findViewById(R.id.textInviteLink);
        textInviteToken = findViewById(R.id.textInviteToken);

        if (!addressLabel.isEmpty()) {
            setTitle(addressLabel);
        }

        findViewById(R.id.buttonCreateInvite).setOnClickListener(v -> createInvite());
        findViewById(R.id.buttonCopyToken).setOnClickListener(v -> copyToken());
    }

    private void createInvite() {
        String tenantEmail = safe(editTenantEmail.getText().toString()).toLowerCase();
        if (TextUtils.isEmpty(propertyId)) {
            Toast.makeText(this, "Property not found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(tenantEmail)) {
            Toast.makeText(this, "Tenant email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.createPropertyShare(propertyId, tenantEmail, new FirebaseRepository.RepoCallback<FirebaseRepository.PropertyShare>() {
            @Override
            public void onSuccess(FirebaseRepository.PropertyShare result) {
                runOnUiThread(() -> {
                    latestToken = safe(result.token);
                    latestDeepLink = "mimo://invite?token=" + latestToken;
                    textInviteLink.setText("Link: " + latestDeepLink);
                    textInviteToken.setText("Token: " + latestToken);
                    imageQrInvite.setImageBitmap(generateQr(latestDeepLink, 640));
                    Toast.makeText(PropertyShareActivity.this, "Invite created", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(PropertyShareActivity.this, "Failed to create invite", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void copyToken() {
        if (TextUtils.isEmpty(latestToken)) {
            Toast.makeText(this, "Create an invite first", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Clipboard unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("mimo_invite_token", latestToken));
        Toast.makeText(this, "Token copied", Toast.LENGTH_SHORT).show();
    }

    private Bitmap generateQr(String content, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
