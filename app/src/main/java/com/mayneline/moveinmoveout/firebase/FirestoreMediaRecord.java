package com.mayneline.moveinmoveout.firebase;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class FirestoreMediaRecord {
    public String mediaId;
    public String roomName;
    public String itemName;
    public String type;
    public String storagePath;
    public String downloadUrl;
    public String sha256;
    public Timestamp createdAt;
    public String notes;
    public List<String> tags = new ArrayList<>();
}
