package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "properties")
public class PropertyEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String addressLine;

    public int bedrooms;
    public int bathrooms;
    public long createdAt;

    public PropertyEntity(@NonNull String addressLine, int bedrooms, int bathrooms, long createdAt) {
        this.addressLine = addressLine;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.createdAt = createdAt;
    }
}
