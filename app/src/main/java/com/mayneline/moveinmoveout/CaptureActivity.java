package com.mayneline.moveinmoveout;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.RoomItemMedia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CaptureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        TextView textCaptureContext = findViewById(R.id.textCaptureContext);
        textCaptureContext.setText(buildContextText());

        findViewById(R.id.buttonPhoto).setOnClickListener(v ->
                captureAndStoreMedia("PHOTO"));

        findViewById(R.id.buttonVideo).setOnClickListener(v ->
                captureAndStoreMedia("VIDEO"));
    }

    private String buildContextText() {
        String mode = getIntent().getStringExtra("mode");
        String room = getIntent().getStringExtra("room");
        String item = getIntent().getStringExtra("item");
        if (isBlank(mode) || isBlank(room) || isBlank(item)) {
            return "No context provided.";
        }
        return "Mode: " + mode + "\nRoom: " + room + "\nItem: " + item;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void captureAndStoreMedia(String captureType) {
        String mode = getIntent().getStringExtra("mode");
        String room = getIntent().getStringExtra("room");
        String item = getIntent().getStringExtra("item");
        if (isBlank(mode) || isBlank(room) || isBlank(item)) {
            Toast.makeText(this, "Missing context. Cannot save media.", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        File mediaFile = new File(getOrCreateCaptureDir(),
                captureType.toLowerCase() + "_" + sanitize(mode) + "_" + sanitize(room) + "_"
                        + sanitize(item) + "_" + timestamp + ".jpg");

        try {
            writePlaceholderImage(mediaFile, mode, room, item, captureType, timestamp);
            RoomItemMedia media = new RoomItemMedia(mode, room, item, mediaFile.getAbsolutePath(), timestamp);
            AppDatabase.getInstance(this).mediaDao().insert(media);
            Toast.makeText(this, captureType + " saved", Toast.LENGTH_SHORT).show();
        } catch (IOException ioException) {
            Toast.makeText(this, "Failed to save media", Toast.LENGTH_SHORT).show();
        }
    }

    private File getOrCreateCaptureDir() {
        File capturesDir = new File(getFilesDir(), "captures");
        if (!capturesDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            capturesDir.mkdirs();
        }
        return capturesDir;
    }

    private void writePlaceholderImage(
            File outFile,
            String mode,
            String room,
            String item,
            String captureType,
            long timestamp
    ) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(960, 720, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(captureType.equals("PHOTO") ? Color.parseColor("#DFF4FF") : Color.parseColor("#FFEFD6"));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);

        paint.setColor(Color.parseColor("#1B1B1B"));
        paint.setTextSize(42f);
        canvas.drawText("Move In Move Out", 48, 100, paint);
        paint.setTextSize(36f);
        canvas.drawText("Type: " + captureType, 48, 180, paint);
        canvas.drawText("Mode: " + mode, 48, 250, paint);
        canvas.drawText("Room: " + room, 48, 320, paint);
        canvas.drawText("Item: " + item, 48, 390, paint);
        canvas.drawText("Timestamp: " + timestamp, 48, 460, paint);

        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        } finally {
            bitmap.recycle();
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
}
