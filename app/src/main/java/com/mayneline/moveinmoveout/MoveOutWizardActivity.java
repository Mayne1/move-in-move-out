package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MoveOutWizardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_out_wizard);

        findViewById(R.id.buttonStartCaptureMoveOut).setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));

        findViewById(R.id.buttonContinueMoveOut).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }
}
