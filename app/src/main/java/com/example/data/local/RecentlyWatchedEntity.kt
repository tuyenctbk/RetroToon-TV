package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CartoonItem

@Entity(tableName = "recently_watched")
data class RecentlyWatchedEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val year: String = "1940s",
    val category: String = "Classic",
    val creator: String = "Public Domain Archive",
    val description: String = "",
    val runtime: String = "8m",
    val downloads: Long = 0L,
    val thumbnailUrl: String = "",
    val videoStreamUrl: String? = null,
    val mp4Filename: String? = null,
    val watchProgressMs: Long = 0L,
    val durationMs: Long = 0L,
    val collectionTag: String = "classic_cartoons",
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    fun toCartoonItem(isFavorite: Boolean = false): CartoonItem {
        return CartoonItem(
            id = id,
            title = title,
            year = year,
            category = category,
            creator = creator,
            description = description,
            runtime = runtime,
            downloads = downloads,
            thumbnailUrl = if (thumbnailUrl.isNotBlank()) thumbnailUrl else "https://archive.org/services/img/$id",
            videoStreamUrl = videoStreamUrl,
            mp4Filename = mp4Filename,
            isFavorite = isFavorite,
            watchProgressMs = watchProgressMs,
            durationMs = durationMs,
            collectionTag = collectionTag
        )
    }

    companion object {
        fun fromCartoonItem(
            item: CartoonItem,
            progressMs: Long = item.watchProgressMs,
            durationMs: Long = item.durationMs,
            timestamp: Long = System.currentTimeMillis()
        ): RecentlyWatchedEntity {
            return RecentlyWatchedEntity(
                id = item.id,
                title = item.title,
                year = item.year,
                category = item.category,
                creator = item.creator,
                description = item.description,
                runtime = item.runtime,
                downloads = item.downloads,
                thumbnailUrl = item.thumbnailUrl,
                videoStreamUrl = item.videoStreamUrl,
                mp4Filename = item.mp4Filename,
                watchProgressMs = progressMs,
                durationMs = durationMs,
                collectionTag = item.collectionTag,
                lastWatchedTimestamp = timestamp
            )
        }
    }
}
