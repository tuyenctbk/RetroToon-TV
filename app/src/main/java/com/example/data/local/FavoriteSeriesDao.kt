package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSeriesDao {

    @Query("SELECT * FROM favorite_series ORDER BY addedTimestamp DESC")
    fun getAllFavoriteSeries(): Flow<List<FavoriteSeriesEntity>>

    @Query("SELECT seriesId FROM favorite_series ORDER BY addedTimestamp DESC")
    fun getAllFavoriteSeriesIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_series WHERE seriesId = :seriesId)")
    fun isSeriesFavorite(seriesId: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_series WHERE seriesId = :seriesId LIMIT 1")
    suspend fun getFavoriteSeriesById(seriesId: String): FavoriteSeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteSeries(series: FavoriteSeriesEntity)

    @Query("DELETE FROM favorite_series WHERE seriesId = :seriesId")
    suspend fun deleteFavoriteSeries(seriesId: String)

    @Query("DELETE FROM favorite_series")
    suspend fun clearAllFavoriteSeries()
}
