package com.mayneline.moveinmoveout.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MediaDao {
    @Insert
    long insert(RoomItemMedia media);

    @Insert
    long insertProperty(PropertyEntity property);

    @Insert
    void insertChecklistItems(List<ChecklistItemEntity> items);

    @Query("SELECT * FROM properties ORDER BY createdAt DESC LIMIT 1")
    PropertyEntity getLatestProperty();

    @Query("SELECT * FROM checklist_items WHERE propertyId = :propertyId ORDER BY sortOrder ASC")
    List<ChecklistItemEntity> getChecklistItemsForProperty(long propertyId);

    @Query("SELECT * FROM checklist_items WHERE id = :checklistItemId LIMIT 1")
    ChecklistItemEntity getChecklistItemById(long checklistItemId);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_IN' AND room = :room AND item = :item ORDER BY timestamp DESC")
    List<RoomItemMedia> getMoveInMedia(String room, String item);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_OUT' AND room = :room AND item = :item ORDER BY timestamp DESC")
    List<RoomItemMedia> getMoveOutMedia(String room, String item);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_IN' ORDER BY room, item, timestamp DESC")
    List<RoomItemMedia> getAllMoveIn();

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_OUT' ORDER BY room, item, timestamp DESC")
    List<RoomItemMedia> getAllMoveOut();

    @Query("SELECT * FROM room_item_media WHERE mode = :mode AND propertyId = :propertyId AND checklistItemId = :checklistItemId ORDER BY timestamp DESC")
    List<RoomItemMedia> getMediaForChecklistItem(String mode, long propertyId, long checklistItemId);

    @Query("SELECT * FROM room_item_media WHERE mode = :mode AND propertyId = :propertyId AND checklistItemId = :checklistItemId ORDER BY timestamp DESC LIMIT 1")
    RoomItemMedia getLatestMediaForChecklistItem(String mode, long propertyId, long checklistItemId);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_IN' AND propertyId = :propertyId ORDER BY checklistItemId, timestamp DESC")
    List<RoomItemMedia> getAllMoveInForProperty(long propertyId);

    @Query("SELECT * FROM room_item_media WHERE mode = 'MOVE_OUT' AND propertyId = :propertyId ORDER BY checklistItemId, timestamp DESC")
    List<RoomItemMedia> getAllMoveOutForProperty(long propertyId);
}
