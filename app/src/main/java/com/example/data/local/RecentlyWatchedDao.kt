package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyWatchedDao {
    @Query("SELECT * FROM recently_watched ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getRecentlyWatched(): Flow<List<RecentlyWatchedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecent(item: RecentlyWatchedEntity)

    @Query("UPDATE recently_watched SET watchProgressMs = :progressMs, lastWatchedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, progressMs: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM recently_watched WHERE id = :id LIMIT 1")
    suspend fun getRecentById(id: String): RecentlyWatchedEntity?

    @Query("DELETE FROM recently_watched WHERE id = :id")
    suspend fun deleteRecent(id: String)

    @Query("DELETE FROM recently_watched")
    suspend fun clearAllRecent()
}
