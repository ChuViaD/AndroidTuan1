package com.example.tuan1android.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ConversionHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ConversionHistoryDao conversionHistoryDao();
}
