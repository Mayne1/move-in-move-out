package com.mayneline.moveinmoveout.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MediaDao {
    @Insert
    long insert(RoomItemMedia media);

    @Update
    void updateMedia(RoomItemMedia media);

    @Insert
    void insertProperty(PropertyEntity property);

    @Query("SELECT * FROM properties WHERE propertyId = :propertyId LIMIT 1")
    PropertyEntity getPropertyById(String propertyId);

    @Query("SELECT * FROM properties ORDER BY createdAt DESC LIMIT 1")
    PropertyEntity getLatestProperty();

    @Insert
    void insertChecklistStructure(List<ChecklistStructureEntity> rows);

    @Query("DELETE FROM checklist_structure WHERE propertyId = :propertyId")
    void deleteChecklistStructureForProperty(String propertyId);

    @Query("SELECT * FROM checklist_structure WHERE propertyId = :propertyId ORDER BY sortOrder ASC")
    List<ChecklistStructureEntity> getChecklistStructureForProperty(String propertyId);

    @Query("SELECT COUNT(*) FROM checklist_structure WHERE propertyId = :propertyId")
    int countChecklistStructureForProperty(String propertyId);

    @Insert
    void insertRun(InspectionRunEntity run);

    @Update
    void updateRun(InspectionRunEntity run);

    @Query("SELECT * FROM inspection_runs WHERE runId = :runId LIMIT 1")
    InspectionRunEntity getRunById(String runId);

    @Query("SELECT * FROM inspection_runs WHERE propertyId = :propertyId AND mode = :mode AND finalized = 0 ORDER BY runLabel DESC LIMIT 1")
    InspectionRunEntity getActiveRun(String propertyId, String mode);

    @Query("SELECT * FROM inspection_runs WHERE propertyId = :propertyId AND mode = :mode ORDER BY runLabel DESC")
    List<InspectionRunEntity> getRunsForPropertyMode(String propertyId, String mode);

    @Query("SELECT * FROM inspection_runs WHERE propertyId = :propertyId AND mode = :mode AND finalized = 1 ORDER BY finalizedAt DESC LIMIT 1")
    InspectionRunEntity getLatestFinalizedRun(String propertyId, String mode);

    @Insert
    long insertMedia(RoomItemMedia media);

    @Query("SELECT * FROM room_item_media WHERE runId = :runId ORDER BY timestamp ASC")
    List<RoomItemMedia> getMediaForRun(String runId);

    @Query("SELECT * FROM room_item_media WHERE runId = :runId AND roomId = :roomId AND itemId = :itemId ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getLatestMediaForRunItem(String runId, String roomId, String itemId);

    @Query("SELECT COUNT(DISTINCT roomId || '|' || itemId) FROM room_item_media WHERE runId = :runId")
    int countCompletedChecklistItemsForRun(String runId);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND mode = 'MOVE_IN' ORDER BY timestamp DESC")
    List<RoomItemMedia> getAllMoveInForProperty(long propertyId);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND mode = 'MOVE_OUT' ORDER BY timestamp DESC")
    List<RoomItemMedia> getAllMoveOutForProperty(long propertyId);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND roomItemId = :roomItemId AND mode = :mode ORDER BY timestamp ASC")
    List<RoomItemMedia> getAllMediaForRoomItemMode(long propertyId, long roomItemId, String mode);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND roomItemId = :roomItemId AND mode = :mode ORDER BY CASE WHEN tag = 'WALKTHROUGH' THEN 0 ELSE 1 END ASC, timestamp DESC LIMIT 1")
    RoomItemMedia getPrimaryMediaForRoomItemMode(long propertyId, long roomItemId, String mode);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND roomItemId = :roomItemId AND mode = :mode ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getLatestMediaForRoomItemMode(long propertyId, long roomItemId, String mode);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND propertyRoomId = :propertyRoomId AND roomItemId = :roomItemId AND mode = :mode ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getLatestForPropertyRoomItemMode(long propertyId, long propertyRoomId, long roomItemId, String mode);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND mode = :mode AND room = :roomName AND item = :itemName ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getLatestForRoomItemTextMode(long propertyId, String mode, String roomName, String itemName);

    @Query("SELECT * FROM room_item_media WHERE propertyProfileId = :propertyId AND mode = :mode AND room = :roomName AND item = :itemName ORDER BY CASE WHEN tag = 'WALKTHROUGH' THEN 0 ELSE 1 END ASC, timestamp DESC LIMIT 1")
    RoomItemMedia getPrimaryForRoomItemTextMode(long propertyId, String mode, String roomName, String itemName);

    @Query("SELECT * FROM room_item_media WHERE mode = :mode AND room = :roomName AND item = :itemName AND (propertyProfileId = :propertyId OR propertyProfileId IS NULL OR propertyId = :propertyIdText) ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getLatestForRoomItemTextModeFallback(long propertyId, String propertyIdText, String mode, String roomName, String itemName);

    @Query("SELECT * FROM room_item_media WHERE mode = :mode AND room = :roomName AND item = :itemName AND (propertyProfileId = :propertyId OR propertyProfileId IS NULL OR propertyId = :propertyIdText) ORDER BY CASE WHEN tag = 'WALKTHROUGH' THEN 0 ELSE 1 END ASC, timestamp DESC LIMIT 1")
    RoomItemMedia getPrimaryForRoomItemTextModeFallback(long propertyId, String propertyIdText, String mode, String roomName, String itemName);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_IN' AND room = :room AND item = :item ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getMoveInMedia(String room, String item);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_OUT' AND room = :room AND item = :item ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getMoveOutMedia(String room, String item);
}
