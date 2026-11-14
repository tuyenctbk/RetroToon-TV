# Smart Suggestion & Engagement Dialog Architecture
### Complete Production Guide: Rating, Updating, and Sharing for Android & Android TV

This architectural blueprint outlines the production-ready engagement and feedback dialog system developed for **RetroToon TV**. It is specifically designed to be portable and reusable across any Android or Android TV application using Jetpack Compose.

---

## 1. Core Principles & UX Philosophy

Modern app stores (Google Play, Amazon Appstore) and user experience standards demand non-intrusive, context-aware engagement:

1. **Zero Disruption Rule**: Never prompt the user during an active, high-immersion flow (e.g., active video streaming, in-game sessions, payment checkouts).
2. **Positive Milestone Triggers**: Only prompt after the user has experienced tangible value (e.g., watched 3 full episodes, saved 3 items to favorites, or launched the app 8+ times over multiple weeks).
3. **Respectful Frequency & Cooldowns**:
   - Minimum **7-day cooldown** between any dialog appearance.
   - Secondary button ("Remind me later") schedules a gentle **5-day postponement**.
   - "Don't ask again" or successful rating **permanently silences** that modal forever.
4. **Universal D-Pad & Touch Accessibility**:
   - Works flawlessly on handheld touchscreens and Android TV remote controls.
   - Automatic focus delivery (`FocusRequester`) to the primary call-to-action on TV.
   - Distinct, high-visibility focus borders (`2.dp` contrast strokes) so the TV remote user always knows where they are.

---

## 2. Architecture Overview

```
                      +---------------------------------------+
                      |       User Engagement Event           |
                      | (Launch, Item Completed, Save Fav)    |
                      +---------------------------------------+
                                          |
                                          v
                      +---------------------------------------+
                      |         EngagementManager             |
                      |  - Records metric count in Prefs      |
                      |  - Evaluates business rules & gates   |
                      +---------------------------------------+
                                          |
                      +-------------------+-------------------+
                      |                                       |
           [Cooldown active or                     [Milestone met &
            video actively playing]                 cooldown elapsed]
                      |                                       |
                      v                                       v
                 (No Dialog)                        AppSuggestion(
                                                      type = RATE_APP,
                                                      title = "Enjoying the App?",
                                                      primaryText = "Rate 5 Stars",
                                                      ...
                                                    )
                                                              |
                                                              v
                                              +-------------------------------+
                                              |       SuggestionDialog        |
                                              | (Jetpack Compose UI Modal)    |
                                              +-------------------------------+
                                                /            |             \
                                               /             |              \
                                     [Primary CTA]    [Secondary CTA]    ["Never Ask"]
                                           |                 |                  |
                                    openPlayStore()   postpone(5 days)   dismissForever()
```

---

## 3. Data Engine: `EngagementManager.kt`

The `EngagementManager` acts as the single source of truth for tracking engagement milestones, cooldown intervals, and triggering conditions.

```kotlin
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
        context.getSharedPreferences("app_engagement_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_MILESTONE_COMPLETED = "milestone_completed_count"
        private const val KEY_FAVORITES_COUNT = "favorites_count"
        private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
        private const val KEY_RATE_DISMISSED = "rate_dismissed_forever"
        private const val KEY_RATE_COMPLETED = "rate_completed"
        private const val KEY_SHARE_DISMISSED = "share_dismissed"
        private const val KEY_UPDATE_DISMISSED = "update_dismissed_forever"

        // Minimum 7 days cooldown between prompts
        private const val COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000L
    }

    fun recordAppLaunch() {
        val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply()
    }

    fun recordContentCompleted() {
        val count = prefs.getInt(KEY_MILESTONE_COMPLETED, 0) + 1
        prefs.edit().putInt(KEY_MILESTONE_COMPLETED, count).apply()
    }

    fun recordFavoriteAdded() {
        val count = prefs.getInt(KEY_FAVORITES_COUNT, 0) + 1
        prefs.edit().putInt(KEY_FAVORITES_COUNT, count).apply()
    }

    /**
     * Evaluates whether a suggestion dialog is eligible to be shown.
     * Returns null if any cooldown is active or user is currently in an active session.
     */
    fun evaluatePendingSuggestion(isCurrentlyBusy: Boolean, currentAppVersion: String): AppSuggestion? {
        if (isCurrentlyBusy) return null

        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)

        // Rule: Guard against frequent prompts
        if (now - lastPrompt < COOLDOWN_MS) return null

        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)
        val completed = prefs.getInt(KEY_MILESTONE_COMPLETED, 0)
        val favorites = prefs.getInt(KEY_FAVORITES_COUNT, 0)
        val rateDismissed = prefs.getBoolean(KEY_RATE_DISMISSED, false)
        val rateCompleted = prefs.getBoolean(KEY_RATE_COMPLETED, false)
        val shareDismissed = prefs.getBoolean(KEY_SHARE_DISMISSED, false)
        val updateDismissed = prefs.getBoolean(KEY_UPDATE_DISMISSED, false)

        // 1. Rating Milestone: User completed at least 3 items
        if (completed >= 3 && !rateCompleted && !rateDismissed) {
            return AppSuggestion(
                type = SuggestionType.RATE_APP,
                title = "Enjoying the App?",
                subtitle = "Your rating on Google Play helps us preserve classic animation and build new features!",
                primaryButtonText = "Rate 5 Stars",
                secondaryButtonText = "Remind Me Later"
            )
        }

        // 2. Sharing Milestone: User saved 3 favorites
        if (favorites >= 3 && !shareDismissed) {
            return AppSuggestion(
                type = SuggestionType.SHARE_APP,
                title = "Share With Friends",
                subtitle = "Share your favorite classic cartoons with friends and family!",
                primaryButtonText = "Share App",
                secondaryButtonText = "Maybe Later"
            )
        }

        // 3. Update Check: Retained user with 8+ sessions
        if (sessions >= 8 && !updateDismissed && (now - lastPrompt >= 14 * 24 * 60 * 60 * 1000L)) {
            return AppSuggestion(
                type = SuggestionType.CHECK_UPDATE,
                title = "Keep Your App Updated",
                subtitle = "You are currently running version $currentAppVersion. Check Google Play for the latest features and bug fixes.",
                primaryButtonText = "Check for Updates",
                secondaryButtonText = "Close"
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
                    // Postpone next prompt by 5 days
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
            val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this awesome app on Google Play: $playStoreUrl"
                )
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (_: Exception) {}
    }
}
```

---

## 4. UI Layer: `SuggestionDialog.kt` (Compose + D-Pad Ready)

The UI component handles rendering across Mobile phones and Android TV:

```kotlin
package com.example.ui.engagement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun SuggestionDialog(
    suggestion: AppSuggestion,
    accentColor: Color = Color(0xFFFFD700), // Gold / Theme accent
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onNeverShowAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    // Crucial for Android TV remote: automatically focus primary action upon opening
    val primaryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(suggestion.type) {
        delay(120)
        try {
            primaryFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E1E24),
            tonalElevation = 10.dp,
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    var closeFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .onFocusChanged { closeFocused = it.isFocused }
                            .background(if (closeFocused) accentColor else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (closeFocused) Color.Black else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Thematic Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (suggestion.type) {
                            SuggestionType.RATE_APP -> Icons.Default.Star
                            SuggestionType.SHARE_APP -> Icons.Default.Share
                            SuggestionType.CHECK_UPDATE -> Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = suggestion.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Description
                Text(
                    text = suggestion.subtitle,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary CTA Button (High prominence + D-Pad focus border)
                var primaryFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    ),
                    border = if (primaryFocused) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .focusRequester(primaryFocusRequester)
                        .onFocusChanged { primaryFocused = it.isFocused }
                ) {
                    Text(
                        text = suggestion.primaryButtonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Button (Outlined / Remind later)
                var secondaryFocused by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = onSecondaryAction,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (secondaryFocused) accentColor else Color.White.copy(alpha = 0.85f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (secondaryFocused) 2.dp else 1.dp,
                        if (secondaryFocused) accentColor else Color.White.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .onFocusChanged { secondaryFocused = it.isFocused }
                ) {
                    Text(
                        text = suggestion.secondaryButtonText,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "Don't ask again" quiet text option
                var neverFocused by remember { mutableStateOf(false) }
                Text(
                    text = "Don't ask again",
                    color = if (neverFocused) accentColor else Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .onFocusChanged { neverFocused = it.isFocused }
                        .clickable(onClick = onNeverShowAgain)
                        .padding(6.dp)
                )
            }
        }
    }
}
```

---

## 5. Screen Integration Pattern (ViewModel / Composable)

In your root or main navigation host (e.g., `MainActivity.kt` or `HomeScreen.kt`):

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val activeSuggestion by viewModel.activeSuggestion.collectAsState()

    // Host screen content
    Box(modifier = Modifier.fillMaxSize()) {
        // App Content here ...

        // Non-intrusive Overlay Dialog
        activeSuggestion?.let { suggestion ->
            SuggestionDialog(
                suggestion = suggestion,
                onPrimaryAction = {
                    viewModel.handleSuggestionAction(suggestion.type, isPositive = true)
                },
                onSecondaryAction = {
                    viewModel.handleSuggestionAction(suggestion.type, isPositive = false)
                },
                onNeverShowAgain = {
                    viewModel.dismissForever(suggestion.type)
                },
                onDismiss = {
                    viewModel.dismissCurrent()
                }
            )
        }
    }
}
```

---

## 6. Checklist for Reusing in Any New Project

- [x] **Copy `EngagementManager.kt`**: Place in `data/engagement/` package.
- [x] **Copy `SuggestionDialog.kt`**: Place in `ui/engagement/` or design system directory.
- [x] **Wire Event Hooks**:
  - `recordAppLaunch()` in `MainActivity.onCreate()` or App startup.
  - `recordContentCompleted()` whenever the user completes a primary task (e.g. finishes audio/video, saves a document, passes a level).
  - `recordFavoriteAdded()` or similar custom milestone.
- [x] **Ensure Safety Flag**: Always pass `isCurrentlyPlaying` (or equivalent busy flag) to prevent interrupting active user actions.
- [x] **Theme Matching**: Pass your app's brand primary/accent color to `accentColor`.
