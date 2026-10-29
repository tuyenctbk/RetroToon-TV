package com.example.ui.engagement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import com.example.data.engagement.AppSuggestion
import com.example.data.engagement.SuggestionType
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby

@Composable
fun SuggestionDialog(
    suggestion: AppSuggestion,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onNeverShowAgain: () -> Unit,
    onDismiss: () -> Unit
) {
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
            color = CinemaDark,
            tonalElevation = 10.dp,
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .border(1.dp, CinemaSurfaceVariant, RoundedCornerShape(18.dp))
                .testTag("suggestion_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close icon
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
                            .background(if (closeFocused) RetroGold else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (closeFocused) CinemaBlack else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Visual Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            when (suggestion.type) {
                                SuggestionType.RATE_APP -> RetroGold.copy(alpha = 0.2f)
                                SuggestionType.SHARE_APP -> RetroRuby.copy(alpha = 0.2f)
                                SuggestionType.CHECK_UPDATE -> Color.White.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (suggestion.type) {
                            SuggestionType.RATE_APP -> Icons.Default.Star
                            SuggestionType.SHARE_APP -> Icons.Default.Share
                            SuggestionType.CHECK_UPDATE -> Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = when (suggestion.type) {
                            SuggestionType.RATE_APP -> RetroGold
                            SuggestionType.SHARE_APP -> RetroRuby
                            SuggestionType.CHECK_UPDATE -> Color.White
                        },
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

                // Subtitle
                Text(
                    text = suggestion.subtitle,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action Button
                var primaryFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroGold,
                        contentColor = CinemaBlack
                    ),
                    border = if (primaryFocused) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .focusRequester(primaryFocusRequester)
                        .onFocusChanged { primaryFocused = it.isFocused }
                        .testTag("suggestion_primary_button")
                ) {
                    Text(
                        text = suggestion.primaryButtonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Button
                var secondaryFocused by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = onSecondaryAction,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (secondaryFocused) RetroGold else Color.White.copy(alpha = 0.85f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (secondaryFocused) 2.dp else 1.dp,
                        if (secondaryFocused) RetroGold else Color.White.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .onFocusChanged { secondaryFocused = it.isFocused }
                        .testTag("suggestion_secondary_button")
                ) {
                    Text(
                        text = suggestion.secondaryButtonText,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "Don't show again" text option
                var neverFocused by remember { mutableStateOf(false) }
                Text(
                    text = "Don't ask again",
                    color = if (neverFocused) RetroGold else Color.White.copy(alpha = 0.45f),
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
