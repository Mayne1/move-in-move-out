package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MoveInWizardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_in_wizard);

        findViewById(R.id.buttonStartCaptureMoveIn).setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));

        findViewById(R.id.buttonContinueMoveIn).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }
}
