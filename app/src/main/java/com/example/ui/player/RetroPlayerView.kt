package com.example.ui.player

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
    streamUrl: String?,
    isResolving: Boolean,
    isInPipMode: Boolean,
    filmEffect: FilmEffect,
    aspectMode: AspectMode,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onEffectChange: (FilmEffect) -> Unit,
    onAspectChange: (AspectMode) -> Unit,
    onSaveProgress: (String, Long) -> Unit,
    onEnterPip: () -> Unit,
    onRetryStream: () -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = !isInPipMode) {
        onClose()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(cartoon.watchProgressMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekSliderPosition by remember { mutableFloatStateOf(0f) }

    // Gesture animated indicators
    var showRewindGesture by remember { mutableStateOf(false) }
    var showForwardGesture by remember { mutableStateOf(false) }

    // Marathon autoplay countdown
    var marathonCountdown by remember { mutableIntStateOf(5) }

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

    // Connect streamUrl
    LaunchedEffect(streamUrl) {
        if (!streamUrl.isNullOrBlank()) {
            playbackError = null
            isBuffering = true
            isEnded = false
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            exoPlayer.setMediaItem(mediaItem)
            if (cartoon.watchProgressMs > 0) {
                exoPlayer.seekTo(cartoon.watchProgressMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
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
                } else if (state == Player.STATE_ENDED) {
                    isEnded = true
                    marathonCountdown = 5
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
            if (currentPos > 0) {
                onSaveProgress(cartoon.id, currentPos)
            }
            exoPlayer.release()
        }
    }

    // Position tracking
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (isPlaying && !isUserSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (currentPositionMs > 0 && currentPositionMs % 5000L < 1000L) {
                    onSaveProgress(cartoon.id, currentPositionMs)
                }
            }
            delay(500)
        }
    }

    // Marathon countdown timer
    LaunchedEffect(isEnded) {
        if (isEnded) {
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

    // Auto-hide OSD controls after idle (unless paused, ended, or in PiP)
    LaunchedEffect(areControlsVisible, lastInteractionTime, isPlaying, isInPipMode, isEnded) {
        if (areControlsVisible && isPlaying && !isInPipMode && !isEnded) {
            delay(4000)
            areControlsVisible = false
        }
    }

    val resizeMode = when (aspectMode) {
        AspectMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            showRewindGesture = true
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
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
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                            true
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
                                // Double tap center: Toggle aspect ratio
                                val nextMode = if (aspectMode == AspectMode.ORIGINAL) AspectMode.FILL else AspectMode.ORIGINAL
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

        // Film Effect Shaders
        if (!isInPipMode) {
            when (filmEffect) {
                FilmEffect.CLEAN -> { /* Clean stream */ }
                FilmEffect.SEPIA_GRAIN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x35825220))
                            .drawBehind {
                                val gradient = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color(0x60000000)),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.maxDimension * 0.7f
                                )
                                drawRect(gradient)
                            }
                    )
                }
                FilmEffect.BLACK_WHITE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x40101015))
                            .drawBehind {
                                drawRect(Color(0x20000000))
                            }
                    )
                }
                FilmEffect.CRT_GLOW -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val scanlineColor = Color(0x15000000)
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = scanlineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 2f
                                    )
                                    y += 4f
                                }
                            }
                            .border(12.dp, Color(0x90000000))
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onPlayNext,
                            colors = ButtonDefaults.buttonColors(containerColor = RetroGold, contentColor = CinemaBlack)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Next Now", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                                isEnded = false
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Replay")
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
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvFocusableIconButton(
                        onClick = onClose,
                        tag = "player_back_button"
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${cartoon.year} • ${cartoon.creator}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = RetroRuby,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PD ARCHIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Buttons (PiP, Filter, Aspect, Rotate) in a horizontally scrollable row
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(scrollState),
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
                                    AspectMode.ORIGINAL -> AspectMode.FILL
                                    AspectMode.FILL -> AspectMode.ORIGINAL
                                    else -> AspectMode.ORIGINAL
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

                        // Orientation Rotate Button
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

                // Center Controls (Rewind 10s, Play/Pause, Forward 10s)
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

                // Bottom Timeline Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
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
            .focusable()
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
