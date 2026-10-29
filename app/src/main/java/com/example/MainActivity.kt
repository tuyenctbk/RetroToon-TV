package com.example

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.RetroToonViewModel
import com.example.ui.detail.CartoonDetailDialog
import com.example.ui.donation.DonationDialog
import com.example.ui.engagement.SuggestionDialog
import com.example.ui.home.HomeScreen
import com.example.ui.player.RetroPlayerScreen
import com.example.ui.splash.RetroSplashScreen
import com.example.ui.theme.RetroToonTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RetroToonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetroToonTheme {
                RetroToonApp(
                    viewModel = viewModel,
                    onEnterPip = { enterPipMode() },
                    onToggleOrientation = { toggleOrientation() }
                )
            }
        }
    }

    private fun toggleOrientation() {
        val currentOrientation = resources.configuration.orientation
        requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If actively streaming a cartoon, seamlessly transition to Picture-in-Picture
        if (viewModel.uiState.value.playingItem != null) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setInPipMode(isInPictureInPictureMode)
    }
}

@Composable
fun RetroToonApp(
    viewModel: RetroToonViewModel,
    onEnterPip: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Crossfade(
        targetState = uiState.isSplashVisible,
        animationSpec = tween(600),
        label = "splash_crossfade"
    ) { isSplash ->
        if (isSplash) {
            RetroSplashScreen(
                onFinished = { viewModel.dismissSplash() }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.playingItem != null) {
                    RetroPlayerScreen(
                        cartoon = uiState.playingItem!!,
                        upNextCartoon = uiState.upNextItem,
                        streamUrl = uiState.resolvedStreamUrl,
                        subtitleUrl = uiState.resolvedSubtitleUrl,
                        isResolving = uiState.isResolvingStream,
                        isInPipMode = uiState.isInPipMode,
                        filmEffect = uiState.filmEffect,
                        aspectMode = uiState.aspectMode,
                        onClose = { viewModel.closePlayer() },
                        onPlayNext = { viewModel.playNext() },
                        onEffectChange = { viewModel.setFilmEffect(it) },
                        onAspectChange = { viewModel.setAspectMode(it) },
                        onSaveProgress = { id, pos, dur -> viewModel.saveProgress(id, pos, dur) },
                        onEnterPip = onEnterPip,
                        onRetryStream = { viewModel.retryStreamWithAlternative() },
                        onToggleOrientation = onToggleOrientation,
                        onOpenDonation = { viewModel.openDonationDialog() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.fillMaxSize()
                    ) { _ ->
                        HomeScreen(
                            uiState = uiState,
                            onTabSelected = { viewModel.selectTab(it) },
                            onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                            onPlay = { viewModel.playItem(it) },
                            onSelectDetail = { viewModel.selectDetail(it) },
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                            onSetHero = { viewModel.setHero(it) },
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onClearRecentlyWatched = { viewModel.clearRecentlyWatched() },
                            onRemoveRecentlyWatched = { viewModel.removeRecentlyWatched(it) },
                            onShufflePlay = { viewModel.playRandomShuffle() },
                            onOpenDonation = { viewModel.openDonationDialog() },
                            onDismissDonationBanner = { viewModel.dismissHomeDonationBanner() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Details Modal Dialog
                if (uiState.playingItem == null) {
                    uiState.selectedDetailItem?.let { detailItem ->
                        CartoonDetailDialog(
                            cartoon = detailItem,
                            onDismiss = { viewModel.selectDetail(null) },
                            onPlay = { viewModel.playItem(it) },
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                        )
                    }
                }

                // Donation Dialog (Google Play In-App, PayPal & QR, Ko-fi)
                if (uiState.showDonationDialog) {
                    DonationDialog(
                        donationManager = viewModel.donationManager,
                        isSupporter = uiState.isSupporter,
                        onSupporterStatusUpdated = { viewModel.onSupporterStatusUpdated(it) },
                        onDismiss = { viewModel.closeDonationDialog() }
                    )
                }

                // Smart Suggestion Dialog (Rate / Share / Update)
                if (uiState.playingItem == null && uiState.selectedDetailItem == null && !uiState.showDonationDialog) {
                    uiState.activeSuggestion?.let { suggestion ->
                        SuggestionDialog(
                            suggestion = suggestion,
                            onPrimaryAction = { viewModel.onSuggestionPrimary() },
                            onSecondaryAction = { viewModel.onSuggestionSecondary() },
                            onNeverShowAgain = { viewModel.onSuggestionNever() },
                            onDismiss = { viewModel.dismissSuggestion() }
                        )
                    }
                }
            }
        }
    }
}
