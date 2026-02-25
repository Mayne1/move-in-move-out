package com.mayneline.moveinmoveout.firebase;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.RoomItemMedia;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaSyncWorker extends Worker {
    public MediaSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<RoomItemMedia> pending = db.mediaDao().getPendingUploads();
        if (pending == null || pending.isEmpty()) {
            return Result.success();
        }

        boolean hasRetryableError = false;
        for (RoomItemMedia media : pending) {
            try {
                if (media.firestorePropertyId == null || media.firestorePropertyId.trim().isEmpty()
                        || media.firestoreInspectionId == null || media.firestoreInspectionId.trim().isEmpty()) {
                    media.pendingUpload = 0;
                    media.uploadError = "Missing session mapping";
                    db.mediaDao().updateMedia(media);
                    continue;
                }

                String localPath = media.filePath == null || media.filePath.isEmpty() ? media.mediaPath : media.filePath;
                if (localPath == null || localPath.trim().isEmpty()) {
                    media.pendingUpload = 0;
                    media.uploadError = "Missing local file path";
                    db.mediaDao().updateMedia(media);
                    continue;
                }

                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    media.pendingUpload = 0;
                    media.uploadError = "File not found";
                    db.mediaDao().updateMedia(media);
                    continue;
                }

                String storagePath = buildStoragePath(media, localFile.getName());
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(storagePath);
                Tasks.await(storageReference.putFile(Uri.fromFile(localFile)));
                String downloadUrl = Tasks.await(storageReference.getDownloadUrl()).toString();

                Map<String, Object> payload = new HashMap<>();
                payload.put("roomName", safe(media.room));
                payload.put("itemName", safe(media.item));
                payload.put("type", media.mediaType == null || media.mediaType.isEmpty() ? "PHOTO" : media.mediaType);
                payload.put("storagePath", storagePath);
                payload.put("downloadUrl", downloadUrl);
                payload.put("sha256", safe(media.sha256));
                payload.put("createdAt", FieldValue.serverTimestamp());
                payload.put("notes", safe(media.note));
                List<String> tags = new ArrayList<>();
                if (media.tag != null && !media.tag.trim().isEmpty()) {
                    tags.add(media.tag);
                }
                payload.put("tags", tags);

                Tasks.await(FirebaseFirestore.getInstance()
                        .collection("properties")
                        .document(media.firestorePropertyId)
                        .collection("inspections")
                        .document(media.firestoreInspectionId)
                        .collection("media")
                        .add(payload));

                media.pendingUpload = 0;
                media.firestoreStoragePath = storagePath;
                media.firestoreDownloadUrl = downloadUrl;
                media.uploadError = null;
                db.mediaDao().updateMedia(media);
            } catch (Exception exception) {
                hasRetryableError = true;
                media.pendingUpload = 1;
                media.uploadError = exception.getMessage();
                db.mediaDao().updateMedia(media);
            }
        }

        return hasRetryableError ? Result.retry() : Result.success();
    }

    private String buildStoragePath(RoomItemMedia media, String fileName) {
        String mode = media.mode == null || media.mode.isEmpty() ? "MOVE_IN" : media.mode;
        return "captures/"
                + media.firestorePropertyId + "/"
                + media.firestoreInspectionId + "/"
                + mode + "/"
                + media.id + "_" + fileName;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
