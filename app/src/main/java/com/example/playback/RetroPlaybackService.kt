package com.example.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class RetroPlaybackService : MediaSessionService() {

    private var internalPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var playbackWakeLock: PowerManager.WakeLock? = null

    private val playerStateListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateServiceWakeLock(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val activePlayer = mediaSession?.player
            val isActive = activePlayer != null &&
                activePlayer.playWhenReady &&
                (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
            updateServiceWakeLock(isActive)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ensureNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build().apply {
                setSmallIcon(R.mipmap.ic_launcher)
            }
        setMediaNotificationProvider(notificationProvider)

        val fallbackPlayer = buildServiceExoPlayer()
        internalPlayer = fallbackPlayer

        val activePlayer = attachedUiPlayer ?: fallbackPlayer
        activePlayer.addListener(playerStateListener)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, activePlayer)
            .setId(SESSION_ID)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        updateServiceWakeLock(
            activePlayer.isPlaying ||
                (activePlayer.playWhenReady && activePlayer.playbackState == Player.STATE_BUFFERING)
        )
    }

    private fun buildServiceExoPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(this, renderersFactory)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
                setWakeMode(C.WAKE_MODE_NETWORK)
                playWhenReady = false
            }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager?.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.app_subtitle)
                    setShowBadge(false)
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    private fun updateServiceWakeLock(active: Boolean) {
        try {
            if (active) {
                if (playbackWakeLock == null) {
                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    playbackWakeLock = pm?.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        WAKE_LOCK_TAG
                    )?.apply {
                        setReferenceCounted(false)
                    }
                }
                if (playbackWakeLock?.isHeld == false) {
                    playbackWakeLock?.acquire(MAX_WAKE_LOCK_DURATION_MS)
                }
            } else {
                if (playbackWakeLock?.isHeld == true) {
                    playbackWakeLock?.release()
                }
            }
        } catch (_: Exception) {
            // Ignore if wake lock permission is unavailable in restricted test sandboxes
        }
    }

    private fun swapSessionPlayer(newPlayer: Player?) {
        val session = mediaSession ?: return
        val current = session.player
        val target = newPlayer ?: internalPlayer ?: return
        if (current !== target) {
            current.removeListener(playerStateListener)
            target.addListener(playerStateListener)
            session.player = target
            updateServiceWakeLock(
                target.isPlaying ||
                    (target.playWhenReady && target.playbackState == Player.STATE_BUFFERING)
            )
        } else if (newPlayer == null) {
            updateServiceWakeLock(false)
        }
    }

    private fun detachAndStopIfIdle() {
        swapSessionPlayer(null)
        updateServiceWakeLock(false)
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        updateServiceWakeLock(false)
        mediaSession?.run {
            player.removeListener(playerStateListener)
            release()
            mediaSession = null
        }
        internalPlayer?.run {
            removeListener(playerStateListener)
            release()
        }
        internalPlayer = null
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "retrotoon_playback_channel"
        const val SESSION_ID = "retrotoon_media_session"
        private const val WAKE_LOCK_TAG = "RetroToonTV:MediaPlaybackWakeLock"
        private const val MAX_WAKE_LOCK_DURATION_MS = 4 * 60 * 60 * 1000L // 4 hours max per short/marathon

        @Volatile
        private var instance: RetroPlaybackService? = null

        @Volatile
        private var attachedUiPlayer: ExoPlayer? = null

        /**
         * Attaches the active UI ExoPlayer to the foreground MediaSessionService and returns
         * a MediaController future bound to RetroPlaybackService so Android TV recognizes
         * the active MediaSession and suppresses the system screensaver during playback.
         */
        fun attachPlayerAndBindSession(
            context: Context,
            exoPlayer: ExoPlayer
        ): ListenableFuture<MediaController>? {
            attachedUiPlayer = exoPlayer
            instance?.swapSessionPlayer(exoPlayer)
            return try {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, RetroPlaybackService::class.java)
                )
                MediaController.Builder(context, sessionToken).buildAsync()
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Detaches the UI ExoPlayer before it is released so the MediaSession cleanly
         * reverts to its idle fallback player and tears down any lingering notification.
         */
        fun detachPlayerAndReleaseController(
            exoPlayer: ExoPlayer,
            controllerFuture: ListenableFuture<MediaController>?
        ) {
            if (attachedUiPlayer === exoPlayer) {
                attachedUiPlayer = null
                instance?.detachAndStopIfIdle()
            } else {
                instance?.swapSessionPlayer(null)
            }
            if (controllerFuture != null) {
                try {
                    MediaController.releaseFuture(controllerFuture)
                } catch (_: Exception) {}
            }
        }
    }
}
