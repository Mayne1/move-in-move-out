package com.mayneline.moveinmoveout.report;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;

import com.mayneline.moveinmoveout.BuildConfig;
import com.mayneline.moveinmoveout.data.PropertyProfile;
import com.mayneline.moveinmoveout.engine.EvidenceFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfReportExporter {
    private static final int PAGE_WIDTH = 1200;
    private static final int PAGE_HEIGHT = 1800;

    public File export(
            Context context,
            PropertyProfile property,
            List<ReportComparisonItem> items,
            int missingCount,
            int needsReviewCount,
            int noChangeCount,
            File outputDir
    ) throws Exception {
        if (!outputDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdirs();
        }

        File outFile = new File(outputDir, "report_" + property.id + "_" + System.currentTimeMillis() + ".pdf");

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int pageNumber = 1;
        PdfDocument.Page cover = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
        drawCoverPage(cover.getCanvas(), paint, property);
        document.finishPage(cover);

        pageNumber++;
        PdfDocument.Page summary = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
        drawSummaryPage(summary.getCanvas(), paint, missingCount, needsReviewCount, noChangeCount, items.size());
        document.finishPage(summary);

        for (ReportComparisonItem item : items) {
            pageNumber++;
            PdfDocument.Page itemPage = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
            drawItemPage(itemPage.getCanvas(), paint, item);
            document.finishPage(itemPage);
        }

        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            document.writeTo(outputStream);
        } finally {
            document.close();
        }

        return outFile;
    }

    private void drawCoverPage(Canvas canvas, Paint paint, PropertyProfile property) {
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(42f);
        canvas.drawText("Move In / Move Out Evidence Report", 50, 100, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(24f);
        int y = 170;
        canvas.drawText("Property: " + property.addressLine1 + ", " + property.city + ", " + property.state + " " + property.zip, 50, y, paint);
        y += 40;
        canvas.drawText("Comparison: MOVE_IN vs MOVE_OUT", 50, y, paint);
        y += 40;
        canvas.drawText("Generated: " + EvidenceFileUtil.isoTimestamp(System.currentTimeMillis()), 50, y, paint);
        y += 40;
        canvas.drawText("App: " + BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", 50, y, paint);
        y += 40;
        canvas.drawText("Device: " + Build.MODEL + " | Android " + Build.VERSION.RELEASE, 50, y, paint);
    }

    private void drawSummaryPage(Canvas canvas, Paint paint, int missing, int needsReview, int noChange, int total) {
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(36f);
        canvas.drawText("Summary", 50, 90, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(26f);
        int y = 160;
        canvas.drawText("Included Items: " + total, 50, y, paint);
        y += 40;
        canvas.drawText("Missing: " + missing, 50, y, paint);
        y += 40;
        canvas.drawText("Needs Review: " + needsReview, 50, y, paint);
        y += 40;
        canvas.drawText("No Change: " + noChange, 50, y, paint);
    }

    private void drawItemPage(Canvas canvas, Paint paint, ReportComparisonItem item) {
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(30f);
        canvas.drawText(item.roomName + " - " + item.itemName, 50, 90, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(22f);
        canvas.drawText("Status: " + item.status, 50, 130, paint);

        drawEvidenceBlock(canvas, paint, "Move In", item.moveInPath, item.moveInCapturedAt, item.moveInSha256, item.moveInFileBytes, 50, 180);
        drawEvidenceBlock(canvas, paint, "Move Out", item.moveOutPath, item.moveOutCapturedAt, item.moveOutSha256, item.moveOutFileBytes, 630, 180);
    }

    private void drawEvidenceBlock(Canvas canvas, Paint paint, String title, String path, String capturedAt, String sha, long bytes, int left, int top) {
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(22f);
        canvas.drawText(title, left, top, paint);

        int imageTop = top + 16;
        int width = 500;
        int height = 360;
        drawImageOrPlaceholder(canvas, paint, path, left, imageTop, width, height);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(16f);
        int y = imageTop + height + 28;
        canvas.drawText("Timestamp: " + safe(capturedAt), left, y, paint);
        y += 22;
        canvas.drawText("SHA-256: " + safe(sha), left, y, paint);
        y += 22;
        canvas.drawText("File Size: " + bytes + " bytes", left, y, paint);
    }

    private void drawImageOrPlaceholder(Canvas canvas, Paint paint, String path, int left, int top, int width, int height) {
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
            canvas.drawText("MISSING", left + 170, top + 190, paint);
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
}
