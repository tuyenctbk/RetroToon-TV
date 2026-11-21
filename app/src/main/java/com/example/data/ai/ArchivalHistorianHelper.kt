package com.example.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.CartoonItem
import java.util.concurrent.TimeUnit

data class ArchivalNote(
    val headline: String,
    val studioTechnique: String,
    val historicalContext: String,
    val preservationFact: String,
    val spokenSummary: String
)

/**
 * Smart Zero-Cost + Optional BYOK ("Bring Your Own Key") AI Film Historian & Mood Curator.
 *
 * - Default Mode ($0 Developer Cost, 100% On-Device):
 *   Generates rich archival historian commentary, animation studio technique notes,
 *   mood-based recommendations, and interactive Q&A locally without any paid cloud API.
 *   Pairs with Android's built-in on-device TextToSpeech for living-room TV audio narration.
 *
 * - Optional BYOK Mode ($0 Developer Cost, User-Provided Personal Key):
 *   Allows enthusiasts to optionally save their own personal Gemini API key in local
 *   SharedPreferences to query Gemini directly from their device.
 */
object ArchivalHistorianHelper {

    private const val PREFS_NAME = "retrotoon_byok_ai_prefs"
    private const val KEY_USER_GEMINI_API_KEY = "user_personal_gemini_api_key"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    fun getUserApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_GEMINI_API_KEY, "")?.trim().orEmpty()
    }

    fun setUserApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun hasUserApiKey(context: Context): Boolean = getUserApiKey(context).isNotBlank()

    fun buildArchivalNote(cartoon: CartoonItem): ArchivalNote {
        val titleLower = cartoon.title.lowercase()
        val creatorLower = cartoon.creator.lowercase()
        val yearNum = cartoon.year.filter { it.isDigit() }.toIntOrNull() ?: 1940

        val studioTechnique = when {
            titleLower.contains("superman") || (creatorLower.contains("fleischer") && yearNum in 1941..1943) ->
                "Produced by Fleischer Studios using three-strip Technicolor, lush Art Deco theatrical backgrounds, and rotoscoped action sequences with budgets reaching $50,000 per short."
            titleLower.contains("popeye") && yearNum <= 1942 ->
                "Filmed at Fleischer Studios using Max Fleischer's patented 3D 'Stereoptical Process'—hand-cel animation photographed in front of rotating physical miniature tabletop sets."
            titleLower.contains("popeye") ->
                "Produced by Famous Studios with high-contrast theatrical cel painting, brassy orchestral scores, and Jack Mercer's signature improvised muttering vocals."
            titleLower.contains("betty boop") || titleLower.contains("bimbo") || titleLower.contains("koko") ->
                "Crafted in New York's Inkwell / Fleischer Studios using rubber-hose ink-and-paint animation and early Western Electric optical sound-on-film synchronization."
            titleLower.contains("stone age") || titleLower.contains("granite hotel") ->
                "Produced in 1940 by Fleischer Studios as part of the 'Stone Age Cartoons' series—pioneering prehistoric gag comedy two decades before prime-time TV cartoons."
            titleLower.contains("bleep") || creatorLower.contains("soundac") ->
                "Created in 1957 by Robert D. Buchanan at Soundac Productions in Miami as the very first animated sci-fi series produced in color specifically for television."
            titleLower.contains("casper") || titleLower.contains("herman") || titleLower.contains("katnip") ->
                "Animated by Famous Studios (Paramount's successor to Fleischer Studios) using rich Technicolor cel shading and fast-paced orchestral slapstick timing."
            titleLower.contains("kitties") || titleLower.contains("hare") || titleLower.contains("wabbit") || titleLower.contains("daffy") || creatorLower.contains("schlesinger") ->
                "Directed at Termite Terrace under Leon Schlesinger Productions with snappy timing, Carl Stalling's musical cues, and iconic golden-age vocal characterizations."
            titleLower.contains("woody") || creatorLower.contains("lantz") ->
                "Produced by Walter Lantz Productions with vibrant Universal theatrical ink-and-paint and energetic manic slapstick animation."
            titleLower.contains("felix") ->
                "Pioneered by Otto Messmer and Pat Sullivan Studios with expressive visual pantomime, surreal metamorphosing question marks, and high-contrast ink linework."
            titleLower.contains("keaton") || titleLower.contains("stooges") || creatorLower.contains("keaton") ->
                "Captured on 35mm nitrate cinema stock using wide-angle Master Shots so audiences could verify every physical stunt was performed live in a single take."
            else ->
                "Hand-inked on celluloid sheets over watercolor theatrical backgrounds and photographed frame-by-frame onto 35mm theatrical film at 24 frames per second."
        }

        val historicalContext = when {
            yearNum < 1930 ->
                "Released in ${cartoon.year} during the Silent-to-Sound transition era, when theatrical shorts preceded every feature film in grand movie palaces."
            yearNum in 1930..1939 ->
                "Premiered in ${cartoon.year} during the Golden Age of 1930s Jazz Age animation, offering Depression-era theatergoers surreal escapism and big-band rhythms."
            yearNum in 1940..1945 ->
                "Theatrically exhibited in ${cartoon.year} during the early 1940s cinema boom, when animated serials pushed dramatic lighting and cinematic camera angles."
            else ->
                "Released in ${cartoon.year} during the post-war mid-century animation era, transitioning classic theatrical characters toward modern Technicolor storytelling."
        }

        val preservationFact =
            "Preserved in the Internet Archive public domain collection from archival 16mm and 35mm exhibition prints (${cartoon.runtime})."

        val headline = "${cartoon.year} • ${cartoon.creator} Archival Dossier"
        val spokenSummary =
            "${cartoon.title}, released in ${cartoon.year} by ${cartoon.creator}. $studioTechnique $historicalContext"

        return ArchivalNote(
            headline = headline,
            studioTechnique = studioTechnique,
            historicalContext = historicalContext,
            preservationFact = preservationFact,
            spokenSummary = spokenSummary
        )
    }

    /**
     * Answers a user question about the cartoon:
     * - If the user configured their own personal Gemini API key (BYOK), queries Gemini directly
     *   from the user's device ($0 developer cost) and falls back gracefully if offline/invalid.
     * - Otherwise, uses the instant on-device Archival Historian knowledge engine ($0 cost).
     */
    suspend fun askHistorian(
        context: Context,
        cartoon: CartoonItem,
        question: String,
        topicKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val note = buildArchivalNote(cartoon)
        val userKey = getUserApiKey(context)

        if (userKey.isNotBlank()) {
            try {
                val prompt = """
                    You are the RetroToon TV Archival Film Historian specializing in 1920s-1950s public domain animation and classic cinema.
                    Short Title: ${cartoon.title} (${cartoon.year})
                    Studio/Creator: ${cartoon.creator}
                    Synopsis: ${cartoon.description}
                    Archival Notes: ${note.studioTechnique} ${note.historicalContext}
                    
                    User Question: $question
                    Answer concisely in 2 to 4 engaging sentences suitable for a classic movie theater program.
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
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$userKey")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (response.isSuccessful && bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        val candidates = json.optJSONArray("candidates")
                        val firstPart = candidates
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")
                            ?.trim()
                        if (!firstPart.isNullOrBlank()) {
                            return@withContext Result.success(firstPart)
                        }
                    } else {
                        return@withContext Result.success(
                            answerLocally(cartoon, note, question, topicKey) +
                                "\n\n(Note: Personal Gemini key returned HTTP ${response.code}; answered via Built-in On-Device Historian.)"
                        )
                    }
                }
            } catch (_: Exception) {
                return@withContext Result.success(
                    answerLocally(cartoon, note, question, topicKey)
                )
            }
        }

        Result.success(answerLocally(cartoon, note, question, topicKey))
    }

    private fun answerLocally(
        cartoon: CartoonItem,
        note: ArchivalNote,
        question: String,
        topicKey: String? = null
    ): String {
        val q = question.lowercase()
        return when {
            topicKey == "technique" || q.contains("how") || q.contains("animat") || q.contains("tech") || q.contains("style") || q.contains("3d") ->
                "Animation & Technique: ${note.studioTechnique} Every frame was photographed onto 35mm film before digital restoration for the Internet Archive."
            topicKey == "music" || q.contains("music") || q.contains("sound") || q.contains("voice") || q.contains("audio") ->
                "Sound & Music: '${cartoon.title}' (${cartoon.year}) features vintage optical sound-on-film recording (${cartoon.creator}). In RetroToon TV's player, you can toggle Optical Audio Boost (+5.5 dB) or 1920s Ragtime Piano accompaniment."
            topicKey == "public_domain" || q.contains("public domain") || q.contains("legal") || q.contains("copyright") || q.contains("archive") ->
                "Public Domain Status: '${cartoon.title}' (${cartoon.year}) entered the public domain after its initial 28-year US copyright term under the 1909 Copyright Act lapsed without renewal, and is preserved by the Internet Archive."
            else ->
                "'${cartoon.title}' (${cartoon.year}) by ${cartoon.creator}: ${cartoon.description} ${note.historicalContext} ${note.studioTechnique}"
        }
    }

    /**
     * Converts a spoken natural-language voice command (e.g., "Play 1930s Betty Boop jazz cartoons")
     * into an effective Internet Archive search query at $0 cost on-device.
     */
    fun extractSearchKeywordsFromVoice(spokenText: String): String {
        val raw = spokenText.trim()
        if (raw.isEmpty()) return ""
        val lower = raw.lowercase()
        return when {
            lower.contains("superman") -> "Superman"
            lower.contains("popeye") -> "Popeye"
            lower.contains("betty") || lower.contains("boop") -> "Betty Boop"
            lower.contains("felix") -> "Felix the Cat"
            lower.contains("keaton") || lower.contains("buster") -> "Buster Keaton"
            lower.contains("casper") -> "Casper"
            lower.contains("herman") || lower.contains("katnip") -> "Herman and Katnip"
            lower.contains("stone age") || lower.contains("granite") -> "Stone Age"
            lower.contains("bleep") || lower.contains("space") -> "Colonel Bleep"
            lower.contains("looney") || lower.contains("daffy") || lower.contains("hare") || lower.contains("wabbit") -> "Looney Tunes"
            lower.contains("woody") -> "Woody Woodpecker"
            lower.contains("audrey") -> "Little Audrey"
            lower.contains("stooges") -> "Three Stooges"
            else -> raw
                .replace(Regex("(?i)\\b(play|show|me|find|search|for|some|watch|cartoons|cartoon|shorts|short|please)\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifEmpty { raw }
        }
    }
}

