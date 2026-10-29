package com.example.ui.player

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.CartoonItem
import com.example.ui.AspectMode
import com.example.ui.FilmEffect
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.RetroCream
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun RetroPlayerScreen(
    cartoon: CartoonItem,
    upNextCartoon: CartoonItem? = null,
    streamUrl: String?,
    subtitleUrl: String? = null,
    isResolving: Boolean,
    isInPipMode: Boolean,
    filmEffect: FilmEffect,
    aspectMode: AspectMode,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onEffectChange: (FilmEffect) -> Unit,
    onAspectChange: (AspectMode) -> Unit,
    onSaveProgress: (String, Long, Long) -> Unit,
    onEnterPip: () -> Unit,
    onRetryStream: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenDonation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val filmEffectFocusRequester = remember { FocusRequester() }
    val nextShortFocusRequester = remember { FocusRequester() }
    val playNextBannerFocusRequester = remember { FocusRequester() }
    var isRootPlayerFocused by remember { mutableStateOf(false) }

    val latestCartoon by rememberUpdatedState(cartoon)
    val latestOnSaveProgress by rememberUpdatedState(onSaveProgress)

    LaunchedEffect(cartoon.id) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(enabled = !isInPipMode) {
        onClose()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember(cartoon.id) { mutableLongStateOf(cartoon.watchProgressMs) }
    var durationMs by remember(cartoon.id) { mutableLongStateOf(cartoon.durationMs) }
    var isBuffering by remember(cartoon.id) { mutableStateOf(true) }
    var isEnded by remember(cartoon.id) { mutableStateOf(false) }
    var playbackError by remember(cartoon.id) { mutableStateOf<String?>(null) }
    var hasAutoRetried by remember(cartoon.id) { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isUserSeeking by remember(cartoon.id) { mutableStateOf(false) }
    var seekSliderPosition by remember(cartoon.id) { mutableFloatStateOf(0f) }
    var areSubtitlesEnabled by remember { mutableStateOf(true) }

    // Gesture animated indicators
    var showRewindGesture by remember { mutableStateOf(false) }
    var showForwardGesture by remember { mutableStateOf(false) }

    // Marathon autoplay countdown
    var marathonCountdown by remember(cartoon.id) { mutableIntStateOf(5) }

    // ExoPlayer initialization with movie audio attributes
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            val audioAttr = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttr, true)
        }
    }

    // Connect streamUrl with rich MediaMetadata & optional sidecar subtitles
    LaunchedEffect(streamUrl, subtitleUrl, cartoon.id) {
        if (!streamUrl.isNullOrBlank()) {
            playbackError = null
            isBuffering = true
            isEnded = false
            val metadata = MediaMetadata.Builder()
                .setTitle(cartoon.title)
                .setArtist(cartoon.creator)
                .setDisplayTitle("${cartoon.title} (${cartoon.year})")
                .setArtworkUri(Uri.parse(cartoon.thumbnailUrl))
                .build()
            val itemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMediaId(cartoon.id)
                .setMediaMetadata(metadata)
            if (!subtitleUrl.isNullOrBlank()) {
                val subMime = if (subtitleUrl.lowercase().endsWith(".vtt")) {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(subMime)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                itemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }
            exoPlayer.setMediaItem(itemBuilder.build())
            exoPlayer.seekTo(cartoon.watchProgressMs.coerceAtLeast(0L))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Automatic backup stream failover on mirror error
    LaunchedEffect(playbackError) {
        if (playbackError != null && !hasAutoRetried) {
            hasAutoRetried = true
            delay(1200)
            onRetryStream()
        }
    }

    // Player events listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    playbackError = null
                    isEnded = false
                    if (durationMs > 0L) {
                        latestOnSaveProgress(latestCartoon.id, exoPlayer.currentPosition.coerceAtLeast(0L), durationMs)
                    }
                } else if (state == Player.STATE_ENDED) {
                    isEnded = true
                    marathonCountdown = 5
                    latestOnSaveProgress(latestCartoon.id, 0L, durationMs)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "Archive mirror busy. Swapping to backup format..."
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            val currentPos = exoPlayer.currentPosition
            val totalDur = exoPlayer.duration.coerceAtLeast(durationMs)
            if (currentPos > 0 && !isEnded) {
                latestOnSaveProgress(latestCartoon.id, currentPos, totalDur)
            }
            exoPlayer.release()
        }
    }

    // Position tracking
    LaunchedEffect(exoPlayer, isPlaying, cartoon.id) {
        while (true) {
            if (isPlaying && !isUserSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(durationMs)
                if (currentPositionMs > 0 && currentPositionMs % 5000L < 1000L) {
                    latestOnSaveProgress(latestCartoon.id, currentPositionMs, durationMs)
                }
            }
            delay(500)
        }
    }

    // Marathon countdown timer & auto-focus primary banner button on TV
    LaunchedEffect(isEnded) {
        if (isEnded) {
            try {
                playNextBannerFocusRequester.requestFocus()
            } catch (_: Exception) {}
            while (marathonCountdown > 0 && isEnded) {
                delay(1000)
                marathonCountdown--
            }
            if (isEnded) {
                onPlayNext()
            }
        }
    }

    // Auto-hide gesture badges
    LaunchedEffect(showRewindGesture) {
        if (showRewindGesture) {
            delay(750)
            showRewindGesture = false
        }
    }
    LaunchedEffect(showForwardGesture) {
        if (showForwardGesture) {
            delay(750)
            showForwardGesture = false
        }
    }

    // Auto-hide OSD controls after idle (only while root video surface has focus, not while navigating toolbar)
    LaunchedEffect(areControlsVisible, lastInteractionTime, isPlaying, isInPipMode, isEnded, isRootPlayerFocused) {
        if (areControlsVisible && isPlaying && !isInPipMode && !isEnded && isRootPlayerFocused) {
            delay(4000)
            areControlsVisible = false
        }
    }

    val resizeMode = when (aspectMode) {
        AspectMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    // Animated projector grain & flicker for vintage film effects
    val filmInfinite = rememberInfiniteTransition(label = "film_shader_transition")
    val grainPhase by filmInfinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grain_phase"
    )
    val projectorFlicker by filmInfinite.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(95, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "projector_flicker"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .focusRequester(focusRequester)
            .onFocusChanged { isRootPlayerFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            showRewindGesture = true
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            showForwardGesture = true
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            onPlayNext()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (isRootPlayerFocused && !isEnded && playbackError == null) {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (isRootPlayerFocused && !isEnded && playbackError == null) {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showRewindGesture = true
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isRootPlayerFocused && !isEnded && playbackError == null) {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showForwardGesture = true
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (isRootPlayerFocused && !isEnded && playbackError == null) {
                                val wasVisible = areControlsVisible
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                if (wasVisible) {
                                    try {
                                        filmEffectFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (isRootPlayerFocused && !isEnded && playbackError == null) {
                                val wasVisible = areControlsVisible
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                if (wasVisible) {
                                    try {
                                        nextShortFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .pointerInput(isInPipMode) {
                if (!isInPipMode) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.4f) {
                                // Double tap left: Rewind 10s
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showRewindGesture = true
                            } else if (offset.x > width * 0.6f) {
                                // Double tap right: Forward 10s
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showForwardGesture = true
                            } else {
                                // Double tap center: Cycle aspect ratio
                                val nextMode = when (aspectMode) {
                                    AspectMode.ORIGINAL -> AspectMode.FIT
                                    AspectMode.FIT -> AspectMode.FILL
                                    AspectMode.FILL -> AspectMode.ORIGINAL
                                }
                                onAspectChange(nextMode)
                            }
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onTap = {
                            areControlsVisible = !areControlsVisible
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    )
                }
            }
            .testTag("player_screen_root")
    ) {
        // Embedded Android Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = resizeMode
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Film Effect Shaders (1930s Sepia Reel, Silver-Screen B&W, Retro CRT)
        if (!isInPipMode) {
            when (filmEffect) {
                FilmEffect.CLEAN -> { /* Clean stream */ }
                FilmEffect.SEPIA_GRAIN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x338B5A2B).copy(alpha = 0.22f * projectorFlicker))
                            .drawBehind {
                                val gradient = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color(0x701A0F05)),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.maxDimension * 0.68f
                                )
                                drawRect(gradient)
                                // Animated 1930s vertical projector scratch lines
                                val scratchX1 = size.width * ((0.22f + grainPhase * 0.53f) % 0.88f + 0.06f)
                                val scratchX2 = size.width * ((0.78f - grainPhase * 0.37f + 1f) % 0.88f + 0.06f)
                                drawLine(
                                    color = RetroCream.copy(alpha = 0.14f * projectorFlicker),
                                    start = Offset(scratchX1, 0f),
                                    end = Offset(scratchX1, size.height),
                                    strokeWidth = 1.2f
                                )
                                drawLine(
                                    color = Color.Black.copy(alpha = 0.18f),
                                    start = Offset(scratchX2, 0f),
                                    end = Offset(scratchX2, size.height),
                                    strokeWidth = 1f
                                )
                            }
                    )
                }
                FilmEffect.BLACK_WHITE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x28181A20))
                            .drawBehind {
                                val vignette = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x10E0E5F0).copy(alpha = 0.08f * projectorFlicker),
                                        Color(0x88050608)
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.maxDimension * 0.66f
                                )
                                drawRect(vignette)
                                val scratchX = size.width * ((0.41f + grainPhase * 0.29f) % 0.85f + 0.07f)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.12f * projectorFlicker),
                                    start = Offset(scratchX, 0f),
                                    end = Offset(scratchX, size.height),
                                    strokeWidth = 1f
                                )
                            }
                    )
                }
                FilmEffect.CRT_GLOW -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val scanlineColor = Color(0x22000000)
                                var y = (grainPhase * 4f) % 4f
                                while (y < size.height) {
                                    drawLine(
                                        color = scanlineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.8f
                                    )
                                    y += 4f
                                }
                                val crtVignette = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x35000000),
                                        Color(0x88000000)
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.maxDimension * 0.64f
                                )
                                drawRect(crtVignette)
                            }
                            .border(8.dp, Color(0xAA08070C), RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        // Mobile Gesture Visual Feedback Pills
        AnimatedVisibility(
            visible = showRewindGesture,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.FastRewind, contentDescription = null, tint = RetroGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        AnimatedVisibility(
            visible = showForwardGesture,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = RetroGold)
                }
            }
        }

        // Error Recovery Overlay
        if (playbackError != null && !isInPipMode) {
            Surface(
                color = CinemaDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetroRuby),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = RetroRuby,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Archive.org Mirror Hiccup",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Switching to backup mirror stream for this public domain short...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRetryStream,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RetroGold,
                            contentColor = CinemaBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Switch to Backup Format", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Marathon Autoplay Banner when finished
        if (isEnded && !isInPipMode) {
            Surface(
                color = CinemaDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, RetroGold),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SHORT CONCLUDED",
                        color = RetroGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Up Next in Marathon in ${marathonCountdown}s",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    upNextCartoon?.let { nextUp ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${nextUp.title} (${nextUp.year})",
                            color = RetroCream.copy(alpha = 0.85f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var isNextBannerFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onPlayNext,
                            colors = ButtonDefaults.buttonColors(containerColor = RetroGold, contentColor = CinemaBlack),
                            border = if (isNextBannerFocused) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                            modifier = Modifier
                                .focusRequester(playNextBannerFocusRequester)
                                .onFocusChanged { isNextBannerFocused = it.isFocused }
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Next Now", fontWeight = FontWeight.Bold)
                        }
                        var isReplayFocused by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                                isEnded = false
                                try {
                                    focusRequester.requestFocus()
                                } catch (_: Exception) {}
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                if (isReplayFocused) 2.dp else 1.dp,
                                if (isReplayFocused) RetroGold else Color.White.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.onFocusChanged { isReplayFocused = it.isFocused }
                        ) {
                            Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Replay", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    var isCoffeeBannerFocused by remember { mutableStateOf(false) }
                    Surface(
                        color = if (isCoffeeBannerFocused) RetroGold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isCoffeeBannerFocused) androidx.compose.foundation.BorderStroke(1.5.dp, RetroGold) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isCoffeeBannerFocused = it.isFocused }
                            .clickable { onOpenDonation() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("☕ Enjoyed this classic short? ", color = Color.White.copy(alpha = 0.8f), fontSize = 11.5.sp)
                            Text("Buy dev a coffee", color = RetroGold, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Loading spinner
        if ((isResolving || isBuffering) && playbackError == null && !isInPipMode && !isEnded) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = RetroGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = CinemaDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isResolving) "Connecting Archive.org Direct Stream..." else "Loading Film Reel...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // OSD Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible && !isInPipMode && !isEnded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            ) {
                // Top Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvFocusableIconButton(
                        onClick = onClose,
                        tag = "player_back_button"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cartoon.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${cartoon.year} • ${cartoon.creator}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = RetroRuby,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PD ARCHIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Buttons (PiP, Filter, Aspect, CC, Rotate) aligned to top-right
                    val isTelevision = remember(context) {
                        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // PiP Button
                        TvFocusableIconButton(
                            onClick = onEnterPip,
                            tag = "player_pip_button"
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = "PiP", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
                            }
                        }

                        // Film Effect Switcher
                        TvFocusableIconButton(
                            onClick = {
                                val nextEffect = when (filmEffect) {
                                    FilmEffect.CLEAN -> FilmEffect.SEPIA_GRAIN
                                    FilmEffect.SEPIA_GRAIN -> FilmEffect.BLACK_WHITE
                                    FilmEffect.BLACK_WHITE -> FilmEffect.CRT_GLOW
                                    FilmEffect.CRT_GLOW -> FilmEffect.CLEAN
                                }
                                onEffectChange(nextEffect)
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier.focusRequester(filmEffectFocusRequester),
                            tag = "player_film_effect_button"
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MovieFilter,
                                    contentDescription = "Vintage Filter",
                                    tint = RetroGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = filmEffect.title.take(5), color = RetroGold, fontSize = 8.sp)
                            }
                        }

                        // Aspect Ratio Toggle
                        TvFocusableIconButton(
                            onClick = {
                                val nextMode = when (aspectMode) {
                                    AspectMode.ORIGINAL -> AspectMode.FIT
                                    AspectMode.FIT -> AspectMode.FILL
                                    AspectMode.FILL -> AspectMode.ORIGINAL
                                }
                                onAspectChange(nextMode)
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            tag = "player_aspect_ratio_button"
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = aspectMode.title.take(4), color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
                            }
                        }

                        // Closed Captions Toggle (shown when sidecar subtitle is resolved)
                        if (!subtitleUrl.isNullOrBlank()) {
                            TvFocusableIconButton(
                                onClick = {
                                    areSubtitlesEnabled = !areSubtitlesEnabled
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !areSubtitlesEnabled)
                                        .build()
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                tag = "player_cc_button"
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "Closed Captions",
                                        tint = if (areSubtitlesEnabled) RetroGold else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (areSubtitlesEnabled) "CC On" else "CC Off",
                                        color = if (areSubtitlesEnabled) RetroGold else Color.White.copy(alpha = 0.7f),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }

                        // Orientation Rotate Button (only on mobile/tablet touch devices, hidden on TV)
                        if (!isTelevision) {
                            TvFocusableIconButton(
                                onClick = {
                                    onToggleOrientation()
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                tag = "player_orientation_button"
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Rotate",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(text = "Rotate", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }

                // Center Controls (Rewind 10s, Play/Pause, Forward 10s) - hidden while spinner is active
                if (!isResolving && !isBuffering) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvFocusableIconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showRewindGesture = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            tag = "player_rewind_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        TvFocusableIconButton(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(RetroGold),
                            tag = "player_play_pause_button"
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = CinemaBlack,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        TvFocusableIconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showForwardGesture = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            tag = "player_forward_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    val totalDuration = if (durationMs > 0) durationMs.toFloat() else 1f
                    val currentPosFloat = if (isUserSeeking) seekSliderPosition else currentPositionMs.toFloat()

                    Slider(
                        value = (currentPosFloat / totalDuration).coerceIn(0f, 1f),
                        onValueChange = { ratio ->
                            isUserSeeking = true
                            seekSliderPosition = ratio * totalDuration
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(seekSliderPosition.toLong())
                            currentPositionMs = seekSliderPosition.toLong()
                            isUserSeeking = false
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = RetroGold,
                            activeTrackColor = RetroGold,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_timeline_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimeMs(if (isUserSeeking) seekSliderPosition.toLong() else currentPositionMs),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Next short in marathon button
                        TvFocusableIconButton(
                            onClick = {
                                onPlayNext()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier.focusRequester(nextShortFocusRequester),
                            tag = "player_next_button"
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Next Short",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Short",
                                    tint = RetroGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = formatTimeMs(durationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvFocusableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "",
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = spring(),
        label = "tv_focus_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(2.dp, RetroGold, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun formatTimeMs(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
