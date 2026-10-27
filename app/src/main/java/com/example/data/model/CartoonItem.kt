package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class CartoonItem(
    val id: String,
    val title: String,
    val year: String = "1940s",
    val category: String = "Classic",
    val creator: String = "Public Domain Archive",
    val description: String = "",
    val runtime: String = "8m",
    val downloads: Long = 0L,
    val thumbnailUrl: String = "https://archive.org/services/img/$id",
    val videoStreamUrl: String? = null,
    val mp4Filename: String? = null,
    val isFavorite: Boolean = false,
    val watchProgressMs: Long = 0L,
    val durationMs: Long = 0L,
    val collectionTag: String = "classic_cartoons",
    val alternativeStreamUrls: List<String> = emptyList()
) {
    val archiveDetailsUrl: String
        get() = "https://archive.org/details/$id"

    fun getResolvedStreamUrl(): String {
        return videoStreamUrl ?: if (!mp4Filename.isNullOrBlank()) {
            "https://archive.org/download/$id/$mp4Filename"
        } else {
            "https://archive.org/download/$id/${id}_512kb.mp4"
        }
    }
}

// Internet Archive Advanced Search API response structures
@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "response") val response: ArchiveSearchBody? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveSearchBody(
    @Json(name = "numFound") val numFound: Int = 0,
    @Json(name = "start") val start: Int = 0,
    @Json(name = "docs") val docs: List<ArchiveDoc> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: Any? = null, // Can be String or List
    @Json(name = "year") val year: Any? = null,
    @Json(name = "creator") val creator: Any? = null,
    @Json(name = "downloads") val downloads: Long? = 0L,
    @Json(name = "runtime") val runtime: Any? = null,
    @Json(name = "mediatype") val mediatype: String? = null
) {
    fun getCleanTitle(): String {
        val raw = title ?: identifier.replace("_", " ")
        return raw.replace(Regex("(?i)\\.mp4$"), "").trim()
    }

    fun getCleanYear(): String {
        return when (year) {
            is String -> year.take(4)
            is Number -> year.toString()
            else -> "Classic"
        }
    }

    fun getCleanDescription(): String {
        return when (description) {
            is String -> description.replace(Regex("<[^>]*>"), " ").trim()
            is List<*> -> description.joinToString(" ").replace(Regex("<[^>]*>"), " ").trim()
            else -> "Classic public domain cartoon preserved by the Internet Archive repository."
        }
    }

    fun getCleanCreator(): String {
        return when (creator) {
            is String -> creator.trim()
            is List<*> -> creator.joinToString(", ").trim()
            else -> "Classic Studios"
        }
    }
}

// Internet Archive Metadata Files response structures
@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    @Json(name = "result") val result: List<ArchiveFileEntry>? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFileEntry(
    @Json(name = "name") val name: String,
    @Json(name = "format") val format: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "length") val length: String? = null
)
