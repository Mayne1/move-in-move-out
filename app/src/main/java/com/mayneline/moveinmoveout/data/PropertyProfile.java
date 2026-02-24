package com.mayneline.moveinmoveout.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "property_profiles")
public class PropertyProfile {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String addressLine1;
    public String city;
    public String state;
    public String zip;
    public int bedrooms;
    public String bathrooms;
    public boolean hasGarage;
    public String garageType;
    public boolean hasYard;
    public boolean hasSprinklers;
    public long createdAt;

    public PropertyProfile(
            String addressLine1,
            String city,
            String state,
            String zip,
            int bedrooms,
            String bathrooms,
            boolean hasGarage,
            String garageType,
            boolean hasYard,
            boolean hasSprinklers,
            long createdAt
    ) {
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.hasGarage = hasGarage;
        this.garageType = garageType;
        this.hasYard = hasYard;
        this.hasSprinklers = hasSprinklers;
        this.createdAt = createdAt;
    }
}
