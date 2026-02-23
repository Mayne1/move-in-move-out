package com.mayneline.moveinmoveout.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.mayneline.moveinmoveout.data.InspectionRunEntity;
import com.mayneline.moveinmoveout.data.PropertyEntity;
import com.mayneline.moveinmoveout.model.ComparisonResult;
import com.mayneline.moveinmoveout.model.ComparisonResultItem;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportGenerator {
    private static final int PAGE_WIDTH = 1200;
    private static final int PAGE_HEIGHT = 1800;

    public File generatePdf(
            Context context,
            PropertyEntity property,
            InspectionRunEntity moveInRun,
            InspectionRunEntity moveOutRun,
            ComparisonResult comparison
    ) throws Exception {
        File outDir = new File(context.getFilesDir(), "reports");
        if (!outDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        }

        String fileName = "report_" + property.propertyId + "_" + System.currentTimeMillis() + ".pdf";
        File outFile = new File(outDir, fileName);

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNumber = 1;

        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
        Canvas canvas = page.getCanvas();
        int y = 80;

        paint.setColor(Color.BLACK);
        paint.setTextSize(36f);
        canvas.drawText("Inspection Report", 50, y, paint);
        y += 50;

        paint.setTextSize(22f);
        drawLine(canvas, paint, "Property ID: " + property.propertyId, y); y += 32;
        drawLine(canvas, paint, "Address: " + property.address + formatUnit(property.unitNumber), y); y += 32;
        drawLine(canvas, paint, "Bedrooms/Bathrooms: " + property.bedrooms + " / " + property.bathrooms, y); y += 32;
        drawLine(canvas, paint, "Garage/Basement/Yard: " + bool(property.hasGarage) + " / " + bool(property.hasBasement) + " / " + bool(property.hasYard), y); y += 45;

        drawRunBlock(canvas, paint, "Move-In Run", moveInRun, y);
        y += 145;
        drawRunBlock(canvas, paint, "Move-Out Run", moveOutRun, y);
        y += 165;

        paint.setTextSize(24f);
        drawLine(canvas, paint, "Comparison Summary", y); y += 34;
        paint.setTextSize(20f);
        drawLine(canvas, paint, "Missing: " + comparison.getMissingCount(), y); y += 30;
        drawLine(canvas, paint, "No Change: " + comparison.getNoChangeCount(), y); y += 30;
        drawLine(canvas, paint, "Needs Review: " + comparison.getNeedsReviewCount(), y); y += 40;

        paint.setTextSize(22f);
        drawLine(canvas, paint, "Room-by-Room Details", y); y += 30;

        List<ComparisonResultItem> items = comparison.getItems();
        for (ComparisonResultItem item : items) {
            if (y > PAGE_HEIGHT - 280) {
                document.finishPage(page);
                pageNumber++;
                page = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
                canvas = page.getCanvas();
                y = 80;
                paint.setTextSize(22f);
                drawLine(canvas, paint, "Room-by-Room Details (cont.)", y);
                y += 36;
            }

            paint.setTextSize(20f);
            drawLine(canvas, paint,
                    ChecklistEngineService.displayRoomName(item.getRoomId())
                            + " | "
                            + ChecklistEngineService.displayItemName(item.getItemId())
                            + " | "
                            + item.getStatus().name(),
                    y);
            y += 26;

            y = drawImagePair(canvas, paint, item.getMoveInMediaPath(), item.getMoveOutMediaPath(), y);
            y += 20;
        }

        document.finishPage(page);

        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            document.writeTo(outputStream);
        } finally {
            document.close();
        }

        return outFile;
    }

    private int drawImagePair(Canvas canvas, Paint paint, String moveInPath, String moveOutPath, int y) {
        int imageWidth = 220;
        int imageHeight = 140;

        paint.setTextSize(18f);
        drawLine(canvas, paint, "Move-In", 70, y + 18);
        drawLine(canvas, paint, "Move-Out", 360, y + 18);

        drawImage(canvas, moveInPath, 50, y + 25, imageWidth, imageHeight);
        drawImage(canvas, moveOutPath, 340, y + 25, imageWidth, imageHeight);
        return y + imageHeight + 35;
    }

    private void drawImage(Canvas canvas, String path, int left, int top, int width, int height) {
        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.DKGRAY);
        boxPaint.setStrokeWidth(2f);

        if (path == null || path.isEmpty()) {
            canvas.drawRect(left, top, left + width, top + height, boxPaint);
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap == null) {
            canvas.drawRect(left, top, left + width, top + height, boxPaint);
            return;
        }

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
        canvas.drawBitmap(scaled, left, top, null);
        canvas.drawRect(left, top, left + width, top + height, boxPaint);
        scaled.recycle();
        bitmap.recycle();
    }

    private void drawRunBlock(Canvas canvas, Paint paint, String title, InspectionRunEntity run, int y) {
        paint.setTextSize(24f);
        drawLine(canvas, paint, title, y);
        paint.setTextSize(20f);
        if (run == null) {
            drawLine(canvas, paint, "No run available", y + 30);
            return;
        }

        drawLine(canvas, paint, "Run ID: " + run.runId, y + 30);
        drawLine(canvas, paint, "Label: " + run.runLabel + " | Finalized: " + bool(run.finalized), y + 58);
        drawLine(canvas, paint, "Finalized At: " + formatTime(run.finalizedAt), y + 86);
        drawLine(canvas, paint, "Hash: " + (run.sha256Hash == null ? "" : run.sha256Hash), y + 114);
    }

    private void drawLine(Canvas canvas, Paint paint, String text, int y) {
        canvas.drawText(text, 50, y, paint);
    }

    private void drawLine(Canvas canvas, Paint paint, String text, int x, int y) {
        canvas.drawText(text, x, y, paint);
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(timestamp));
    }

    private String bool(boolean value) {
        return value ? "YES" : "NO";
    }

    private String formatUnit(String unitNumber) {
        if (unitNumber == null || unitNumber.trim().isEmpty()) {
            return "";
        }
        return " Unit " + unitNumber.trim();
    }
}
