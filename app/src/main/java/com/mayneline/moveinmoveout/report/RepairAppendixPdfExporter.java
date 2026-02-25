package com.mayneline.moveinmoveout.report;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;

import java.io.File;
import java.io.FileOutputStream;

public class RepairAppendixPdfExporter {
    private static final int PAGE_WIDTH = 1200;
    private static final int PAGE_HEIGHT = 1800;

    public File export(
            Context context,
            String propertyAddress,
            String roomName,
            String itemName,
            TimelineEntry moveIn,
            TimelineEntry repair,
            TimelineEntry moveOut,
            File outputDir
    ) throws Exception {
        if (!outputDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdirs();
        }

        String fileName = sanitize(propertyAddress + "_" + roomName + "_" + itemName)
                + "_repair_appendix_" + System.currentTimeMillis() + ".pdf";
        File outFile = new File(outputDir, fileName);

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create());
        Canvas canvas = page.getCanvas();

        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(36f);
        canvas.drawText("Repair Appendix", 50, 90, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(22f);
        canvas.drawText("Property: " + safe(propertyAddress), 50, 130, paint);
        canvas.drawText("Room/Item: " + safe(roomName) + " / " + safe(itemName), 50, 160, paint);
        canvas.drawText("Generated: " + EvidenceFileUtil.isoTimestamp(System.currentTimeMillis()), 50, 190, paint);

        drawEntry(canvas, paint, "Move In", moveIn, 50, 240);
        drawEntry(canvas, paint, "Repair", repair, 50, 760);
        drawEntry(canvas, paint, "Move Out", moveOut, 50, 1280);

        document.finishPage(page);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return outFile;
    }

    private void drawEntry(Canvas canvas, Paint paint, String title, TimelineEntry entry, int left, int top) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(24f);
        paint.setColor(Color.BLACK);
        canvas.drawText(title, left, top, paint);

        int imageTop = top + 16;
        int width = 700;
        int height = 300;
        drawImage(canvas, paint, entry == null ? "" : entry.path, left, imageTop, width, height);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(18f);
        int textY = imageTop + height + 24;
        canvas.drawText("Timestamp: " + (entry == null ? "-" : safe(entry.timestampIso)), left, textY, paint);
        textY += 24;
        canvas.drawText("Note: " + (entry == null ? "-" : safe(entry.note)), left, textY, paint);
    }

    private void drawImage(Canvas canvas, Paint paint, String path, int left, int top, int width, int height) {
        Bitmap bitmap = null;
        if (path != null && !path.isEmpty()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeFile(path, options);
        }

        if (bitmap == null) {
            paint.setColor(Color.LTGRAY);
            canvas.drawRect(left, top, left + width, top + height, paint);
            paint.setColor(Color.DKGRAY);
            paint.setTextSize(28f);
            canvas.drawText("NO EVIDENCE", left + 220, top + 160, paint);
            return;
        }

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
        canvas.drawBitmap(scaled, left, top, null);
        scaled.recycle();
        bitmap.recycle();
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private String sanitize(String value) {
        return (value == null ? "appendix" : value).replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public static class TimelineEntry {
        public String path;
        public String timestampIso;
        public String note;
    }
}
