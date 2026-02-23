package com.mayneline.moveinmoveout.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "inspection_runs",
        foreignKeys = @ForeignKey(
                entity = PropertyEntity.class,
                parentColumns = "propertyId",
                childColumns = "propertyId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("propertyId")}
)
public class InspectionRunEntity {
    @PrimaryKey
    @NonNull
    public String runId;

    @NonNull
    public String propertyId;

    @NonNull
    public String mode;

    @NonNull
    public String runLabel;

    public boolean finalized;
    public long finalizedAt;
    public String sha256Hash;

    public InspectionRunEntity(
            @NonNull String runId,
            @NonNull String propertyId,
            @NonNull String mode,
            @NonNull String runLabel,
            boolean finalized,
            long finalizedAt,
            String sha256Hash
    ) {
        this.runId = runId;
        this.propertyId = propertyId;
        this.mode = mode;
        this.runLabel = runLabel;
        this.finalized = finalized;
        this.finalizedAt = finalizedAt;
        this.sha256Hash = sha256Hash;
    }
}
