package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CaptureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        TextView textCaptureContext = findViewById(R.id.textCaptureContext);
        textCaptureContext.setText(buildContextText());

        findViewById(R.id.buttonPhoto).setOnClickListener(v ->
                Toast.makeText(this, "Photo capture coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.buttonVideo).setOnClickListener(v ->
                Toast.makeText(this, "Video capture coming soon", Toast.LENGTH_SHORT).show());
    }

    private String buildContextText() {
        String mode = getIntent().getStringExtra("mode");
        String room = getIntent().getStringExtra("room");
        String item = getIntent().getStringExtra("item");
        if (isBlank(mode) || isBlank(room) || isBlank(item)) {
            return "No context provided.";
        }
        return "Mode: " + mode + "\nRoom: " + room + "\nItem: " + item;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
