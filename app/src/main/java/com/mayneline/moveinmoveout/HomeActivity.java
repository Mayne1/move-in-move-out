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
                startActivity(new Intent(this, MoveInWizardActivity.class)));
        findViewById(R.id.buttonMoveOut).setOnClickListener(v ->
                startActivity(new Intent(this, MoveOutWizardActivity.class)));
        findViewById(R.id.buttonReports).setOnClickListener(v ->
                startActivity(new Intent(this, ReportActivity.class)));
        findViewById(R.id.buttonTimeline).setOnClickListener(v ->
                startActivity(new Intent(this, TimelineActivity.class)));
        findViewById(R.id.buttonRespectFilter).setOnClickListener(v ->
                startActivity(new Intent(this, RespectFilterActivity.class)));
        findViewById(R.id.buttonLegalTranslator).setOnClickListener(v ->
                startActivity(new Intent(this, LegalTranslatorActivity.class)));
        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }
}
