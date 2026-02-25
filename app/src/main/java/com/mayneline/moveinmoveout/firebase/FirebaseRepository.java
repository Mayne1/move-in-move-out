package com.mayneline.moveinmoveout.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public FirebaseRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public interface RepoCallback<T> {
        void onSuccess(T result);

        void onError(@NonNull Exception exception);
    }

    public static class PropertyInput {
        public String addressLine;
        public String unit;
        public String city;
        public String state;
        public String zip;
    }

    public static class MediaInput {
        public String roomName;
        public String itemName;
        public String type;
        public String storagePath;
        public String downloadUrl;
        public String sha256;
        public String notes;
        public List<String> tags;
    }

    public void createProperty(@NonNull PropertyInput input, @NonNull RepoCallback<String> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User not signed in"));
            return;
        }

        DocumentReference doc = firestore.collection("properties").document();
        Map<String, Object> payload = new HashMap<>();
        payload.put("ownerUid", user.getUid());
        payload.put("addressLine", safe(input.addressLine));
        payload.put("unit", safe(input.unit));
        payload.put("city", safe(input.city));
        payload.put("state", safe(input.state));
        payload.put("zip", safe(input.zip));
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("lastUpdatedAt", FieldValue.serverTimestamp());

        doc.set(payload)
                .addOnSuccessListener(unused -> callback.onSuccess(doc.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void listPropertiesForUser(@NonNull String uid, @NonNull String role, @NonNull RepoCallback<List<FirestoreProperty>> callback) {
        if ("LANDLORD".equalsIgnoreCase(role)) {
            firestore.collection("properties")
                    .whereEqualTo("ownerUid", uid)
                    .get()
                    .addOnSuccessListener(snapshot -> callback.onSuccess(mapProperties(snapshot)))
                    .addOnFailureListener(callback::onError);
            return;
        }

        // Tenant property sharing via invites will be added in a follow-up.
        callback.onSuccess(new ArrayList<>());
    }

    public void createInspection(@NonNull String propertyId, @NonNull String mode, @NonNull RepoCallback<String> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User not signed in"));
            return;
        }

        DocumentReference doc = firestore.collection("properties")
                .document(propertyId)
                .collection("inspections")
                .document();

        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", mode);
        payload.put("createdByUid", user.getUid());
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("finalized", false);
        payload.put("finalizedAt", null);

        doc.set(payload)
                .addOnSuccessListener(unused -> callback.onSuccess(doc.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void addMediaRecord(@NonNull String propertyId, @NonNull String inspectionId, @NonNull MediaInput mediaData, @NonNull RepoCallback<String> callback) {
        DocumentReference doc = firestore.collection("properties")
                .document(propertyId)
                .collection("inspections")
                .document(inspectionId)
                .collection("media")
                .document();

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomName", safe(mediaData.roomName));
        payload.put("itemName", safe(mediaData.itemName));
        payload.put("type", safe(mediaData.type));
        payload.put("storagePath", safe(mediaData.storagePath));
        payload.put("downloadUrl", safe(mediaData.downloadUrl));
        payload.put("sha256", safe(mediaData.sha256));
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("notes", safe(mediaData.notes));
        payload.put("tags", mediaData.tags == null ? new ArrayList<>() : mediaData.tags);

        doc.set(payload)
                .addOnSuccessListener(unused -> callback.onSuccess(doc.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void listInspectionMedia(@NonNull String propertyId, @NonNull String inspectionId, @NonNull RepoCallback<List<FirestoreMediaRecord>> callback) {
        firestore.collection("properties")
                .document(propertyId)
                .collection("inspections")
                .document(inspectionId)
                .collection("media")
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapMedia(snapshot)))
                .addOnFailureListener(callback::onError);
    }

    private List<FirestoreProperty> mapProperties(QuerySnapshot snapshot) {
        List<FirestoreProperty> rows = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            FirestoreProperty row = new FirestoreProperty();
            row.propertyId = document.getId();
            row.ownerUid = document.getString("ownerUid");
            row.addressLine = document.getString("addressLine");
            row.unit = document.getString("unit");
            row.city = document.getString("city");
            row.state = document.getString("state");
            row.zip = document.getString("zip");
            row.createdAt = document.getTimestamp("createdAt");
            row.lastUpdatedAt = document.getTimestamp("lastUpdatedAt");
            rows.add(row);
        }
        return rows;
    }

    private List<FirestoreMediaRecord> mapMedia(QuerySnapshot snapshot) {
        List<FirestoreMediaRecord> rows = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            FirestoreMediaRecord row = new FirestoreMediaRecord();
            row.mediaId = document.getId();
            row.roomName = document.getString("roomName");
            row.itemName = document.getString("itemName");
            row.type = document.getString("type");
            row.storagePath = document.getString("storagePath");
            row.downloadUrl = document.getString("downloadUrl");
            row.sha256 = document.getString("sha256");
            row.createdAt = document.getTimestamp("createdAt");
            row.notes = document.getString("notes");
            Object tags = document.get("tags");
            if (tags instanceof List) {
                for (Object tag : (List<?>) tags) {
                    if (tag != null) {
                        row.tags.add(String.valueOf(tag));
                    }
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
