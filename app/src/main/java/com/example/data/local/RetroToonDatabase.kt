package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecentlyWatchedEntity::class,
        WatchlistEntity::class,
        FavoriteSeriesEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class RetroToonDatabase : RoomDatabase() {
    abstract fun recentlyWatchedDao(): RecentlyWatchedDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun favoriteSeriesDao(): FavoriteSeriesDao

    companion object {
        @Volatile
        private var INSTANCE: RetroToonDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_series` (
                        `seriesId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `era` TEXT NOT NULL,
                        `studio` TEXT NOT NULL,
                        `categoryKey` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `thumbnailUrl` TEXT NOT NULL,
                        `episodeCount` INTEGER NOT NULL,
                        `accentHex` INTEGER NOT NULL,
                        `addedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`seriesId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): RetroToonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RetroToonDatabase::class.java,
                    "retrotoon_database.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
