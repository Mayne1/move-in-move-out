package com.mayneline.moveinmoveout;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.PropertyProfile;
import com.mayneline.moveinmoveout.data.PropertyRoom;
import com.mayneline.moveinmoveout.data.RoomItem;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.model.ComparisonRow;
import com.mayneline.moveinmoveout.ui.ComparisonReportAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ComparisonReportActivity extends AppCompatActivity {
    private static final String MODE_MOVE_IN = "MOVE_IN";
    private static final String MODE_MOVE_OUT = "MOVE_OUT";

    private RecyclerView recyclerComparison;
    private TextView textEmpty;
    private TextView textSummary;

    private AppDatabase db;
    private PropertyProfile property;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_report);

        db = AppDatabase.getInstance(this);

        recyclerComparison = findViewById(R.id.recyclerComparison);
        textEmpty = findViewById(R.id.textEmpty);
        textSummary = findViewById(R.id.textSummary);

        recyclerComparison.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.buttonGeneratePdf).setOnClickListener(v -> loadComparisonRows());
        findViewById(R.id.buttonSeedDemoPair).setOnClickListener(v -> seedDemoPair());

        loadComparisonRows();
    }

    private void loadComparisonRows() {
        long propertyId = getIntent().getLongExtra("propertyProfileId", -1L);
        property = propertyId > 0 ? db.propertyDao().getPropertyById(propertyId) : db.propertyDao().getLatestProperty();

        if (property == null) {
            textSummary.setText("No property found.");
            recyclerComparison.setAdapter(new ComparisonReportAdapter(new ArrayList<>()));
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<PropertyRoom> rooms = db.propertyRoomDao().getRoomsForProperty(property.id);
        List<ComparisonRow> rows = new ArrayList<>();
        int missingCount = 0;
        int noChangeCount = 0;
        int needsReviewCount = 0;

        for (PropertyRoom room : rooms) {
            List<RoomItem> items = db.roomItemDao().getItemsForRoom(room.id);
            for (RoomItem item : items) {
                RoomItemMedia moveIn = resolvePrimaryMedia(property.id, room, item, MODE_MOVE_IN);
                RoomItemMedia moveOut = resolvePrimaryMedia(property.id, room, item, MODE_MOVE_OUT);

                String status;
                if (moveIn == null || moveOut == null) {
                    status = "Media Missing";
                    missingCount++;
                } else if (bestPath(moveIn).equals(bestPath(moveOut))) {
                    status = "No Change";
                    noChangeCount++;
                } else {
                    status = "Needs Review";
                    needsReviewCount++;
                }

                rows.add(new ComparisonRow(
                        room.name,
                        item.name,
                        moveIn == null ? "" : bestPath(moveIn),
                        moveOut == null ? "" : bestPath(moveOut),
                        moveIn == null ? "PHOTO" : normalizeMediaType(moveIn.mediaType),
                        moveOut == null ? "PHOTO" : normalizeMediaType(moveOut.mediaType),
                        status
                ));
            }
        }

        int moveInCount = db.mediaDao().getAllMoveInForProperty(property.id).size();
        int moveOutCount = db.mediaDao().getAllMoveOutForProperty(property.id).size();

        textSummary.setText(
                "Property: " + property.addressLine1 + ", " + property.city + ", " + property.state + " " + property.zip
                        + "\nMove-In entries: " + moveInCount
                        + " | Move-Out entries: " + moveOutCount
                        + "\nMissing: " + missingCount + " | No Change: " + noChangeCount + " | Needs Review: " + needsReviewCount
        );

        recyclerComparison.setAdapter(new ComparisonReportAdapter(rows));
        textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private RoomItemMedia resolvePrimaryMedia(long propertyId, PropertyRoom room, RoomItem item, String mode) {
        RoomItemMedia media = db.mediaDao().getPrimaryMediaForRoomItemMode(propertyId, item.id, mode);
        if (media != null) {
            return media;
        }
        media = db.mediaDao().getPrimaryForRoomItemTextMode(propertyId, mode, room.name, item.name);
        if (media != null) {
            return media;
        }
        return db.mediaDao().getPrimaryForRoomItemTextModeFallback(propertyId, String.valueOf(propertyId), mode, room.name, item.name);
    }

    private String bestPath(RoomItemMedia media) {
        if (media == null) {
            return "";
        }
        if (media.filePath != null && !media.filePath.isEmpty()) {
            return media.filePath;
        }
        return media.mediaPath == null ? "" : media.mediaPath;
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null || mediaType.trim().isEmpty()) {
            return "PHOTO";
        }
        return mediaType.toUpperCase();
    }

    private void seedDemoPair() {
        if (property == null) {
            Toast.makeText(this, "No property available to seed", Toast.LENGTH_SHORT).show();
            return;
        }

        List<PropertyRoom> rooms = db.propertyRoomDao().getRoomsForProperty(property.id);
        if (rooms.isEmpty()) {
            Toast.makeText(this, "No generated rooms found", Toast.LENGTH_SHORT).show();
            return;
        }

        PropertyRoom room = rooms.get(0);
        List<RoomItem> items = db.roomItemDao().getItemsForRoom(room.id);
        if (items.isEmpty()) {
            Toast.makeText(this, "No generated items found", Toast.LENGTH_SHORT).show();
            return;
        }

        RoomItem item = items.get(0);
        long now = System.currentTimeMillis();

        File moveInFile = createDemoImageFile(property.id, MODE_MOVE_IN, room.id, item.id, now, "Move In");
        File moveOutFile = createDemoImageFile(property.id, MODE_MOVE_OUT, room.id, item.id, now + 1, "Move Out");

        if (moveInFile == null || moveOutFile == null) {
            Toast.makeText(this, "Failed to seed demo files", Toast.LENGTH_SHORT).show();
            return;
        }

        RoomItemMedia moveIn = new RoomItemMedia(MODE_MOVE_IN, room.name, item.name, moveInFile.getAbsolutePath(), now, "PHOTO", "WALKTHROUGH", null, 0L);
        moveIn.applyPropertyLinks(property.id, room.id, item.id);
        moveIn.propertyId = String.valueOf(property.id);
        moveIn.roomId = room.name;
        moveIn.itemId = item.name;
        moveIn.mediaPath = moveInFile.getAbsolutePath();

        RoomItemMedia moveOut = new RoomItemMedia(MODE_MOVE_OUT, room.name, item.name, moveOutFile.getAbsolutePath(), now + 1, "PHOTO", "WALKTHROUGH", null, 0L);
        moveOut.applyPropertyLinks(property.id, room.id, item.id);
        moveOut.propertyId = String.valueOf(property.id);
        moveOut.roomId = room.name;
        moveOut.itemId = item.name;
        moveOut.mediaPath = moveOutFile.getAbsolutePath();

        db.mediaDao().insert(moveIn);
        db.mediaDao().insert(moveOut);

        Toast.makeText(this, "Seeded demo move-in/out pair", Toast.LENGTH_SHORT).show();
        loadComparisonRows();
    }

    private File createDemoImageFile(long propertyId, String mode, long roomId, long itemId, long timestamp, String label) {
        try {
            File dir = new File(getFilesDir(), "captures/" + propertyId + "/" + mode);
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }

            File outFile = new File(dir, roomId + "_" + itemId + "_demo_" + timestamp + ".jpg");

            Bitmap bitmap = Bitmap.createBitmap(960, 540, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor("MOVE_IN".equals(mode) ? Color.parseColor("#DFF4FF") : Color.parseColor("#FFE7D6"));
            canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
            paint.setColor(Color.parseColor("#202020"));
            paint.setTextSize(42f);
            canvas.drawText(label + " Demo", 40, 120, paint);
            paint.setTextSize(30f);
            canvas.drawText("Room: " + roomId + " Item: " + itemId, 40, 190, paint);
            canvas.drawText("Property: " + propertyId, 40, 250, paint);

            try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            }
            bitmap.recycle();
            return outFile;
        } catch (Exception exception) {
            return null;
        }
    }
}
