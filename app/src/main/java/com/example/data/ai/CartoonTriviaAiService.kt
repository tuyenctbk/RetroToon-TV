package com.example.data.ai

import android.content.Context
import com.example.data.model.CartoonItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DidYouKnowFact(
    val categoryBadge: String,
    val factText: String,
    val isCloudAiGenerated: Boolean = false
)

/**
 * Gemini AI Service that generates short historical context summaries ("Did You Know?" cards)
 * for classic cartoons during video playback.
 *
 * - Supports User-Provided BYOK Gemini API Key (via ArchivalHistorianHelper) calling
 *   Gemini 2.5 Flash with structured JSON output (`responseMimeType = "application/json"`).
 * - Automatically caches generated facts per cartoon ID in memory.
 * - Seamlessly falls back to rich, cartoon-specific historical context cards at $0 developer cost
 *   when no API key is configured or when offline.
 */
object CartoonTriviaAiService {

    private val factCache = ConcurrentHashMap<String, List<DidYouKnowFact>>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getDidYouKnowFacts(
        context: Context,
        cartoon: CartoonItem
    ): List<DidYouKnowFact> = withContext(Dispatchers.IO) {
        val userKey = ArchivalHistorianHelper.getUserApiKey(context)
        val cacheKey = "${cartoon.id}_${userKey.hashCode()}"
        factCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        if (userKey.isNotBlank()) {
            val geminiFacts = fetchFactsFromGeminiApi(userKey, cartoon)
            if (geminiFacts.isNotEmpty()) {
                factCache[cacheKey] = geminiFacts
                return@withContext geminiFacts
            }
        }

        val curatedFacts = buildCuratedHistoricalFacts(cartoon)
        factCache[cacheKey] = curatedFacts
        curatedFacts
    }

    private fun fetchFactsFromGeminiApi(
        apiKey: String,
        cartoon: CartoonItem
    ): List<DidYouKnowFact> {
        return try {
            val prompt = """
                Generate 3 short, fascinating "Did You Know?" historical context facts about this classic public domain film:
                Title: ${cartoon.title}
                Year: ${cartoon.year}
                Studio/Creator: ${cartoon.creator}
                Synopsis: ${cartoon.description}
                
                Return a JSON array of objects where each object has:
                - "categoryBadge": short uppercase label (e.g., "STUDIO HISTORY", "ANIMATION TECH", "CULTURAL IMPACT")
                - "factText": 1 to 2 concise sentences (max 180 characters) suitable for an on-screen "Did You Know?" overlay card during video playback.
            """.trimIndent()

            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", prompt))
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.4)
                    }
                )
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful || bodyStr.isBlank()) return emptyList()

                val root = JSONObject(bodyStr)
                val rawJsonText = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    .orEmpty()

                if (rawJsonText.isBlank()) return emptyList()

                val array = JSONArray(rawJsonText)
                val list = mutableListOf<DidYouKnowFact>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val badge = obj.optString("categoryBadge", "HISTORICAL TRIVIA").trim()
                    val text = obj.optString("factText", "").trim()
                    if (text.isNotBlank()) {
                        list.add(
                            DidYouKnowFact(
                                categoryBadge = badge,
                                factText = text,
                                isCloudAiGenerated = true
                            )
                        )
                    }
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildCuratedHistoricalFacts(cartoon: CartoonItem): List<DidYouKnowFact> {
        val note = ArchivalHistorianHelper.buildArchivalNote(cartoon)
        val titleLower = cartoon.title.lowercase()
        val yearNum = cartoon.year.filter { it.isDigit() }.toIntOrNull() ?: 1940

        val triviaExtra = when {
            titleLower.contains("superman") ->
                "Bud Collyer voiced both mild-mannered Clark Kent and Superman by dropping his vocal register an octave whenever Clark stepped into a phone booth!"
            titleLower.contains("popeye") ->
                "Voice actor Jack Mercer improvised many of Popeye's famous under-his-breath mutterings live at the microphone during Fleischer recording sessions."
            titleLower.contains("betty boop") ->
                "Mae Questel's signature 'Boop-Oop-A-Doop' vocals helped make Betty Boop the first animated character to headline her own theatrical series."
            titleLower.contains("felix") ->
                "In 1928, a paper-mache statue of Felix the Cat became the very first image ever broadcast by RCA's experimental mechanical television transmitter!"
            titleLower.contains("keaton") ->
                "Buster Keaton insisted on performing his own high-risk physical stunts without camera cuts so theater audiences knew the gag was 100% real."
            yearNum < 1935 ->
                "Early 1930s ink-and-paint artists hand-traced up to 10,000 individual celluloid frames for a single 7-minute theatrical short."
            else ->
                "Before home television became widespread, theatrical cartoon shorts like '${cartoon.title}' (${cartoon.year}) were screened alongside newsreels in movie palaces."
        }

        return listOf(
            DidYouKnowFact(
                categoryBadge = "STUDIO & TECHNIQUE • ${cartoon.year}",
                factText = note.studioTechnique,
                isCloudAiGenerated = false
            ),
            DidYouKnowFact(
                categoryBadge = "HISTORICAL CONTEXT • ${cartoon.creator.uppercase()}",
                factText = note.historicalContext,
                isCloudAiGenerated = false
            ),
            DidYouKnowFact(
                categoryBadge = "BEHIND THE REEL • ARCHIVAL TRIVIA",
                factText = triviaExtra,
                isCloudAiGenerated = false
            )
        )
    }
}
