package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TimelineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        findViewById(R.id.buttonAddEntry).setOnClickListener(v ->
                Toast.makeText(this, "Add entry coming soon", Toast.LENGTH_SHORT).show());
    }
}
