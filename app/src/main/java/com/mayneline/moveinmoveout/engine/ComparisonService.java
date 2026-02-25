package com.mayneline.moveinmoveout.engine;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComparisonService {
    private static final String STATUS_MEDIA_MISSING = "MEDIA_MISSING";
    private static final String STATUS_NO_CHANGE = "NO_CHANGE";
    private static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";

    private final FirebaseFirestore firestore;

    public ComparisonService() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public FirestoreComparisonData loadPropertyComparison(String propertyId, File cacheRoot) throws Exception {
        DocumentSnapshot propertyDoc = Tasks.await(firestore.collection("properties").document(propertyId).get());
        if (!propertyDoc.exists()) {
            throw new IllegalStateException("Property not found");
        }

        String address = composeAddress(propertyDoc);

        InspectionRef moveInInspection = findBestInspection(propertyId, "MOVE_IN");
        InspectionRef moveOutInspection = findBestInspection(propertyId, "MOVE_OUT");

        Map<String, MediaRef> moveInMap = moveInInspection == null
                ? Collections.emptyMap()
                : loadPrimaryPhotoMedia(propertyId, moveInInspection.inspectionId, cacheRoot);
        Map<String, MediaRef> moveOutMap = moveOutInspection == null
                ? Collections.emptyMap()
                : loadPrimaryPhotoMedia(propertyId, moveOutInspection.inspectionId, cacheRoot);

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(moveInMap.keySet());
        keys.addAll(moveOutMap.keySet());

        List<FirestoreComparisonRow> rows = new ArrayList<>();
        for (String key : keys) {
            MediaRef in = moveInMap.get(key);
            MediaRef out = moveOutMap.get(key);

            String[] split = key.split("\\|", 2);
            String roomName = split.length > 0 ? split[0] : "";
            String itemName = split.length > 1 ? split[1] : "";

            String status;
            double similarity;
            String explanation;

            if (in == null || out == null || in.localPath.isEmpty() || out.localPath.isEmpty()) {
                status = STATUS_MEDIA_MISSING;
                similarity = 0.0;
                explanation = "Missing photo evidence on one side of this checklist item.";
            } else {
                ImageDiffUtil.DiffResult diff = ImageDiffUtil.comparePhotos(in.localPath, out.localPath);
                status = STATUS_NO_CHANGE.equals(diff.label) ? STATUS_NO_CHANGE : STATUS_NEEDS_REVIEW;
                similarity = diff.similarity;
                explanation = diff.explanation;
            }

            FirestoreComparisonRow row = new FirestoreComparisonRow();
            row.roomName = roomName;
            row.itemName = itemName;
            row.moveInPath = in == null ? "" : in.localPath;
            row.moveOutPath = out == null ? "" : out.localPath;
            row.moveInCapturedAt = in == null ? "" : in.createdAt;
            row.moveOutCapturedAt = out == null ? "" : out.createdAt;
            row.moveInSha256 = in == null ? "" : in.sha256;
            row.moveOutSha256 = out == null ? "" : out.sha256;
            row.moveInFileBytes = in == null ? 0L : in.fileBytes;
            row.moveOutFileBytes = out == null ? 0L : out.fileBytes;
            row.status = status;
            row.similarityScore = similarity;
            row.explanation = explanation;
            rows.add(row);
        }

        FirestoreComparisonData data = new FirestoreComparisonData();
        data.propertyId = propertyId;
        data.propertyAddress = address;
        data.moveInInspectionId = moveInInspection == null ? "" : moveInInspection.inspectionId;
        data.moveOutInspectionId = moveOutInspection == null ? "" : moveOutInspection.inspectionId;
        data.moveInInspectionCreatedAt = moveInInspection == null ? "" : moveInInspection.createdAt;
        data.moveOutInspectionCreatedAt = moveOutInspection == null ? "" : moveOutInspection.createdAt;
        data.totalMoveInEntries = moveInMap.size();
        data.totalMoveOutEntries = moveOutMap.size();
        data.rows = rows;
        return data;
    }

    private InspectionRef findBestInspection(String propertyId, String mode) throws Exception {
        QuerySnapshot snapshot = Tasks.await(firestore.collection("properties")
                .document(propertyId)
                .collection("inspections")
                .whereEqualTo("mode", mode)
                .get());

        InspectionRef bestFinalized = null;
        InspectionRef bestAny = null;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            InspectionRef row = new InspectionRef();
            row.inspectionId = doc.getId();
            row.finalized = Boolean.TRUE.equals(doc.getBoolean("finalized"));
            Timestamp createdAt = doc.getTimestamp("createdAt");
            Timestamp finalizedAt = doc.getTimestamp("finalizedAt");
            row.createdAtEpoch = createdAt == null ? 0L : createdAt.toDate().getTime();
            row.finalizedAtEpoch = finalizedAt == null ? 0L : finalizedAt.toDate().getTime();
            row.createdAt = row.createdAtEpoch > 0 ? EvidenceFileUtil.isoTimestamp(row.createdAtEpoch) : "";

            if (bestAny == null || row.createdAtEpoch > bestAny.createdAtEpoch) {
                bestAny = row;
            }

            if (row.finalized) {
                if (bestFinalized == null || row.finalizedAtEpoch > bestFinalized.finalizedAtEpoch) {
                    bestFinalized = row;
                }
            }
        }

        return bestFinalized != null ? bestFinalized : bestAny;
    }

    private Map<String, MediaRef> loadPrimaryPhotoMedia(String propertyId, String inspectionId, File cacheRoot) throws Exception {
        QuerySnapshot snapshot = Tasks.await(firestore.collection("properties")
                .document(propertyId)
                .collection("inspections")
                .document(inspectionId)
                .collection("media")
                .whereEqualTo("type", "PHOTO")
                .get());

        Map<String, MediaRef> newestByKey = new HashMap<>();
        Map<String, MediaRef> walkthroughByKey = new HashMap<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String roomName = safe(doc.getString("roomName"));
            String itemName = safe(doc.getString("itemName"));
            if (roomName.isEmpty() || itemName.isEmpty()) {
                continue;
            }

            String key = roomName + "|" + itemName;

            MediaRef ref = new MediaRef();
            ref.mediaId = doc.getId();
            ref.roomName = roomName;
            ref.itemName = itemName;
            ref.storagePath = safe(doc.getString("storagePath"));
            ref.downloadUrl = safe(doc.getString("downloadUrl"));
            ref.sha256 = safe(doc.getString("sha256"));
            ref.note = safe(doc.getString("notes"));
            Timestamp createdAt = doc.getTimestamp("createdAt");
            ref.createdAtEpoch = createdAt == null ? 0L : createdAt.toDate().getTime();
            ref.createdAt = ref.createdAtEpoch > 0 ? EvidenceFileUtil.isoTimestamp(ref.createdAtEpoch) : "";

            Object tagsObj = doc.get("tags");
            boolean walkthrough = false;
            if (tagsObj instanceof List) {
                for (Object tag : (List<?>) tagsObj) {
                    if (tag != null && "WALKTHROUGH".equalsIgnoreCase(String.valueOf(tag))) {
                        walkthrough = true;
                        break;
                    }
                }
            }

            ref.localPath = ensureLocalPhotoCache(propertyId, inspectionId, ref, cacheRoot);
            ref.fileBytes = EvidenceFileUtil.sizeBytes(ref.localPath);
            if (ref.sha256.isEmpty() && !ref.localPath.isEmpty()) {
                ref.sha256 = HashUtils.sha256File(ref.localPath);
            }

            MediaRef newest = newestByKey.get(key);
            if (newest == null || ref.createdAtEpoch >= newest.createdAtEpoch) {
                newestByKey.put(key, ref);
            }
            if (walkthrough) {
                MediaRef currentWalkthrough = walkthroughByKey.get(key);
                if (currentWalkthrough == null || ref.createdAtEpoch >= currentWalkthrough.createdAtEpoch) {
                    walkthroughByKey.put(key, ref);
                }
            }
        }

        Map<String, MediaRef> selected = new HashMap<>();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(newestByKey.keySet());
        keys.addAll(walkthroughByKey.keySet());
        for (String key : keys) {
            selected.put(key, walkthroughByKey.containsKey(key) ? walkthroughByKey.get(key) : newestByKey.get(key));
        }
        return selected;
    }

    private String ensureLocalPhotoCache(String propertyId, String inspectionId, MediaRef ref, File cacheRoot) {
        if (cacheRoot == null) {
            return "";
        }
        File dir = new File(cacheRoot, propertyId + "/" + inspectionId);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        String extension = "jpg";
        if (ref.storagePath.endsWith(".png")) {
            extension = "png";
        }
        File outFile = new File(dir, ref.mediaId + "." + extension);
        if (outFile.exists() && outFile.length() > 0) {
            return outFile.getAbsolutePath();
        }

        try {
            if (!ref.storagePath.isEmpty()) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(ref.storagePath);
                Tasks.await(storageRef.getFile(outFile));
                return outFile.getAbsolutePath();
            }
            if (!ref.downloadUrl.isEmpty()) {
                URL url = new URL(ref.downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                java.io.FileOutputStream output = new java.io.FileOutputStream(outFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.close();
                input.close();
                return outFile.getAbsolutePath();
            }
        } catch (Exception ignored) {
            // leave empty path to avoid crashing whole report.
        }
        return "";
    }

    private String composeAddress(DocumentSnapshot propertyDoc) {
        String address = safe(propertyDoc.getString("addressLine"));
        String city = safe(propertyDoc.getString("city"));
        String state = safe(propertyDoc.getString("state"));
        String zip = safe(propertyDoc.getString("zip"));
        return (address + ", " + city + ", " + state + " " + zip).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class FirestoreComparisonData {
        public String propertyId;
        public String propertyAddress;
        public String moveInInspectionId;
        public String moveOutInspectionId;
        public String moveInInspectionCreatedAt;
        public String moveOutInspectionCreatedAt;
        public int totalMoveInEntries;
        public int totalMoveOutEntries;
        public List<FirestoreComparisonRow> rows = new ArrayList<>();
    }

    public static class FirestoreComparisonRow {
        public String roomName;
        public String itemName;
        public String moveInPath;
        public String moveOutPath;
        public String moveInCapturedAt;
        public String moveOutCapturedAt;
        public String moveInSha256;
        public String moveOutSha256;
        public long moveInFileBytes;
        public long moveOutFileBytes;
        public double similarityScore;
        public String status;
        public String explanation;
    }

    private static class InspectionRef {
        String inspectionId;
        boolean finalized;
        String createdAt;
        long createdAtEpoch;
        long finalizedAtEpoch;
    }

    private static class MediaRef {
        String mediaId;
        String roomName;
        String itemName;
        String storagePath;
        String downloadUrl;
        String sha256;
        String note;
        String localPath;
        String createdAt;
        long createdAtEpoch;
        long fileBytes;
    }
}
