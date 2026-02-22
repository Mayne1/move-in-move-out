package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        findViewById(R.id.buttonGenerateReport).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }
}
