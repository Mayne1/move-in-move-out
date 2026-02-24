package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.PropertyProfile;

public class HomeActivity extends AppCompatActivity {
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        db = AppDatabase.getInstance(this);

        findViewById(R.id.buttonMoveIn).setOnClickListener(v ->
                startGuidedFlow("MOVE_IN"));
        findViewById(R.id.buttonMoveOut).setOnClickListener(v ->
                startGuidedFlow("MOVE_OUT"));
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
        findViewById(R.id.buttonBuildInfo).setOnClickListener(v ->
                startActivity(new Intent(this, BuildInfoActivity.class)));
    }

    private void startGuidedFlow(String mode) {
        PropertyProfile latest = db.propertyDao().getLatestProperty();
        if (latest == null) {
            openPropertyIntake(mode);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Use latest property?")
                .setMessage(latest.addressLine1 + ", " + latest.city + ", " + latest.state + " " + latest.zip)
                .setPositiveButton("Yes", (dialog, which) -> openInspectionWizard(mode, latest.id))
                .setNegativeButton("No", (dialog, which) -> openPropertyIntake(mode))
                .show();
    }

    private void openPropertyIntake(String mode) {
        Intent intent = new Intent(this, PropertyProfileActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    private void openInspectionWizard(String mode, long propertyId) {
        if (propertyId <= 0) {
            Toast.makeText(this, "Property not found", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, InspectionWizardActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("propertyId", propertyId);
        startActivity(intent);
    }
}
