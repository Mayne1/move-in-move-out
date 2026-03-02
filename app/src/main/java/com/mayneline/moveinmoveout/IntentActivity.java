package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class IntentActivity extends AppCompatActivity {
    public static final String EXTRA_FLOW_TYPE = "flowType";
    public static final String FLOW_MOVE_IN = "move_in";
    public static final String FLOW_MOVE_OUT = "move_out";
    public static final String FLOW_TOUR_VIEWING = "tour_viewing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intent);

        findViewById(R.id.cardMoveIn).setOnClickListener(v -> openWizard(FLOW_MOVE_IN));
        findViewById(R.id.cardMoveOut).setOnClickListener(v -> openWizard(FLOW_MOVE_OUT));
        findViewById(R.id.cardTourViewing).setOnClickListener(v -> openWizard(FLOW_TOUR_VIEWING));
        findViewById(R.id.cardBnB).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }

    private void openWizard(String flowType) {
        Intent intent = new Intent(this, PropertyProfileWizardActivity.class);
        intent.putExtra(EXTRA_FLOW_TYPE, flowType);
        startActivity(intent);
    }
}
