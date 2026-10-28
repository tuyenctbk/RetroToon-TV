package com.example.data.donation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

data class DonationTier(
    val id: String,
    val title: String,
    val price: String,
    val iconEmoji: String,
    val description: String
)

class DonationManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("retrotoon_donation_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_SUPPORTER = "is_supporter"
        private const val KEY_SUPPORTER_TIER = "supporter_tier"
        private const val KEY_INSTALL_TIME = "install_timestamp"
        private const val KEY_OPS_COUNT = "successful_ops_count"
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_BANNER_DISMISSED = "banner_dismissed_time"
        private const val KEY_AUTO_PROMPT_DISMISSED = "auto_prompt_dismissed_time"

        // Cooldowns
        private const val BANNER_COOLDOWN_MS = 14 * 24 * 60 * 60 * 1000L // 14 days
        private const val AUTO_PROMPT_COOLDOWN_MS = 30 * 24 * 60 * 60 * 1000L // 30 days
        private const val QUALIFYING_DAYS_MS = 2 * 24 * 60 * 60 * 1000L // 2 days since install
    }

    init {
        if (!prefs.contains(KEY_INSTALL_TIME)) {
            prefs.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply()
        }
    }

    val playTiers = listOf(
        DonationTier("tip_tier_1", "Classic Soda", "$1.99", "🥤", "Keep the projector running with a refreshing soda tip"),
        DonationTier("tip_tier_2", "Popcorn & Soda", "$4.99", "🍿", "Support theater server hosting and restored archiving"),
        DonationTier("tip_tier_3", "Film Reel Sponsor", "$9.99", "🎞️", "Help preserve and digitize rare vintage cartoons"),
        DonationTier("tip_tier_4", "Cinema Benefactor", "$19.99", "⭐", "Ultimate patron of public domain classic animation")
    )

    fun recordAppLaunch() {
        val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply()
    }

    fun recordSuccessfulOperation() {
        val count = prefs.getInt(KEY_OPS_COUNT, 0) + 1
        prefs.edit().putInt(KEY_OPS_COUNT, count).apply()
    }

    val isSupporter: Boolean
        get() = prefs.getBoolean(KEY_IS_SUPPORTER, false)

    val supporterTier: String?
        get() = prefs.getString(KEY_SUPPORTER_TIER, null)

    fun markAsSupporter(tierTitle: String) {
        prefs.edit()
            .putBoolean(KEY_IS_SUPPORTER, true)
            .putString(KEY_SUPPORTER_TIER, tierTitle)
            .apply()
    }

    fun shouldShowHomeBanner(): Boolean {
        if (isSupporter) return false

        val now = System.currentTimeMillis()
        val installTime = prefs.getLong(KEY_INSTALL_TIME, now)
        val dismissedTime = prefs.getLong(KEY_BANNER_DISMISSED, 0L)
        val opsCount = prefs.getInt(KEY_OPS_COUNT, 0)
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)

        // Satisfaction Safeguards:
        // 1. Min 2 days since install
        if (now - installTime < QUALIFYING_DAYS_MS) return false
        // 2. Min 3 successful operations
        if (opsCount < 3) return false
        // 3. Min 3 sessions
        if (sessions < 3) return false
        // 4. 14-day cooldown after dismissal
        if (now - dismissedTime < BANNER_COOLDOWN_MS) return false

        return true
    }

    fun dismissHomeBanner() {
        prefs.edit().putLong(KEY_BANNER_DISMISSED, System.currentTimeMillis()).apply()
    }

    fun shouldTriggerAutoPrompt(): Boolean {
        if (isSupporter) return false

        val now = System.currentTimeMillis()
        val installTime = prefs.getLong(KEY_INSTALL_TIME, now)
        val dismissedTime = prefs.getLong(KEY_AUTO_PROMPT_DISMISSED, 0L)
        val opsCount = prefs.getInt(KEY_OPS_COUNT, 0)

        // 7+ operations and 30-day cooldown
        if (opsCount < 7) return false
        if (now - installTime < QUALIFYING_DAYS_MS) return false
        if (now - dismissedTime < AUTO_PROMPT_COOLDOWN_MS) return false

        return true
    }

    fun dismissAutoPrompt() {
        prefs.edit().putLong(KEY_AUTO_PROMPT_DISMISSED, System.currentTimeMillis()).apply()
    }

    fun openPayPal(presetAmount: Int? = null) {
        val baseUrl = "https://paypal.me/retrotoontv"
        val targetUrl = if (presetAmount != null) "$baseUrl/${presetAmount}USD" else baseUrl
        launchUrl(targetUrl)
    }

    fun openKofi() {
        launchUrl("https://ko-fi.com/retrotoontv")
    }

    private fun launchUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
