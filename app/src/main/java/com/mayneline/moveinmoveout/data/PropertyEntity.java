package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "properties")
public class PropertyEntity {
    @PrimaryKey
    @NonNull
    public String propertyId;

    @NonNull
    public String address;

    public String unitNumber;
    public int bedrooms;
    public int bathrooms;
    public boolean hasGarage;
    public boolean hasBasement;
    public boolean hasYard;
    public long createdAt;

    public PropertyEntity(
            @NonNull String propertyId,
            @NonNull String address,
            String unitNumber,
            int bedrooms,
            int bathrooms,
            boolean hasGarage,
            boolean hasBasement,
            boolean hasYard,
            long createdAt
    ) {
        this.propertyId = propertyId;
        this.address = address;
        this.unitNumber = unitNumber;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.hasGarage = hasGarage;
        this.hasBasement = hasBasement;
        this.hasYard = hasYard;
        this.createdAt = createdAt;
    }
}
