package com.example.ui.live

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.live.LiveChannelProgram
import com.example.data.live.LiveTvChannel
import com.example.data.live.LiveTvChannelEngine
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaBorder
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.RetroCream
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby
import kotlinx.coroutines.delay

@Composable
fun LiveTvGuideScreen(
    channels: List<LiveTvChannel>,
    isWideScreen: Boolean,
    onTuneChannel: (LiveTvChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    // Refresh live EPG clock every 15 seconds so progress bars & "Up Next" stay synchronized
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val programs = remember(channels, nowMs) {
        channels.mapNotNull { ch -> LiveTvChannelEngine.getCurrentProgram(ch, nowMs) }
    }

    val firstChannelFocus = remember { FocusRequester() }
    LaunchedEffect(isWideScreen, programs.isNotEmpty()) {
        if (isWideScreen && programs.isNotEmpty()) {
            delay(220)
            try {
                firstChannelFocus.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val liveDotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .testTag("live_tv_guide_list"),
        contentPadding = PaddingValues(
            horizontal = if (isWideScreen) 28.dp else 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Broadcast Header Banner
        item {
            Surface(
                color = CinemaDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RetroGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    RetroRuby.copy(alpha = 0.22f),
                                    CinemaDark,
                                    RetroGold.copy(alpha = 0.14f)
                                )
                            )
                        )
                        .padding(if (isWideScreen) 20.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = RetroRuby,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = liveDotAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = stringResource(R.string.live_on_air_badge),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.live_guide_subtitle_tag),
                                    color = RetroGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.live_guide_title),
                                color = Color.White,
                                fontSize = if (isWideScreen) 22.sp else 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.live_guide_description),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        }

                        if (isWideScreen && programs.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(16.dp))
                            var heroWatchFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { onTuneChannel(programs.first().channel) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RetroGold,
                                    contentColor = CinemaBlack
                                ),
                                border = if (heroWatchFocused) BorderStroke(2.dp, Color.White) else null,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .onFocusChanged { heroWatchFocused = it.isFocused }
                                    .testTag("live_watch_now_button")
                            ) {
                                Icon(imageVector = Icons.Default.LiveTv, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.live_tune_in_now),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Channel EPG Cards
        itemsIndexed(programs, key = { _, p -> p.channel.id }) { index, program ->
            LiveChannelEpgCard(
                program = program,
                isWideScreen = isWideScreen,
                liveDotAlpha = liveDotAlpha,
                onClick = { onTuneChannel(program.channel) },
                modifier = if (index == 0) Modifier.focusRequester(firstChannelFocus) else Modifier
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiveChannelEpgCard(
    program: LiveChannelProgram,
    isWideScreen: Boolean,
    liveDotAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "channel_card_scale"
    )
    val accentColor = Color(program.channel.accentHex)
    val remainingMins = ((program.remainingMs / 60_000L) + 1L).coerceAtLeast(1L)

    Surface(
        color = if (isFocused) CinemaSurface else CinemaDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) RetroGold else CinemaBorder
        ),
        tonalElevation = if (isFocused) 10.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .testTag("live_channel_card_${program.channel.number}")
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ChannelBadgeBlock(
                    channelNumber = program.channel.number,
                    accentColor = accentColor,
                    liveDotAlpha = liveDotAlpha
                )

                // Live Thumbnail
                Box(
                    modifier = Modifier
                        .width(154.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CinemaSurfaceVariant)
                ) {
                    AsyncImage(
                        model = program.currentItem.thumbnailUrl,
                        contentDescription = program.currentItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = if (isFocused) RetroGold else Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.live_tune_in_now),
                                    tint = if (isFocused) CinemaBlack else RetroGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Program Info & Schedule
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(program.channel.nameRes),
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "• ${stringResource(program.channel.taglineRes)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = program.currentItem.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { program.progressFraction },
                            color = accentColor,
                            trackColor = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = RetroCream.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.live_remaining_min, remainingMins),
                                color = RetroCream.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = CinemaSurfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.live_up_next_label),
                                color = RetroGold,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${program.upNextItem.title} (${program.upNextItem.year})",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            // Mobile Compact Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CinemaSurfaceVariant)
                    ) {
                        AsyncImage(
                            model = program.currentItem.thumbnailUrl,
                            contentDescription = program.currentItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = RetroRuby,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.live_ch_short, program.channel.number),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(RetroRuby.copy(alpha = liveDotAlpha))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(program.channel.nameRes),
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = program.currentItem.title,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.live_remaining_min, remainingMins),
                            color = RetroCream.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { program.progressFraction },
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.live_up_next_label),
                        color = RetroGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = program.upNextItem.title,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelBadgeBlock(
    channelNumber: Int,
    accentColor: Color,
    liveDotAlpha: Float
) {
    Surface(
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
        modifier = Modifier.size(width = 74.dp, height = 74.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.live_ch_prefix),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = String.format("%02d", channelNumber),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(RetroRuby.copy(alpha = liveDotAlpha))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.live_badge),
                    color = RetroRuby,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
