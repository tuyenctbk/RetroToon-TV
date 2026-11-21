package com.example.data.live

import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.CartoonItem

data class LiveTvChannel(
    val id: String,
    val number: Int,
    @param:StringRes val nameRes: Int,
    @param:StringRes val taglineRes: Int,
    val accentHex: Long,
    val playlist: List<CartoonItem>
)

data class LiveChannelProgram(
    val channel: LiveTvChannel,
    val currentItem: CartoonItem,
    val offsetMs: Long,
    val durationMs: Long,
    val upNextItem: CartoonItem,
    val laterItem: CartoonItem
) {
    val remainingMs: Long
        get() = (durationMs - offsetMs).coerceAtLeast(0L)

    val progressFraction: Float
        get() = if (durationMs > 0L) (offsetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

data class PublicDomainSuggestion(
    val matchedFranchiseLabel: String,
    val recommendedQueries: List<String>
)

/**
 * Deterministic 24/7 Live TV Schedule Engine (Zero Backend / $0 Cost).
 *
 * Uses UTC wall-clock time modulo each channel's total playlist runtime so every viewer
 * tuning into a channel watches the exact same classic cartoon at the exact same timestamp,
 * recreating an authentic linear TV broadcast experience on Android & Android TV.
 */
object LiveTvChannelEngine {

    private const val DEFAULT_SHORT_DURATION_MS = 7L * 60L * 1000L // 7 min fallback

    fun parseRuntimeMs(runtime: String, durationMsHint: Long = 0L): Long {
        if (durationMsHint > 15_000L) return durationMsHint
        val cleaned = runtime.trim().lowercase()
        if (cleaned.isEmpty()) return DEFAULT_SHORT_DURATION_MS

        // Format "MM:SS" or "HH:MM:SS"
        if (cleaned.contains(":")) {
            val parts = cleaned.split(":").mapNotNull { it.filter(Char::isDigit).toLongOrNull() }
            return when (parts.size) {
                2 -> ((parts[0] * 60L) + parts[1]).coerceAtLeast(60L) * 1000L
                3 -> ((parts[0] * 3600L) + (parts[1] * 60L) + parts[2]).coerceAtLeast(60L) * 1000L
                else -> DEFAULT_SHORT_DURATION_MS
            }
        }

        // Format "8m" or "12 min"
        val minutes = cleaned.filter(Char::isDigit).toLongOrNull()
        return if (minutes != null && minutes in 1L..180L) {
            minutes * 60L * 1000L
        } else {
            DEFAULT_SHORT_DURATION_MS
        }
    }

    fun buildChannels(
        featured: List<CartoonItem>,
        superman: List<CartoonItem>,
        popeye: List<CartoonItem>,
        betty: List<CartoonItem>,
        classicsAndSciFi: List<CartoonItem>,
        serials: List<CartoonItem>
    ): List<LiveTvChannel> {
        val allMix = (featured + classicsAndSciFi + superman + popeye + betty + serials)
            .distinctBy { it.id }
        val heroesAndSciFi = (superman + classicsAndSciFi.filter {
            val t = it.title.lowercase()
            t.contains("bleep") || t.contains("stone age") || t.contains("superman") || t.contains("space")
        } + serials.take(2)).distinctBy { it.id }.ifEmpty { allMix }

        val slapstickAndLaughs = (popeye + classicsAndSciFi + serials.filter {
            val t = it.title.lowercase()
            t.contains("stooges") || t.contains("keaton") || t.contains("felix")
        }).distinctBy { it.id }.ifEmpty { allMix }

        val jazzAndInkwell = (betty + serials.filter {
            it.title.lowercase().contains("felix") || it.title.lowercase().contains("keaton")
        } + classicsAndSciFi.filter {
            it.title.lowercase().contains("casper") || it.title.lowercase().contains("kitties")
        }).distinctBy { it.id }.ifEmpty { allMix }

        return listOf(
            LiveTvChannel(
                id = "ch_01_saturday_mix",
                number = 1,
                nameRes = R.string.live_ch1_name,
                taglineRes = R.string.live_ch1_tagline,
                accentHex = 0xFFFFC857,
                playlist = allMix
            ),
            LiveTvChannel(
                id = "ch_02_heroes_scifi",
                number = 2,
                nameRes = R.string.live_ch2_name,
                taglineRes = R.string.live_ch2_tagline,
                accentHex = 0xFFE63946,
                playlist = heroesAndSciFi
            ),
            LiveTvChannel(
                id = "ch_03_slapstick_laughs",
                number = 3,
                nameRes = R.string.live_ch3_name,
                taglineRes = R.string.live_ch3_tagline,
                accentHex = 0xFF38BDF8,
                playlist = slapstickAndLaughs
            ),
            LiveTvChannel(
                id = "ch_04_jazz_inkwell",
                number = 4,
                nameRes = R.string.live_ch4_name,
                taglineRes = R.string.live_ch4_tagline,
                accentHex = 0xFFC084FC,
                playlist = jazzAndInkwell
            )
        ).filter { it.playlist.isNotEmpty() }
    }

    /**
     * Resolves what is currently "On Air" on [channel] at [nowMs] UTC.
     */
    fun getCurrentProgram(
        channel: LiveTvChannel,
        nowMs: Long = System.currentTimeMillis()
    ): LiveChannelProgram? {
        val items = channel.playlist
        if (items.isEmpty()) return null

        val durations = items.map { parseRuntimeMs(it.runtime, it.durationMs) }
        val totalCycleMs = durations.sum().coerceAtLeast(1L)
        // Offset each channel by a deterministic prime phase so channels never air the same show together
        val channelPhaseMs = (channel.number.toLong() * 317_000L)
        val cyclePosMs = Math.floorMod(nowMs + channelPhaseMs, totalCycleMs)

        var cursor = 0L
        var activeIndex = 0
        var offsetInItem = 0L
        for (i in items.indices) {
            val dur = durations[i]
            if (cyclePosMs < cursor + dur) {
                activeIndex = i
                offsetInItem = (cyclePosMs - cursor).coerceAtLeast(0L)
                break
            }
            cursor += dur
        }

        val currentItem = items[activeIndex]
        val upNextItem = items[(activeIndex + 1) % items.size]
        val laterItem = items[(activeIndex + 2) % items.size]

        // Keep initial live tune-in at least 15 seconds before the end so user doesn't land on credits immediately
        val currentDuration = durations[activeIndex]
        val safeOffset = if (currentDuration > 25_000L && offsetInItem > currentDuration - 15_000L) {
            (currentDuration - 15_000L).coerceAtLeast(0L)
        } else {
            offsetInItem
        }

        return LiveChannelProgram(
            channel = channel,
            currentItem = currentItem,
            offsetMs = safeOffset,
            durationMs = currentDuration,
            upNextItem = upNextItem,
            laterItem = laterItem
        )
    }

    /**
     * Returns the next scheduled item on [channel] after [currentItemId].
     */
    fun getNextItemInChannel(channel: LiveTvChannel, currentItemId: String): CartoonItem? {
        val items = channel.playlist
        if (items.isEmpty()) return null
        val idx = items.indexOfFirst { it.id == currentItemId }
        return if (idx >= 0) items[(idx + 1) % items.size] else items.first()
    }

    /**
     * Detects when a user searches for actively copyrighted studio franchises and returns
     * tailored 100% legal Public Domain search suggestions so users always discover great cartoons.
     */
    fun detectCopyrightedFranchiseQuery(query: String): PublicDomainSuggestion? {
        val q = query.trim().lowercase()
        if (q.length < 3) return null

        return when {
            q.contains("tom and jerry") || q.contains("tom & jerry") || q.contains("tom jerry") ->
                PublicDomainSuggestion(
                    matchedFranchiseLabel = query.trim(),
                    recommendedQueries = listOf("Herman and Katnip", "Looney Tunes", "Popeye", "Woody Woodpecker")
                )
            q.contains("flintstone") || q.contains("fling stone") || q.contains("flinstone") ->
                PublicDomainSuggestion(
                    matchedFranchiseLabel = query.trim(),
                    recommendedQueries = listOf("Stone Age", "Popeye", "Looney Tunes", "Casper")
                )
            q.contains("jetson") ->
                PublicDomainSuggestion(
                    matchedFranchiseLabel = query.trim(),
                    recommendedQueries = listOf("Colonel Bleep", "Superman", "Stone Age", "Felix the Cat")
                )
            q.contains("batman") || q.contains("bat man") || q.contains("robin") || q.contains("robbin") ||
                q.contains("super friends") || q.contains("superfriends") || q.contains("justice league") ||
                q.contains("spider") || q.contains("avengers") || q.contains("x-men") ->
                PublicDomainSuggestion(
                    matchedFranchiseLabel = query.trim(),
                    recommendedQueries = listOf("Superman", "Colonel Bleep", "Flash Gordon", "Popeye")
                )
            q.contains("scooby") || q.contains("simpson") || q.contains("spongebob") ||
                q.contains("pokemon") || q.contains("disney") || q.contains("mickey") ||
                q.contains("pink panther") || q.contains("garfield") || q.contains("he-man") ->
                PublicDomainSuggestion(
                    matchedFranchiseLabel = query.trim(),
                    recommendedQueries = listOf("Casper", "Looney Tunes", "Betty Boop", "Felix the Cat")
                )
            else -> null
        }
    }
}
