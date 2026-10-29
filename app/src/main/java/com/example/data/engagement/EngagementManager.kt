package com.example.data.engagement

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

enum class SuggestionType {
    RATE_APP,
    SHARE_APP,
    CHECK_UPDATE
}

data class AppSuggestion(
    val type: SuggestionType,
    val title: String,
    val subtitle: String,
    val primaryButtonText: String,
    val secondaryButtonText: String
)

class EngagementManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("retrotoon_engagement_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_SHORTS_WATCHED = "shorts_watched_count"
        private const val KEY_WATCHLIST_ADDS = "watchlist_adds_count"
        private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
        private const val KEY_RATE_DISMISSED = "rate_dismissed_forever"
        private const val KEY_RATE_COMPLETED = "rate_completed"
        private const val KEY_SHARE_DISMISSED = "share_dismissed"
        private const val KEY_UPDATE_DISMISSED = "update_dismissed_forever"

        // Min 7 days cooldown between any prompt to ensure users are never bothered frequently
        private const val COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000L
    }

    fun recordAppLaunch() {
        val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply()
    }

    fun recordShortCompleted() {
        val count = prefs.getInt(KEY_SHORTS_WATCHED, 0) + 1
        prefs.edit().putInt(KEY_SHORTS_WATCHED, count).apply()
    }

    fun recordWatchlistAdd() {
        val count = prefs.getInt(KEY_WATCHLIST_ADDS, 0) + 1
        prefs.edit().putInt(KEY_WATCHLIST_ADDS, count).apply()
    }

    fun evaluatePendingSuggestion(isCurrentlyPlaying: Boolean): AppSuggestion? {
        // Rule 1: Never interrupt user while watching a movie/short
        if (isCurrentlyPlaying) return null

        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)

        // Rule 2: Minimum 7-day cooldown between any suggestion
        if (now - lastPrompt < COOLDOWN_MS) return null

        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)
        val shortsWatched = prefs.getInt(KEY_SHORTS_WATCHED, 0)
        val watchlistAdds = prefs.getInt(KEY_WATCHLIST_ADDS, 0)
        val rateDismissed = prefs.getBoolean(KEY_RATE_DISMISSED, false)
        val rateCompleted = prefs.getBoolean(KEY_RATE_COMPLETED, false)
        val shareDismissed = prefs.getBoolean(KEY_SHARE_DISMISSED, false)
        val updateDismissed = prefs.getBoolean(KEY_UPDATE_DISMISSED, false)

        // Rule 3: Positive milestone 1 - User completed watching multiple classic shorts (at least 3)
        if (shortsWatched >= 3 && !rateCompleted && !rateDismissed) {
            return AppSuggestion(
                type = SuggestionType.RATE_APP,
                title = "Enjoying Vintage Cartoons?",
                subtitle = "Help us keep public domain animation free and accessible by rating RetroToon TV on Google Play!",
                primaryButtonText = "Rate 5 Stars ★",
                secondaryButtonText = "Remind Later"
            )
        }

        // Rule 4: Positive milestone 2 - User curated their watchlist (at least 3 items)
        if (watchlistAdds >= 3 && !shareDismissed) {
            return AppSuggestion(
                type = SuggestionType.SHARE_APP,
                title = "Spread the Golden Age Magic!",
                subtitle = "Share RetroToon TV with friends and family so they can stream Betty Boop, Popeye, and Superman shorts for free.",
                primaryButtonText = "Share App",
                secondaryButtonText = "Maybe Later"
            )
        }

        // Rule 5: Loyal user update confirmation after many sessions (at least 8)
        if (sessions >= 8 && !updateDismissed && (now - lastPrompt >= 14 * 24 * 60 * 60 * 1000L)) {
            return AppSuggestion(
                type = SuggestionType.CHECK_UPDATE,
                title = "RetroToon TV is Up-to-Date",
                subtitle = "You are running the latest v1.0 release. Connected live with Internet Archive public domain mirrors.",
                primaryButtonText = "Check Play Store",
                secondaryButtonText = "Great!"
            )
        }

        return null
    }

    fun onSuggestionActionTaken(type: SuggestionType, isPositiveAction: Boolean) {
        val now = System.currentTimeMillis()
        val editor = prefs.edit().putLong(KEY_LAST_PROMPT_TIME, now)

        when (type) {
            SuggestionType.RATE_APP -> {
                if (isPositiveAction) {
                    editor.putBoolean(KEY_RATE_COMPLETED, true)
                    openPlayStore()
                } else {
                    // post-pone for at least 5 days
                    editor.putLong(KEY_LAST_PROMPT_TIME, now + 5 * 24 * 60 * 60 * 1000L)
                }
            }
            SuggestionType.SHARE_APP -> {
                if (isPositiveAction) {
                    editor.putBoolean(KEY_SHARE_DISMISSED, true)
                    launchShareSheet()
                }
            }
            SuggestionType.CHECK_UPDATE -> {
                if (isPositiveAction) {
                    openPlayStore()
                }
            }
        }
        editor.apply()
    }

    fun dismissForever(type: SuggestionType) {
        val editor = prefs.edit().putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
        when (type) {
            SuggestionType.RATE_APP -> editor.putBoolean(KEY_RATE_DISMISSED, true)
            SuggestionType.SHARE_APP -> editor.putBoolean(KEY_SHARE_DISMISSED, true)
            SuggestionType.CHECK_UPDATE -> editor.putBoolean(KEY_UPDATE_DISMISSED, true)
        }
        editor.apply()
    }

    private fun openPlayStore() {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun launchShareSheet() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Stream classic 1930s-1950s public domain cartoons (Superman 1941, Popeye, Betty Boop, Buster Keaton) for free with RetroToon TV! https://play.google.com/store/apps/details?id=${context.packageName}"
                )
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share RetroToon TV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (_: Exception) {}
    }
}
