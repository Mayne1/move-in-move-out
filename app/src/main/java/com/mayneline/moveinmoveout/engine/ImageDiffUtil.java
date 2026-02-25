package com.mayneline.moveinmoveout.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import java.io.FileInputStream;

public final class ImageDiffUtil {
    private static final int MAX_DIMENSION = 256;
    private static final int HASH_SIZE = 8;

    private ImageDiffUtil() {
    }

    public static DiffResult comparePhotos(String pathA, String pathB) {
        Bitmap bitmapA = null;
        Bitmap bitmapB = null;
        Bitmap normalizedA = null;
        Bitmap normalizedB = null;

        try {
            bitmapA = decodeSampledBitmap(pathA, MAX_DIMENSION);
            bitmapB = decodeSampledBitmap(pathB, MAX_DIMENSION);
            if (bitmapA == null || bitmapB == null) {
                return new DiffResult(0.0, "NEEDS_REVIEW", "Unable to decode one or both photos. Manual review required.");
            }

            normalizedA = Bitmap.createScaledBitmap(toGrayscale(bitmapA), HASH_SIZE, HASH_SIZE, true);
            normalizedB = Bitmap.createScaledBitmap(toGrayscale(bitmapB), HASH_SIZE, HASH_SIZE, true);

            long hashA = averageHash(normalizedA);
            long hashB = averageHash(normalizedB);
            int distance = Long.bitCount(hashA ^ hashB);
            double similarity = 1.0 - (distance / 64.0);
            String explanation = buildChangeExplanation(normalizedA, normalizedB, similarity);

            if (similarity >= 0.92) {
                return new DiffResult(similarity, "NO_CHANGE", explanation);
            }
            if (similarity >= 0.80) {
                return new DiffResult(similarity, "NEEDS_REVIEW", explanation);
            }
            return new DiffResult(similarity, "CHANGE_DETECTED", explanation);
        } catch (Exception ignored) {
            return new DiffResult(0.0, "NEEDS_REVIEW", "Comparison failed due to image processing error. Manual review required.");
        } finally {
            recycle(bitmapA);
            recycle(bitmapB);
            recycle(normalizedA);
            recycle(normalizedB);
        }
    }

    private static String buildChangeExplanation(Bitmap left, Bitmap right, double similarity) {
        if (left == null || right == null) {
            return "Unable to localize differences.";
        }
        if (similarity >= 0.92) {
            return "No meaningful visual differences detected between these photos.";
        }

        double[] quadrants = new double[4];
        int width = Math.min(left.getWidth(), right.getWidth());
        int height = Math.min(left.getHeight(), right.getHeight());
        int midX = width / 2;
        int midY = height / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int lv = left.getPixel(x, y) & 0xFF;
                int rv = right.getPixel(x, y) & 0xFF;
                double delta = Math.abs(lv - rv);
                int index;
                if (x < midX && y < midY) {
                    index = 0;
                } else if (x >= midX && y < midY) {
                    index = 1;
                } else if (x < midX) {
                    index = 2;
                } else {
                    index = 3;
                }
                quadrants[index] += delta;
            }
        }

        int dominant = 0;
        for (int i = 1; i < quadrants.length; i++) {
            if (quadrants[i] > quadrants[dominant]) {
                dominant = i;
            }
        }

        String area;
        if (dominant == 0) {
            area = "upper-left";
        } else if (dominant == 1) {
            area = "upper-right";
        } else if (dominant == 2) {
            area = "lower-left";
        } else {
            area = "lower-right";
        }
        return "Detected change near " + area + " area; object appears moved or removed.";
    }

    private static Bitmap decodeSampledBitmap(String path, int maxDimension) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, maxDimension, maxDimension);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) {
            return null;
        }

        int rotation = readExifRotation(path);
        if (rotation == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private static int readExifRotation(String path) {
        try (FileInputStream inputStream = new FileInputStream(path)) {
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static Bitmap toGrayscale(Bitmap source) {
        Bitmap gray = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int color = source.getPixel(x, y);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                int luminance = (r * 30 + g * 59 + b * 11) / 100;
                int grayColor = 0xFF000000 | (luminance << 16) | (luminance << 8) | luminance;
                gray.setPixel(x, y, grayColor);
            }
        }
        return gray;
    }

    private static long averageHash(Bitmap bitmap) {
        long total = 0;
        int[] values = new int[HASH_SIZE * HASH_SIZE];
        int index = 0;

        for (int y = 0; y < HASH_SIZE; y++) {
            for (int x = 0; x < HASH_SIZE; x++) {
                int pixel = bitmap.getPixel(x, y) & 0xFF;
                values[index++] = pixel;
                total += pixel;
            }
        }

        int avg = (int) (total / values.length);
        long hash = 0L;
        for (int i = 0; i < values.length; i++) {
            if (values[i] >= avg) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public static class DiffResult {
        public final double similarity;
        public final String label;
        public final String explanation;

        public DiffResult(double similarity, String label) {
            this(similarity, label, "");
        }

        public DiffResult(double similarity, String label, String explanation) {
            this.similarity = similarity;
            this.label = label;
            this.explanation = explanation == null ? "" : explanation;
        }
    }
}
