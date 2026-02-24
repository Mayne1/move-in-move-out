package com.mayneline.moveinmoveout.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                RoomItemMedia.class,
                PropertyEntity.class,
                ChecklistStructureEntity.class,
                InspectionRunEntity.class,
                PropertyProfile.class,
                PropertyRoom.class,
                RoomItem.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MediaDao mediaDao();
    public abstract PropertyDao propertyDao();
    public abstract PropertyRoomDao propertyRoomDao();
    public abstract RoomItemDao roomItemDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "move_media.db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
