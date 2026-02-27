package com.mayneline.moveinmoveout.firebase;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";
    public static final String VERIFICATION_STATUS_PENDING = "pending";
    public static final String VERIFICATION_STATUS_VERIFIED = "verified";
    public static final String VERIFICATION_STATUS_DENIED = "denied";

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

    public static class PropertyShare {
        public String shareId;
        public String propertyId;
        public String createdByUid;
        public String tenantEmailLower;
        public String tenantUid;
        public String token;
        public String status;
    }

    public static class InspectionSummary {
        public String inspectionId;
        public String mode;
        public boolean finalized;
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
        payload.put("address", safe(input.addressLine));
        payload.put("addressLine", safe(input.addressLine));
        payload.put("unit", safe(input.unit));
        payload.put("city", safe(input.city));
        payload.put("state", safe(input.state));
        payload.put("zip", safe(input.zip));
        payload.put("tenantUids", new ArrayList<String>());
        payload.put("verificationStatus", VERIFICATION_STATUS_PENDING);
        payload.put("submittedAt", FieldValue.serverTimestamp());
        payload.put("verifiedAt", null);
        payload.put("verifiedBy", null);
        payload.put("denialReason", null);
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("lastUpdatedAt", FieldValue.serverTimestamp());

        doc.set(payload)
                .addOnSuccessListener(unused -> callback.onSuccess(doc.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void listPropertiesForUser(@NonNull String uid, @NonNull String role, @NonNull RepoCallback<List<FirestoreProperty>> callback) {
        if ("LANDLORD".equalsIgnoreCase(role)) {
            Log.d(TAG, "Loading LANDLORD properties for uid=" + uid);
            firestore.collection("properties")
                    .whereEqualTo("ownerUid", uid)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        Log.d(TAG, "LANDLORD properties result count=" + snapshot.size());
                        callback.onSuccess(mapProperties(snapshot));
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "LANDLORD properties query failed for uid=" + uid, exception);
                        callback.onError(exception);
                    });
            return;
        }

        if ("TENANT".equalsIgnoreCase(role)) {
            Log.d(TAG, "Loading TENANT properties by tenantUids for uid=" + uid);
            firestore.collection("properties")
                    .whereArrayContains("tenantUids", uid)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            Log.d(TAG, "TENANT properties result count=" + snapshot.size() + " via tenantUids");
                            callback.onSuccess(mapProperties(snapshot));
                            return;
                        }
                        Log.d(TAG, "TENANT properties empty via tenantUids. Trying shares fallback for uid=" + uid);
                        loadTenantPropertiesViaSharesFallback(uid, callback);
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "TENANT tenantUids query failed. Trying shares fallback for uid=" + uid, exception);
                        loadTenantPropertiesViaSharesFallback(uid, callback);
                    });
            return;
        }

        Log.w(TAG, "Unknown role in listPropertiesForUser: " + role);
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

    public void getLatestInspectionForMode(@NonNull String propertyId, @NonNull String mode, @NonNull RepoCallback<InspectionSummary> callback) {
        firestore.collection("properties")
                .document(propertyId)
                .collection("inspections")
                .whereEqualTo("mode", mode)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    DocumentSnapshot document = snapshot.getDocuments().get(0);
                    InspectionSummary summary = new InspectionSummary();
                    summary.inspectionId = document.getId();
                    summary.mode = mode;
                    summary.finalized = Boolean.TRUE.equals(document.getBoolean("finalized"));
                    callback.onSuccess(summary);
                })
                .addOnFailureListener(callback::onError);
    }

    public void createPropertyShare(@NonNull String propertyId, @NonNull String tenantEmail, @NonNull RepoCallback<PropertyShare> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User not signed in"));
            return;
        }

        CollectionReference shares = firestore.collection("properties")
                .document(propertyId)
                .collection("shares");
        DocumentReference shareDoc = shares.document();
        String token = UUID.randomUUID().toString().replace("-", "");
        String tenantEmailLower = safe(tenantEmail).toLowerCase();

        Map<String, Object> payload = new HashMap<>();
        payload.put("createdByUid", user.getUid());
        payload.put("tenantEmailLower", tenantEmailLower);
        payload.put("tenantUid", null);
        payload.put("token", token);
        payload.put("status", "PENDING");
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("acceptedAt", null);

        shareDoc.set(payload)
                .addOnSuccessListener(unused -> {
                    PropertyShare share = new PropertyShare();
                    share.shareId = shareDoc.getId();
                    share.propertyId = propertyId;
                    share.createdByUid = user.getUid();
                    share.tenantEmailLower = tenantEmailLower;
                    share.tenantUid = null;
                    share.token = token;
                    share.status = "PENDING";
                    callback.onSuccess(share);
                })
                .addOnFailureListener(callback::onError);
    }

    public void acceptShareByToken(@NonNull String token, @NonNull RepoCallback<PropertyShare> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User not signed in"));
            return;
        }
        String currentEmailLower = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();

        firestore.collectionGroup("shares")
                .whereEqualTo("token", token)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onError(new IllegalStateException("Invite not found"));
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String status = doc.getString("status");
                    if ("REVOKED".equalsIgnoreCase(status)) {
                        callback.onError(new IllegalStateException("Invite has been revoked"));
                        return;
                    }
                    String tenantEmailLower = doc.getString("tenantEmailLower");
                    if (tenantEmailLower == null || !tenantEmailLower.equals(currentEmailLower)) {
                        callback.onError(new IllegalStateException("Invite email does not match signed-in user"));
                        return;
                    }

                    DocumentReference ref = doc.getReference();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("tenantUid", user.getUid());
                    updates.put("status", "ACCEPTED");
                    updates.put("acceptedAt", FieldValue.serverTimestamp());
                    DocumentReference propertyRef = ref.getParent().getParent();
                    WriteBatch batch = firestore.batch();
                    batch.update(ref, updates);
                    if (propertyRef != null) {
                        batch.update(propertyRef, "tenantUids", FieldValue.arrayUnion(user.getUid()));
                    } else {
                        Log.w(TAG, "acceptShareByToken: propertyRef was null for shareId=" + ref.getId());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                PropertyShare share = mapShare(doc);
                                share.tenantUid = user.getUid();
                                share.status = "ACCEPTED";
                                callback.onSuccess(share);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getShareByToken(@NonNull String token, @NonNull RepoCallback<PropertyShare> callback) {
        firestore.collectionGroup("shares")
                .whereEqualTo("token", token)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onError(new IllegalStateException("Invite not found"));
                        return;
                    }
                    callback.onSuccess(mapShare(snapshot.getDocuments().get(0)));
                })
                .addOnFailureListener(callback::onError);
    }

    public void backfillTenantUidsForCurrentLandlord(@NonNull RepoCallback<Integer> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User not signed in"));
            return;
        }
        String ownerUid = user.getUid();

        firestore.collection("properties")
                .whereEqualTo("ownerUid", ownerUid)
                .get()
                .addOnSuccessListener(propertiesSnapshot -> {
                    if (propertiesSnapshot.isEmpty()) {
                        Log.d(TAG, "Backfill: no landlord properties for uid=" + ownerUid);
                        callback.onSuccess(0);
                        return;
                    }

                    List<Task<QuerySnapshot>> shareTasks = new ArrayList<>();
                    List<DocumentReference> propertyRefs = new ArrayList<>();
                    for (DocumentSnapshot propertyDoc : propertiesSnapshot.getDocuments()) {
                        DocumentReference propertyRef = propertyDoc.getReference();
                        propertyRefs.add(propertyRef);
                        shareTasks.add(propertyRef.collection("shares")
                                .whereEqualTo("status", "ACCEPTED")
                                .get());
                    }

                    Tasks.whenAllSuccess(shareTasks)
                            .addOnSuccessListener(results -> {
                                WriteBatch batch = firestore.batch();
                                int updates = 0;

                                for (int i = 0; i < results.size(); i++) {
                                    Object result = results.get(i);
                                    if (!(result instanceof QuerySnapshot)) {
                                        continue;
                                    }
                                    QuerySnapshot shares = (QuerySnapshot) result;
                                    List<String> tenantIds = new ArrayList<>();
                                    for (DocumentSnapshot shareDoc : shares.getDocuments()) {
                                        String tenantUid = shareDoc.getString("tenantUid");
                                        if (tenantUid != null && !tenantUid.trim().isEmpty()) {
                                            tenantIds.add(tenantUid.trim());
                                        }
                                    }
                                    if (!tenantIds.isEmpty()) {
                                        batch.update(propertyRefs.get(i), "tenantUids", FieldValue.arrayUnion(tenantIds.toArray()));
                                        updates++;
                                    }
                                }

                                if (updates == 0) {
                                    Log.d(TAG, "Backfill: no tenantUids updates needed for ownerUid=" + ownerUid);
                                    callback.onSuccess(0);
                                    return;
                                }

                                batch.commit()
                                        .addOnSuccessListener(unused -> {
                                            Log.d(TAG, "Backfill: tenantUids updated for properties=" + updates + ", ownerUid=" + ownerUid);
                                            callback.onSuccess(updates);
                                        })
                                        .addOnFailureListener(callback::onError);
                            })
                            .addOnFailureListener(callback::onError);
                })
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

    private void loadAcceptedPropertiesFromShares(QuerySnapshot snapshot, RepoCallback<List<FirestoreProperty>> callback) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (DocumentSnapshot shareDoc : snapshot.getDocuments()) {
            String status = shareDoc.getString("status");
            if (!"ACCEPTED".equalsIgnoreCase(status)) {
                continue;
            }
            DocumentReference propertyRef = shareDoc.getReference().getParent().getParent();
            if (propertyRef != null) {
                tasks.add(propertyRef.get());
            }
        }

        if (tasks.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    Map<String, FirestoreProperty> unique = new LinkedHashMap<>();
                    for (Object result : results) {
                        if (!(result instanceof DocumentSnapshot)) {
                            continue;
                        }
                        DocumentSnapshot propertyDoc = (DocumentSnapshot) result;
                        if (!propertyDoc.exists()) {
                            continue;
                        }
                        FirestoreProperty row = new FirestoreProperty();
                        row.propertyId = propertyDoc.getId();
                        row.ownerUid = propertyDoc.getString("ownerUid");
                        row.addressLine = propertyDoc.getString("addressLine");
                        row.unit = propertyDoc.getString("unit");
                        row.city = propertyDoc.getString("city");
                        row.state = propertyDoc.getString("state");
                        row.zip = propertyDoc.getString("zip");
                        row.createdAt = propertyDoc.getTimestamp("createdAt");
                        row.lastUpdatedAt = propertyDoc.getTimestamp("lastUpdatedAt");
                        unique.put(row.propertyId, row);
                    }
                    callback.onSuccess(new ArrayList<>(unique.values()));
                })
                .addOnFailureListener(callback::onError);
    }

    private void loadTenantPropertiesViaSharesFallback(@NonNull String uid, @NonNull RepoCallback<List<FirestoreProperty>> callback) {
        firestore.collectionGroup("shares")
                .whereEqualTo("tenantUid", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d(TAG, "TENANT shares fallback: no accepted share rows for uid=" + uid);
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    Log.d(TAG, "TENANT shares fallback rows=" + snapshot.size() + " for uid=" + uid);
                    loadAcceptedPropertiesFromShares(snapshot, callback);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "TENANT shares fallback failed for uid=" + uid + ". Returning empty list.", exception);
                    callback.onSuccess(new ArrayList<>());
                });
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

    private PropertyShare mapShare(DocumentSnapshot document) {
        PropertyShare share = new PropertyShare();
        share.shareId = document.getId();
        DocumentReference propertyRef = document.getReference().getParent().getParent();
        share.propertyId = propertyRef == null ? "" : propertyRef.getId();
        share.createdByUid = document.getString("createdByUid");
        share.tenantEmailLower = document.getString("tenantEmailLower");
        share.tenantUid = document.getString("tenantUid");
        share.token = document.getString("token");
        share.status = document.getString("status");
        return share;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
