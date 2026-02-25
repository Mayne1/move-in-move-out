package com.mayneline.moveinmoveout.firebase;

import com.google.firebase.Timestamp;

public class FirestoreProperty {
    public String propertyId;
    public String ownerUid;
    public String addressLine;
    public String unit;
    public String city;
    public String state;
    public String zip;
    public Timestamp createdAt;
    public Timestamp lastUpdatedAt;
}
