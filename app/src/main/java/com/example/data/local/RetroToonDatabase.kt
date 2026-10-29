package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentlyWatchedEntity::class, WatchlistEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RetroToonDatabase : RoomDatabase() {
    abstract fun recentlyWatchedDao(): RecentlyWatchedDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: RetroToonDatabase? = null

        fun getInstance(context: Context): RetroToonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RetroToonDatabase::class.java,
                    "retrotoon_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
