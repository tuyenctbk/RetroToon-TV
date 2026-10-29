package com.example.data.api

import android.util.Log
import com.example.data.model.ArchiveDoc
import com.example.data.model.ArchiveFileEntry
import com.example.data.model.CartoonItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArchiveApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun searchCartoons(query: String, rows: Int = 30): List<CartoonItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CartoonItem>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery" +
                    "&fl[]=identifier,title,description,year,creator,downloads,runtime,mediatype" +
                    "&sort[]=downloads+desc&rows=$rows&page=1&output=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RetroToonTV-Leanback/1.0 (Android Leanback)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Search request failed with HTTP ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonRoot = JSONObject(bodyString)
                val respObj = jsonRoot.optJSONObject("response") ?: return@withContext emptyList()
                val docsArray = respObj.optJSONArray("docs") ?: return@withContext emptyList()

                for (i in 0 until docsArray.length()) {
                    val docObj = docsArray.optJSONObject(i) ?: continue
                    val identifier = docObj.optString("identifier", "")
                    if (identifier.isBlank()) continue

                    val rawTitle = docObj.optString("title", identifier.replace("_", " "))
                    val cleanTitle = rawTitle.replace(Regex("(?i)\\.mp4$"), "").trim()
                    val yearStr = docObj.opt("year")?.toString()?.take(4) ?: "1940s"
                    val creatorStr = docObj.opt("creator")?.toString() ?: "Classic Animation"
                    val descStr = docObj.opt("description")?.toString()
                        ?.replace(Regex("<[^>]*>"), " ")?.trim() ?: "Vintage cartoon classic."
                    val downloadsCount = docObj.optLong("downloads", 0L)
                    val runtimeStr = docObj.opt("runtime")?.toString() ?: "8m"

                    results.add(
                        CartoonItem(
                            id = identifier,
                            title = cleanTitle,
                            year = yearStr,
                            creator = creatorStr,
                            description = descStr,
                            runtime = runtimeStr,
                            downloads = downloadsCount,
                            thumbnailUrl = "https://archive.org/services/img/$identifier",
                            collectionTag = query
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing search: ${e.message}", e)
        }
        return@withContext results
    }

    private val subtitleCache = mutableMapOf<String, String>()

    fun getCachedSubtitleUrl(identifier: String): String? = subtitleCache[identifier]

    suspend fun resolveMp4Files(identifier: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://archive.org/metadata/$identifier/files"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RetroToonTV-Leanback/1.0 (Android Leanback)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val filesArr = root.optJSONArray("result") ?: return@withContext emptyList()

                val mp4List = mutableListOf<String>()
                val fallbackVideoList = mutableListOf<String>()
                for (i in 0 until filesArr.length()) {
                    val fileObj = filesArr.optJSONObject(i) ?: continue
                    val name = fileObj.optString("name", "")
                    if (name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".m4v", ignoreCase = true)) {
                        mp4List.add(name)
                    } else if (name.endsWith(".ogv", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true)) {
                        fallbackVideoList.add(name)
                    } else if (name.endsWith(".vtt", ignoreCase = true) || name.endsWith(".srt", ignoreCase = true)) {
                        if (!subtitleCache.containsKey(identifier)) {
                            val encodedSub = URLEncoder.encode(name, "UTF-8")
                                .replace("+", "%20")
                                .replace("%2F", "/")
                            subtitleCache[identifier] = "https://archive.org/download/$identifier/$encodedSub"
                        }
                    }
                }

                val combined = mp4List + fallbackVideoList
                if (combined.isEmpty()) return@withContext emptyList()

                // Sort: MP4s with sound first (512kb -> standard mp4 -> ia.mp4), then fallback containers
                return@withContext combined.sortedWith(
                    compareByDescending<String> { it.endsWith(".mp4", ignoreCase = true) || it.endsWith(".m4v", ignoreCase = true) }
                        .thenByDescending { !it.contains("silent", ignoreCase = true) }
                        .thenByDescending { it.contains("512kb", ignoreCase = true) }
                        .thenByDescending { !it.contains("ia.mp4", ignoreCase = true) }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve files for $identifier: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun resolveBestMp4File(identifier: String): String? {
        return resolveMp4Files(identifier).firstOrNull()
    }

    companion object {
        private const val TAG = "ArchiveApiClient"
    }
}
