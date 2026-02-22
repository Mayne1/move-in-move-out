package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CaptureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        findViewById(R.id.buttonPhoto).setOnClickListener(v ->
                Toast.makeText(this, "Photo capture coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.buttonVideo).setOnClickListener(v ->
                Toast.makeText(this, "Video capture coming soon", Toast.LENGTH_SHORT).show());
    }
}
