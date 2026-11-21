package com.example.ui.favorites

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CartoonItem
import com.example.data.model.CartoonSeries
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
fun SeriesCollectionDialog(
    series: CartoonSeries,
    episodes: List<CartoonItem>,
    onDismiss: () -> Unit,
    onPlayEpisode: (CartoonItem) -> Unit,
    onPlayMarathon: (CartoonSeries) -> Unit,
    onToggleFavoriteSeries: (CartoonSeries) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit
) {
    val accent = Color(series.accentHex)
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(series.id) {
        delay(120)
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 600.dp
            var playFocused by remember { mutableStateOf(false) }
            var favFocused by remember { mutableStateOf(false) }
            var closeFocused by remember { mutableStateOf(false) }
            val heartTint by animateColorAsState(
                targetValue = if (series.isFavorite) RetroRuby else Color.White,
                label = "series_dialog_heart"
            )
            val heartScale by animateFloatAsState(
                targetValue = if (series.isFavorite) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "series_dialog_heart_scale"
            )

            Surface(
                color = CinemaDark,
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 10.dp,
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f)),
                modifier = Modifier
                    .widthIn(max = if (isWide) 680.dp else 520.dp)
                    .fillMaxWidth(if (isWide) 0.90f else 0.95f)
                    .heightIn(max = (maxHeight * 0.92f).coerceAtMost(680.dp))
                    .testTag("series_collection_dialog")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWide) 170.dp else 155.dp)
                            .background(CinemaSurface)
                    ) {
                        AsyncImage(
                            model = series.thumbnailUrl,
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.25f),
                                            CinemaDark.copy(alpha = 0.85f),
                                            CinemaDark
                                        )
                                    )
                                )
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(48.dp)
                                .onFocusChanged { closeFocused = it.isFocused }
                                .background(
                                    if (closeFocused) RetroGold else Color.Black.copy(alpha = 0.65f),
                                    CircleShape
                                )
                                .border(
                                    width = if (closeFocused) 2.dp else 1.dp,
                                    color = if (closeFocused) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .testTag("series_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = if (closeFocused) CinemaBlack else Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = accent,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = series.era,
                                        color = CinemaBlack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${series.studio} • ${stringResource(R.string.shorts_count, episodes.size.coerceAtLeast(series.episodeCount))}",
                                    color = RetroGold,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = series.title,
                                color = Color.White,
                                fontSize = if (isWide) 22.sp else 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Description + Primary Actions
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = series.description,
                            color = RetroCream.copy(alpha = 0.82f),
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onPlayMarathon(series) },
                                enabled = episodes.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RetroGold,
                                    contentColor = CinemaBlack
                                ),
                                border = if (playFocused) BorderStroke(2.dp, Color.White) else null,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .focusRequester(playFocusRequester)
                                    .onFocusChanged { playFocused = it.isFocused }
                                    .testTag("series_dialog_marathon_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.play_marathon),
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = { onToggleFavoriteSeries(series) },
                                border = BorderStroke(
                                    width = if (favFocused || series.isFavorite) 2.dp else 1.dp,
                                    color = when {
                                        favFocused -> RetroGold
                                        series.isFavorite -> RetroRuby
                                        else -> Color.White.copy(alpha = 0.4f)
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { favFocused = it.isFocused }
                                    .testTag("series_dialog_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = heartTint,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            scaleX = heartScale
                                            scaleY = heartScale
                                        }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        if (series.isFavorite) R.string.saved_in_favorites else R.string.save_series
                                    ),
                                    color = if (series.isFavorite) RetroGold else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Episodes List
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(episodes, key = { "series_ep_${it.id}" }) { episode ->
                            var rowFocused by remember { mutableStateOf(false) }
                            Surface(
                                color = if (rowFocused) CinemaSurfaceVariant else CinemaSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (rowFocused) 2.dp else 1.dp,
                                    color = if (rowFocused) RetroGold else CinemaBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .onFocusChanged { rowFocused = it.isFocused }
                                    .clickable { onPlayEpisode(episode) }
                                    .testTag("series_episode_row_${episode.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(CinemaDark)
                                    ) {
                                        AsyncImage(
                                            model = episode.thumbnailUrl,
                                            contentDescription = episode.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = RetroGold,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = episode.title,
                                            color = if (rowFocused) RetroGold else Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${episode.year} • ${episode.runtime} • ${episode.creator}",
                                            color = RetroCream.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onToggleWatchlist(episode) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (episode.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = stringResource(R.string.nav_favorites),
                                            tint = if (episode.isFavorite) RetroGold else Color.White.copy(alpha = 0.75f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
