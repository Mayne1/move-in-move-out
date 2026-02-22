package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.model.InspectionChecklistFactory;
import com.mayneline.moveinmoveout.ui.InspectionChecklistAdapter;

public class MoveInWizardActivity extends AppCompatActivity {
    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String PREFS_NAME = "inspection_checklist_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_in_wizard);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        RecyclerView recyclerChecklist = findViewById(R.id.recyclerChecklistMoveIn);
        recyclerChecklist.setLayoutManager(new LinearLayoutManager(this));
        recyclerChecklist.setAdapter(new InspectionChecklistAdapter(
                MODE_MOVE_IN,
                prefs,
                InspectionChecklistFactory.createBaselineSections(),
                this::openCaptureForItem));

        findViewById(R.id.buttonContinueMoveIn).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }

    private void openCaptureForItem(String roomName, String itemName) {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra("mode", MODE_MOVE_IN);
        intent.putExtra("room", roomName);
        intent.putExtra("item", itemName);
        startActivity(intent);
    }
}
