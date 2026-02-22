package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "checklist_items",
        foreignKeys = @ForeignKey(
                entity = PropertyEntity.class,
                parentColumns = "id",
                childColumns = "propertyId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("propertyId")}
)
public class ChecklistItemEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long propertyId;

    @NonNull
    public String roomName;

    @NonNull
    public String itemName;

    public int sortOrder;

    public ChecklistItemEntity(long propertyId, @NonNull String roomName, @NonNull String itemName, int sortOrder) {
        this.propertyId = propertyId;
        this.roomName = roomName;
        this.itemName = itemName;
        this.sortOrder = sortOrder;
    }
}
