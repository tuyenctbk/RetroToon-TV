package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CartoonSeries

@Entity(tableName = "favorite_series")
data class FavoriteSeriesEntity(
    @PrimaryKey
    val seriesId: String,
    val title: String,
    val era: String,
    val studio: String,
    val categoryKey: String,
    val description: String,
    val thumbnailUrl: String,
    val episodeCount: Int,
    val accentHex: Long,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    fun toCartoonSeries(): CartoonSeries {
        return CartoonSeries(
            id = seriesId,
            title = title,
            era = era,
            studio = studio,
            categoryKey = categoryKey,
            description = description,
            thumbnailUrl = thumbnailUrl,
            episodeCount = episodeCount,
            accentHex = accentHex,
            isFavorite = true,
            addedTimestamp = addedTimestamp
        )
    }

    companion object {
        fun fromCartoonSeries(
            series: CartoonSeries,
            timestamp: Long = System.currentTimeMillis()
        ): FavoriteSeriesEntity {
            return FavoriteSeriesEntity(
                seriesId = series.id,
                title = series.title,
                era = series.era,
                studio = series.studio,
                categoryKey = series.categoryKey,
                description = series.description,
                thumbnailUrl = series.thumbnailUrl,
                episodeCount = series.episodeCount,
                accentHex = series.accentHex,
                addedTimestamp = timestamp
            )
        }
    }
}
