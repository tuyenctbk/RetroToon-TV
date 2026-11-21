package com.example.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.ArchivalHistorianHelper
import com.example.data.download.DownloadHelper
import com.example.data.model.CartoonItem
import com.example.data.model.CartoonSeries
import com.example.ui.theme.CinemaBorder
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.RetroCream
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CartoonDetailDialog(
    cartoon: CartoonItem,
    relatedSeries: CartoonSeries? = null,
    onDismiss: () -> Unit,
    onPlay: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onToggleFavoriteSeries: ((CartoonSeries) -> Unit)? = null,
    onDownload: ((CartoonItem) -> Unit)? = null,
    onCopyStreamUrl: ((CartoonItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadHelper = remember { DownloadHelper(context) }
    val playFocusRequester = remember { FocusRequester() }
    val archivalNote = remember(cartoon.id) { ArchivalHistorianHelper.buildArchivalNote(cartoon) }

    // On-device zero-cost TextToSpeech Audio Docent
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.getDefault()
                ttsReady = true
            }
        }
        ttsRef.value = instance
        onDispose {
            try {
                instance?.stop()
                instance?.shutdown()
            } catch (_: Exception) {}
        }
    }

    // Optional BYOK & Historian Q&A state
    var showHistorianPanel by remember { mutableStateOf(false) }
    var showByokSettings by remember { mutableStateOf(false) }
    var byokInput by remember { mutableStateOf(ArchivalHistorianHelper.getUserApiKey(context)) }
    var hasPersonalKey by remember { mutableStateOf(ArchivalHistorianHelper.hasUserApiKey(context)) }
    var historianAnswer by remember(cartoon.id) { mutableStateOf<String?>(null) }
    var isAskingHistorian by remember { mutableStateOf(false) }

    LaunchedEffect(cartoon.id) {
        kotlinx.coroutines.delay(140)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWideOrLandscape = maxWidth >= 600.dp || maxHeight <= 520.dp
            var closeFocused by remember { mutableStateOf(false) }
            var playFocused by remember { mutableStateOf(false) }
            var watchlistFocused by remember { mutableStateOf(false) }
            var downloadFocused by remember { mutableStateOf(false) }
            var sourceFocused by remember { mutableStateOf(false) }
            var copyFocused by remember { mutableStateOf(false) }
            var ttsFocused by remember { mutableStateOf(false) }
            var historianToggleFocused by remember { mutableStateOf(false) }

            val playScale by animateFloatAsState(
                targetValue = if (playFocused) 1.04f else 1.0f,
                animationSpec = spring(),
                label = "detail_play_scale"
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CinemaDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = if (isWideOrLandscape) 660.dp else 520.dp)
                    .fillMaxWidth(if (isWideOrLandscape) 0.90f else 0.94f)
                    .heightIn(max = (maxHeight * 0.94f).coerceAtMost(660.dp))
                    .border(1.5.dp, RetroGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .testTag("cartoon_detail_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with adaptive thumbnail height & close button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isWideOrLandscape) {
                                    Modifier.height(175.dp)
                                } else {
                                    Modifier.aspectRatio(16f / 9f)
                                }
                            )
                            .background(CinemaSurface)
                    ) {
                        AsyncImage(
                            model = cartoon.thumbnailUrl,
                            contentDescription = cartoon.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, CinemaDark)
                                    )
                                )
                        )

                        // Close button with 48dp touch target & D-Pad focus highlight
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(48.dp)
                                .onFocusChanged { closeFocused = it.isFocused }
                                .background(
                                    if (closeFocused) RetroGold else Color.Black.copy(alpha = 0.65f),
                                    RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = if (closeFocused) 2.dp else 1.dp,
                                    color = if (closeFocused) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .testTag("detail_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = if (closeFocused) CinemaDark else Color.White
                            )
                        }

                        // Duration / Year badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${cartoon.year} • ${cartoon.runtime}",
                                color = RetroGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text(
                            text = cartoon.title,
                            color = Color.White,
                            fontSize = if (isWideOrLandscape) 19.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 25.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = cartoon.creator,
                            color = RetroGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = RetroRuby.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetroRuby)
                            ) {
                                Text(
                                    text = stringResource(R.string.public_domain_badge),
                                    color = RetroRuby,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                color = CinemaSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.internet_archive),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Synopsis
                        Text(
                            text = cartoon.description.ifBlank {
                                archivalNote.historicalContext
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            maxLines = if (isWideOrLandscape) 3 else 5,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons (Play & Watchlist) with 48dp touch targets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    ttsRef.value?.stop()
                                    isSpeaking = false
                                    onPlay(cartoon)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RetroGold,
                                    contentColor = CinemaDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .graphicsLayer {
                                        scaleX = playScale
                                        scaleY = playScale
                                    }
                                    .focusRequester(playFocusRequester)
                                    .onFocusChanged { playFocused = it.isFocused }
                                    .then(
                                        if (playFocused) {
                                            Modifier.border(2.5.dp, Color.White, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("detail_play_button")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.play_stream_action),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = { onToggleWatchlist(cartoon) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (cartoon.isFavorite || watchlistFocused) RetroGold else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (watchlistFocused) 2.dp else 1.dp,
                                    color = if (watchlistFocused) RetroGold else Color.White.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { watchlistFocused = it.isFocused }
                                    .testTag("detail_watchlist_button")
                            ) {
                                Icon(
                                    imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (cartoon.isFavorite || watchlistFocused) RetroGold else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (cartoon.isFavorite) stringResource(R.string.saved) else stringResource(R.string.nav_watchlist),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // One-tap Save Parent Series to Room 'My Favorites'
                        if (relatedSeries != null && onToggleFavoriteSeries != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            var seriesFavFocused by remember { mutableStateOf(false) }
                            val seriesHeartScale by animateFloatAsState(
                                targetValue = if (relatedSeries.isFavorite) 1.15f else 1.0f,
                                animationSpec = spring(),
                                label = "detail_series_heart_scale"
                            )
                            OutlinedButton(
                                onClick = { onToggleFavoriteSeries(relatedSeries) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (relatedSeries.isFavorite || seriesFavFocused) RetroGold else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (seriesFavFocused || relatedSeries.isFavorite) 1.5.dp else 1.dp,
                                    color = when {
                                        seriesFavFocused -> RetroGold
                                        relatedSeries.isFavorite -> RetroRuby
                                        else -> Color.White.copy(alpha = 0.3f)
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { seriesFavFocused = it.isFocused }
                                    .testTag("detail_save_series_button")
                            ) {
                                Icon(
                                    imageVector = if (relatedSeries.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (relatedSeries.isFavorite) RetroRuby else RetroGold,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            scaleX = seriesHeartScale
                                            scaleY = seriesHeartScale
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (relatedSeries.isFavorite) {
                                        stringResource(R.string.detail_series_saved_label, relatedSeries.title)
                                    } else {
                                        stringResource(R.string.detail_save_series_label, relatedSeries.title)
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Zero-Cost AI Archival Film Historian & On-Device Audio Docent Card
                        Surface(
                            color = CinemaSurfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("archival_historian_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = RetroGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.archival_historian_title),
                                            color = RetroGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // On-Device TTS Audio Narration Button ($0 cost, built-in Android TTS)
                                    OutlinedButton(
                                        onClick = {
                                            val engine = ttsRef.value
                                            if (engine != null && ttsReady) {
                                                if (isSpeaking) {
                                                    engine.stop()
                                                    isSpeaking = false
                                                } else {
                                                    engine.speak(
                                                        archivalNote.spokenSummary,
                                                        TextToSpeech.QUEUE_FLUSH,
                                                        null,
                                                        "historian_${cartoon.id}"
                                                    )
                                                    isSpeaking = true
                                                }
                                            }
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (ttsFocused) 2.dp else 1.dp,
                                            color = if (ttsFocused || isSpeaking) RetroGold else Color.White.copy(alpha = 0.35f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .heightIn(min = 44.dp)
                                            .onFocusChanged { ttsFocused = it.isFocused }
                                            .testTag("detail_tts_historian_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = RetroGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSpeaking) stringResource(R.string.stop_audio) else stringResource(R.string.listen_audio),
                                            color = if (isSpeaking || ttsFocused) RetroGold else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = archivalNote.studioTechnique,
                                    color = RetroCream.copy(alpha = 0.9f),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Toggle Interactive Q&A / Optional BYOK
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (hasPersonalKey) stringResource(R.string.mode_byok) else stringResource(R.string.mode_free_historian),
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 10.sp
                                    )

                                    TextButton(
                                        onClick = { showHistorianPanel = !showHistorianPanel },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .onFocusChanged { historianToggleFocused = it.isFocused }
                                            .testTag("toggle_historian_qa_button")
                                    ) {
                                        Text(
                                            text = if (showHistorianPanel) stringResource(R.string.hide_qa) else stringResource(R.string.ask_historian_byok),
                                            color = if (historianToggleFocused) Color.White else RetroGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showHistorianPanel) {
                                    Column(modifier = Modifier.padding(top = 6.dp)) {
                                        // Quick Question Chips
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val quickPrompts = listOf(
                                                Triple("technique", stringResource(R.string.qa_chip_technique), stringResource(R.string.qa_question_technique)),
                                                Triple("music", stringResource(R.string.qa_chip_music), stringResource(R.string.qa_question_music)),
                                                Triple("public_domain", stringResource(R.string.qa_chip_public_domain), stringResource(R.string.qa_question_public_domain))
                                            )
                                            quickPrompts.forEach { (topicKey, label, fullQ) ->
                                                Surface(
                                                    color = CinemaDark,
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaBorder),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable(enabled = !isAskingHistorian) {
                                                            isAskingHistorian = true
                                                            coroutineScope.launch {
                                                                val res = ArchivalHistorianHelper.askHistorian(
                                                                    context = context,
                                                                    cartoon = cartoon,
                                                                    question = fullQ,
                                                                    topicKey = topicKey
                                                                )
                                                                historianAnswer = res.getOrNull()
                                                                isAskingHistorian = false
                                                            }
                                                        }
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = RetroGold,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (isAskingHistorian) {
                                            Text(
                                                text = stringResource(R.string.consulting_archives),
                                                color = RetroGold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        } else if (!historianAnswer.isNullOrBlank()) {
                                            Surface(
                                                color = CinemaDark.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                Text(
                                                    text = historianAnswer.orEmpty(),
                                                    color = Color.White.copy(alpha = 0.92f),
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }

                                        // Optional BYOK ("Bring Your Own Gemini Key") configuration
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { showByokSettings = !showByokSettings },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Key,
                                                    contentDescription = null,
                                                    tint = RetroGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (hasPersonalKey) stringResource(R.string.update_byok_key) else stringResource(R.string.use_byok_key),
                                                    color = RetroGold.copy(alpha = 0.85f),
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = showByokSettings) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.byok_disclaimer),
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    fontSize = 10.sp,
                                                    lineHeight = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = byokInput,
                                                        onValueChange = { byokInput = it },
                                                        placeholder = {
                                                            Text(stringResource(R.string.byok_hint), fontSize = 11.sp)
                                                        },
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = RetroGold,
                                                            unfocusedBorderColor = CinemaBorder,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Button(
                                                        onClick = {
                                                            ArchivalHistorianHelper.setUserApiKey(context, byokInput)
                                                            hasPersonalKey = ArchivalHistorianHelper.hasUserApiKey(context)
                                                            showByokSettings = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = RetroGold,
                                                            contentColor = CinemaDark
                                                        )
                                                    ) {
                                                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Actions: Download Offline, Archive Link, Copy Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (onDownload != null) {
                                        onDownload(cartoon)
                                    } else {
                                        downloadHelper.downloadCartoon(cartoon, cartoon.getResolvedStreamUrl())
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (downloadFocused) 2.dp else 1.dp,
                                    color = if (downloadFocused) RetroGold else Color.White.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { downloadFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (downloadFocused) RetroGold else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.download),
                                    color = if (downloadFocused) RetroGold else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cartoon.archiveDetailsUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clip.setPrimaryClip(ClipData.newPlainText("Archive Page", cartoon.archiveDetailsUrl))
                                        Toast.makeText(context, context.getString(R.string.archive_url_copied_toast), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (sourceFocused) 2.dp else 1.dp,
                                    color = if (sourceFocused) RetroGold else Color.White.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { sourceFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    tint = if (sourceFocused) RetroGold else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.source),
                                    color = if (sourceFocused) RetroGold else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (onCopyStreamUrl != null) {
                                        onCopyStreamUrl(cartoon)
                                    } else {
                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clip.setPrimaryClip(ClipData.newPlainText("Stream URL", cartoon.getResolvedStreamUrl()))
                                        Toast.makeText(context, context.getString(R.string.stream_link_copied_toast), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (copyFocused) 2.dp else 1.dp,
                                    color = if (copyFocused) RetroGold else Color.White.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { copyFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copyFocused) RetroGold else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.copy),
                                    color = if (copyFocused) RetroGold else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
