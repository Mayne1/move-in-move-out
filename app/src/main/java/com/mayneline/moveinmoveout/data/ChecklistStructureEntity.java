package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "checklist_structure",
        foreignKeys = @ForeignKey(
                entity = PropertyEntity.class,
                parentColumns = "propertyId",
                childColumns = "propertyId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("propertyId")}
)
public class ChecklistStructureEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String propertyId;

    @NonNull
    public String roomId;

    @NonNull
    public String itemId;

    public int sortOrder;

    public ChecklistStructureEntity(@NonNull String propertyId, @NonNull String roomId, @NonNull String itemId, int sortOrder) {
        this.propertyId = propertyId;
        this.roomId = roomId;
        this.itemId = itemId;
        this.sortOrder = sortOrder;
    }
}
