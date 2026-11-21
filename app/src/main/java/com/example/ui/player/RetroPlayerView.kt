package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
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
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.ai.CartoonTriviaAiService
import com.example.data.ai.DidYouKnowFact
import com.example.data.live.LiveTvChannel
import com.example.data.model.CartoonItem
import com.example.playback.RetroPlaybackService
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
    activeLiveChannel: LiveTvChannel? = null,
    liveStartOffsetMs: Long = 0L,
    streamUrl: String?,
    subtitleUrl: String? = null,
    hasAlternativeStream: Boolean = false,
    isResolving: Boolean,
    isInPipMode: Boolean,
    filmEffect: FilmEffect,
    aspectMode: AspectMode,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onChannelSurf: (Int) -> Unit = {},
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
    val hostView = LocalView.current
    val isTelevision = remember(context) {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    }
    val focusRequester = remember { FocusRequester() }
    val filmEffectFocusRequester = remember { FocusRequester() }
    val nextShortFocusRequester = remember { FocusRequester() }
    val playNextBannerFocusRequester = remember { FocusRequester() }
    var isRootPlayerFocused by remember { mutableStateOf(false) }

    // Prevent Android TV screensaver (DreamService) and display sleep while RetroPlayerScreen is active
    DisposableEffect(hostView, context) {
        val prevKeepScreenOn = hostView.keepScreenOn
        hostView.keepScreenOn = true
        val activityWindow = (context as? Activity)?.window
        activityWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            hostView.keepScreenOn = prevKeepScreenOn
            activityWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isLiveTvMode = activeLiveChannel != null
    val latestCartoon by rememberUpdatedState(cartoon)
    val latestIsLiveTvMode by rememberUpdatedState(isLiveTvMode)
    val latestOnPlayNext by rememberUpdatedState(onPlayNext)
    val latestOnSaveProgress by rememberUpdatedState(onSaveProgress)
    val latestOnRetryStream by rememberUpdatedState(onRetryStream)
    val latestHasAlternativeStream by rememberUpdatedState(hasAlternativeStream)

    // Brief animated 2.2s Retro Station Bumper when tuning into a Live TV channel or transitioning live shorts
    var showStationBumper by remember(activeLiveChannel?.id, cartoon.id) {
        mutableStateOf(activeLiveChannel != null)
    }
    LaunchedEffect(activeLiveChannel?.id, cartoon.id) {
        if (activeLiveChannel != null) {
            showStationBumper = true
            delay(2200L)
            showStationBumper = false
        } else {
            showStationBumper = false
        }
    }

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

    // Audio Engine: Optical Loudness Boost + 0-Amplitude Silence Detector + 1920s Theater Piano
    var audioMode by remember { mutableStateOf(PlayerAudioMode.NORMAL) }
    var isSilentFilmAutoPiano by remember(cartoon.id) { mutableStateOf(false) }
    var hasTriedAudioFallback by remember(cartoon.id) { mutableStateOf(false) }
    val silenceDetector = remember { SilenceDetectingAudioProcessor() }
    val ragtimePiano = remember { RagtimePianoSynthesizer() }
    val audioBooster = remember { VintageAudioBooster() }
    val latestAudioMode by rememberUpdatedState(audioMode)

    // Gesture animated indicators
    var showRewindGesture by remember { mutableStateOf(false) }
    var showForwardGesture by remember { mutableStateOf(false) }

    // Optional "Did You Know?" AI Historical Context cards during playback
    var isDidYouKnowEnabled by remember { mutableStateOf(false) }
    var didYouKnowFacts by remember(cartoon.id) {
        mutableStateOf(CartoonTriviaAiService.buildCuratedHistoricalFacts(cartoon))
    }
    var activeFactIndex by remember(cartoon.id) { mutableIntStateOf(0) }

    LaunchedEffect(cartoon.id) {
        didYouKnowFacts = CartoonTriviaAiService.getDidYouKnowFacts(context, cartoon)
        activeFactIndex = 0
    }

    LaunchedEffect(isDidYouKnowEnabled, isPlaying, didYouKnowFacts.size) {
        if (isDidYouKnowEnabled && didYouKnowFacts.size > 1) {
            while (isDidYouKnowEnabled) {
                delay(12_000L)
                if (isPlaying) {
                    activeFactIndex = (activeFactIndex + 1) % didYouKnowFacts.size
                }
            }
        }
    }

    // Marathon autoplay countdown
    var marathonCountdown by remember(cartoon.id) { mutableIntStateOf(5) }

    // ExoPlayer initialization with decoder fallback, silence-detecting AudioSink, and movie audio attributes
    val exoPlayer = remember {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(silenceDetector))
                    .build()
            }
        }.setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                playWhenReady = true
                volume = 1f
                setWakeMode(C.WAKE_MODE_NETWORK)
                setHandleAudioBecomingNoisy(true)
                val audioAttr = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
                setAudioAttributes(audioAttr, true)
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build()
            }
    }

    // Sync volume, optical LoudnessEnhancer (+5.5 dB), and 1920s Ragtime Piano score
    LaunchedEffect(audioMode, isSilentFilmAutoPiano, isPlaying, isBuffering, isEnded, playbackError) {
        exoPlayer.volume = when (audioMode) {
            PlayerAudioMode.BOOST, PlayerAudioMode.NORMAL -> 1f
            PlayerAudioMode.PIANO, PlayerAudioMode.MUTE -> 0f
        }
        audioBooster.update(
            audioSessionId = exoPlayer.audioSessionId,
            enableBoost = audioMode == PlayerAudioMode.BOOST && !isSilentFilmAutoPiano
        )
        val shouldPlayPiano = isPlaying &&
            !isBuffering &&
            !isEnded &&
            playbackError == null &&
            audioMode != PlayerAudioMode.MUTE &&
            (audioMode == PlayerAudioMode.PIANO || isSilentFilmAutoPiano)
        ragtimePiano.setPlaying(shouldPlayPiano)
    }

    var lastPreparedMediaKey by remember { mutableStateOf<String?>(null) }

    // Connect streamUrl with rich MediaMetadata & optional sidecar subtitles
    LaunchedEffect(streamUrl, subtitleUrl, cartoon.id, activeLiveChannel?.id) {
        if (!streamUrl.isNullOrBlank()) {
            val currentMediaKey = "${activeLiveChannel?.id ?: "vod"}:${cartoon.id}"
            val isSameMediaSwitch = lastPreparedMediaKey == currentMediaKey
            val resumePos = when {
                isSameMediaSwitch && currentPositionMs > 0L -> currentPositionMs
                activeLiveChannel != null -> liveStartOffsetMs.coerceAtLeast(0L)
                else -> cartoon.watchProgressMs.coerceAtLeast(0L)
            }
            lastPreparedMediaKey = currentMediaKey
            playbackError = null
            isBuffering = true
            isEnded = false
            isSilentFilmAutoPiano = false
            silenceDetector.resetForNewMedia()
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
            exoPlayer.seekTo(resumePos)
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
                    audioBooster.update(
                        audioSessionId = exoPlayer.audioSessionId,
                        enableBoost = latestAudioMode == PlayerAudioMode.BOOST && !isSilentFilmAutoPiano
                    )
                    if (durationMs > 0L && !latestIsLiveTvMode) {
                        latestOnSaveProgress(latestCartoon.id, exoPlayer.currentPosition.coerceAtLeast(0L), durationMs)
                    }
                } else if (state == Player.STATE_ENDED) {
                    if (latestIsLiveTvMode) {
                        // In 24/7 Live TV mode, seamlessly roll into the next scheduled short
                        latestOnPlayNext()
                    } else {
                        isEnded = true
                        marathonCountdown = 5
                        latestOnSaveProgress(latestCartoon.id, 0L, durationMs)
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (tracks.isEmpty) return
                val hasSelectedAudio = tracks.isTypeSelected(C.TRACK_TYPE_AUDIO)
                if (!hasSelectedAudio) {
                    if (tracks.isTypeSupported(C.TRACK_TYPE_AUDIO)) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .build()
                    } else {
                        // Video container has no supported audio track: try alternate Archive file once if available, and enable silent-era piano
                        isSilentFilmAutoPiano = true
                        if (!hasTriedAudioFallback && latestHasAlternativeStream) {
                            hasTriedAudioFallback = true
                            latestOnRetryStream()
                        }
                    }
                } else if (silenceDetector.hasDetectedAudibleAudio()) {
                    isSilentFilmAutoPiano = false
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioBooster.update(
                    audioSessionId = audioSessionId,
                    enableBoost = latestAudioMode == PlayerAudioMode.BOOST && !isSilentFilmAutoPiano
                )
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = context.getString(R.string.mirror_busy_error)
            }
        }
        exoPlayer.addListener(listener)
        val controllerFuture = RetroPlaybackService.attachPlayerAndBindSession(context, exoPlayer)

        onDispose {
            ragtimePiano.release()
            audioBooster.release()
            exoPlayer.removeListener(listener)
            RetroPlaybackService.detachPlayerAndReleaseController(exoPlayer, controllerFuture)
            val currentPos = exoPlayer.currentPosition
            val totalDur = exoPlayer.duration.coerceAtLeast(durationMs)
            if (currentPos > 0 && !isEnded && !latestIsLiveTvMode) {
                latestOnSaveProgress(latestCartoon.id, currentPos, totalDur)
            }
            exoPlayer.release()
        }
    }

    // Position tracking & silent-era 0-amplitude PCM track detection
    LaunchedEffect(exoPlayer, isPlaying, cartoon.id) {
        while (true) {
            if (isPlaying && !isUserSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(durationMs)
                if (silenceDetector.hasDetectedAudibleAudio()) {
                    if (isSilentFilmAutoPiano) isSilentFilmAutoPiano = false
                } else if (silenceDetector.isConfirmedZeroAmplitudeTrack()) {
                    if (!isSilentFilmAutoPiano) isSilentFilmAutoPiano = true
                }
                if (!latestIsLiveTvMode && currentPositionMs > 0 && currentPositionMs % 5000L < 1000L) {
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

    // Auto-hide OSD controls after idle (always auto-hide on touch devices; on TV pause auto-hide while navigating toolbar)
    LaunchedEffect(
        areControlsVisible,
        lastInteractionTime,
        isPlaying,
        isInPipMode,
        isEnded,
        isRootPlayerFocused,
        isUserSeeking,
        isTelevision
    ) {
        val shouldAutoHide = !isTelevision || isRootPlayerFocused
        if (areControlsVisible && isPlaying && !isInPipMode && !isEnded && !isUserSeeking && shouldAutoHide) {
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
                            if (isLiveTvMode) {
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showRewindGesture = true
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            if (isLiveTvMode) {
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                                showForwardGesture = true
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            onPlayNext()
                            true
                        }
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            if (isLiveTvMode) {
                                onChannelSurf(1)
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            if (isLiveTvMode) {
                                onChannelSurf(-1)
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_VOLUME_MUTE -> {
                            audioMode = if (audioMode == PlayerAudioMode.MUTE) PlayerAudioMode.BOOST else PlayerAudioMode.MUTE
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
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
                                if (isLiveTvMode) {
                                    onChannelSurf(-1)
                                } else {
                                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    currentPositionMs = newPos
                                    showRewindGesture = true
                                }
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isRootPlayerFocused && !isEnded && playbackError == null) {
                                if (isLiveTvMode) {
                                    onChannelSurf(1)
                                } else {
                                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(newPos)
                                    currentPositionMs = newPos
                                    showForwardGesture = true
                                }
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
                        KeyEvent.KEYCODE_BACK -> {
                            onClose()
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
                                if (latestIsLiveTvMode) {
                                    onChannelSurf(-1)
                                } else {
                                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    currentPositionMs = newPos
                                    showRewindGesture = true
                                }
                            } else if (offset.x > width * 0.6f) {
                                if (latestIsLiveTvMode) {
                                    onChannelSurf(1)
                                } else {
                                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(newPos)
                                    currentPositionMs = newPos
                                    showForwardGesture = true
                                }
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
        // Embedded Android Video View with explicit keepScreenOn for Android TV / Google TV
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = resizeMode
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
                view.keepScreenOn = isPlaying || isBuffering || isResolving || isEnded
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
                    Text(stringResource(R.string.rewind_10s), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    Text(stringResource(R.string.forward_10s), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        text = stringResource(R.string.mirror_hiccup_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.mirror_hiccup_desc),
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
                        Text(stringResource(R.string.switch_backup), fontWeight = FontWeight.Bold)
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
                        text = stringResource(R.string.short_concluded),
                        color = RetroGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.up_next_in_countdown, marathonCountdown),
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
                            Text(stringResource(R.string.play_next_now), fontWeight = FontWeight.Bold)
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
                            Text(stringResource(R.string.replay), color = Color.White)
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
                            Text(stringResource(R.string.buy_dev_coffee), color = Color.White.copy(alpha = 0.8f), fontSize = 11.5.sp)
                            Text(stringResource(R.string.buy_dev_coffee_action), color = RetroGold, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Animated 24/7 Live TV Retro Station Bumper Slate
        AnimatedVisibility(
            visible = showStationBumper && activeLiveChannel != null && !isInPipMode,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(420)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 24.dp, top = 74.dp)
        ) {
            activeLiveChannel?.let { liveCh ->
                val chAccent = Color(liveCh.accentHex)
                Surface(
                    color = CinemaDark.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, chAccent),
                    modifier = Modifier.testTag("player_live_station_bumper")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = RetroRuby,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.live_ch_short, liveCh.number),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = stringResource(R.string.live_badge),
                                    color = RetroGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(liveCh.nameRes),
                                color = chAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = cartoon.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            upNextCartoon?.let { next ->
                                Text(
                                    text = "${stringResource(R.string.live_up_next_label)}: ${next.title}",
                                    color = RetroCream.copy(alpha = 0.75f),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                        text = if (isResolving) stringResource(R.string.connecting_archive) else stringResource(R.string.loading_film_reel),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Optional "Did You Know?" Historical Context Card during video playback
        AnimatedVisibility(
            visible = isDidYouKnowEnabled && !isInPipMode && !isEnded && playbackError == null && didYouKnowFacts.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = if (areControlsVisible) 96.dp else 28.dp
                )
        ) {
            val currentFact = didYouKnowFacts[activeFactIndex % didYouKnowFacts.size]
            Surface(
                color = CinemaDark.copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .testTag("player_did_you_know_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = RetroGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.did_you_know_header, currentFact.categoryBadge),
                                color = RetroGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.next_fact),
                                color = RetroCream,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        activeFactIndex = (activeFactIndex + 1) % didYouKnowFacts.size
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("player_did_you_know_next")
                            )
                            Text(
                                text = "✕",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { isDidYouKnowEnabled = false }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("player_did_you_know_close")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentFact.factText,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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
                            contentDescription = stringResource(R.string.close_player),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeLiveChannel?.let { liveCh ->
                                Surface(
                                    color = RetroRuby,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("player_live_badge")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${stringResource(R.string.live_ch_short, liveCh.number)} • ${stringResource(R.string.live_badge)}",
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Text(
                                text = cartoon.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeLiveChannel != null) {
                                    "${stringResource(activeLiveChannel.nameRes)} • ${cartoon.year}"
                                } else {
                                    "${cartoon.year} • ${cartoon.creator}"
                                },
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
                                    text = stringResource(R.string.pd_archive_badge),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            if ((isSilentFilmAutoPiano || audioMode == PlayerAudioMode.PIANO) && audioMode != PlayerAudioMode.MUTE) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = RetroGold.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.75.dp, RetroGold)
                                ) {
                                    Text(
                                        text = stringResource(R.string.piano_score_badge),
                                        color = RetroGold,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons (Audio, PiP, Filter, Aspect, CC, Rotate) aligned to top-right
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio Remaster / Boost / Piano / Mute Toggle
                        TvFocusableIconButton(
                            onClick = {
                                audioMode = when (audioMode) {
                                    PlayerAudioMode.BOOST -> PlayerAudioMode.NORMAL
                                    PlayerAudioMode.NORMAL -> PlayerAudioMode.PIANO
                                    PlayerAudioMode.PIANO -> PlayerAudioMode.MUTE
                                    PlayerAudioMode.MUTE -> PlayerAudioMode.BOOST
                                }
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            tag = "player_audio_mode_button"
                        ) {
                            val audioTint = when (audioMode) {
                                PlayerAudioMode.BOOST, PlayerAudioMode.PIANO -> RetroGold
                                PlayerAudioMode.NORMAL -> Color.White
                                PlayerAudioMode.MUTE -> RetroRuby
                            }
                            val audioIcon = when (audioMode) {
                                PlayerAudioMode.BOOST -> Icons.Default.GraphicEq
                                PlayerAudioMode.NORMAL -> Icons.AutoMirrored.Filled.VolumeUp
                                PlayerAudioMode.PIANO -> Icons.Default.MusicNote
                                PlayerAudioMode.MUTE -> Icons.AutoMirrored.Filled.VolumeOff
                            }
                            val audioLabel = stringResource(audioMode.labelRes)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = audioIcon,
                                    contentDescription = stringResource(R.string.audio_mode_label, audioLabel),
                                    tint = audioTint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = audioLabel, color = audioTint, fontSize = 8.sp)
                            }
                        }

                        // "Did You Know?" AI Historical Context Cards Toggle
                        TvFocusableIconButton(
                            onClick = {
                                isDidYouKnowEnabled = !isDidYouKnowEnabled
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            tag = "player_did_you_know_toggle"
                        ) {
                            val factTint = if (isDidYouKnowEnabled) RetroGold else Color.White.copy(alpha = 0.85f)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = stringResource(R.string.did_you_know_toggle_desc),
                                    tint = factTint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isDidYouKnowEnabled) stringResource(R.string.facts_on) else stringResource(R.string.facts_off),
                                    color = factTint,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        // PiP Button
                        TvFocusableIconButton(
                            onClick = onEnterPip,
                            tag = "player_pip_button"
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = stringResource(R.string.pip_desc),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = stringResource(R.string.pip_label), color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
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
                                    contentDescription = stringResource(R.string.vintage_filter_desc),
                                    tint = RetroGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = stringResource(filmEffect.titleRes).take(5), color = RetroGold, fontSize = 8.sp)
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
                                    contentDescription = stringResource(R.string.aspect_ratio_desc),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = stringResource(aspectMode.titleRes).take(4), color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
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
                                        contentDescription = stringResource(R.string.closed_captions_desc),
                                        tint = if (areSubtitlesEnabled) RetroGold else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (areSubtitlesEnabled) stringResource(R.string.cc_on) else stringResource(R.string.cc_off),
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
                                        contentDescription = stringResource(R.string.rotate_label),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(text = stringResource(R.string.rotate_label), color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
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
                                contentDescription = stringResource(R.string.rewind_10s),
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
                                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
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
                                contentDescription = stringResource(R.string.forward_10s),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline Scrubber (VOD) or Live Broadcast Bar (24/7 Live TV)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    val totalDuration = if (durationMs > 0) durationMs.toFloat() else 1f
                    val currentPosFloat = if (isUserSeeking) seekSliderPosition else currentPositionMs.toFloat()

                    if (isLiveTvMode) {
                        LinearProgressIndicator(
                            progress = { (currentPosFloat / totalDuration).coerceIn(0f, 1f) },
                            color = RetroRuby,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("player_live_progress")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TvFocusableIconButton(
                                    onClick = {
                                        onChannelSurf(-1)
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    tag = "player_channel_down_button"
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.live_ch_prev),
                                            tint = RetroGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.live_ch_prev),
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                TvFocusableIconButton(
                                    onClick = {
                                        onChannelSurf(1)
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier
                                        .focusRequester(nextShortFocusRequester)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    tag = "player_channel_up_button"
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.live_ch_next),
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = stringResource(R.string.live_ch_next),
                                            tint = RetroGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            upNextCartoon?.let { next ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(start = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = RetroGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${stringResource(R.string.live_up_next_label)}: ${next.title}",
                                        color = RetroCream.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
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
                                        text = stringResource(R.string.next_short),
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = stringResource(R.string.next_short),
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
