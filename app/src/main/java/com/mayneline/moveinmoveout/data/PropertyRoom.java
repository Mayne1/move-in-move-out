package com.mayneline.moveinmoveout.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "property_rooms", indices = {@Index("propertyId")})
public class PropertyRoom {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long propertyId;
    public String name;
    public long createdAt;

    public PropertyRoom(long propertyId, String name, long createdAt) {
        this.propertyId = propertyId;
        this.name = name;
        this.createdAt = createdAt;
    }
}
