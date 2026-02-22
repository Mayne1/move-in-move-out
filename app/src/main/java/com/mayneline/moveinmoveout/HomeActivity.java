package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        findViewById(R.id.buttonMoveIn).setOnClickListener(v ->
                openSetup("MOVE_IN"));
        findViewById(R.id.buttonMoveOut).setOnClickListener(v ->
                openSetup("MOVE_OUT"));
        findViewById(R.id.buttonReports).setOnClickListener(v ->
                startActivity(new Intent(this, ComparisonReportActivity.class)));
        findViewById(R.id.buttonTimeline).setOnClickListener(v ->
                startActivity(new Intent(this, TimelineActivity.class)));
        findViewById(R.id.buttonRespectFilter).setOnClickListener(v ->
                startActivity(new Intent(this, RespectFilterActivity.class)));
        findViewById(R.id.buttonLegalTranslator).setOnClickListener(v ->
                startActivity(new Intent(this, LegalTranslatorActivity.class)));
        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void openSetup(String mode) {
        Intent intent = new Intent(this, SetupActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }
}
