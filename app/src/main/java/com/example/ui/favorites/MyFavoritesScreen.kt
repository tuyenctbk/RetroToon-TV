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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CartoonItem
import com.example.data.model.CartoonSeries
import com.example.ui.home.CartoonPosterCard
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
fun MyFavoritesScreen(
    favoriteSeries: List<CartoonSeries>,
    recommendedSeries: List<CartoonSeries>,
    savedEpisodes: List<CartoonItem>,
    isWideScreen: Boolean,
    onOpenSeries: (CartoonSeries) -> Unit,
    onPlaySeriesMarathon: (CartoonSeries) -> Unit,
    onToggleFavoriteSeries: (CartoonSeries) -> Unit,
    onPlayEpisode: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onPlayFavoritesMarathon: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAnyFavorites = favoriteSeries.isNotEmpty() || savedEpisodes.isNotEmpty()
    val horizontalPad = if (isWideScreen) 28.dp else 16.dp
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isWideScreen, hasAnyFavorites) {
        if (isWideScreen) {
            delay(180)
            try {
                primaryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .testTag("my_favorites_screen"),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 44.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Theater Favorites Header Banner
        item {
            Surface(
                color = CinemaDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RetroGold.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPad)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    RetroRuby.copy(alpha = 0.24f),
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
                                    color = RetroRuby.copy(alpha = 0.22f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, RetroRuby),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = RetroRuby,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.my_favorites_title),
                                    color = Color.White,
                                    fontSize = if (isWideScreen) 22.sp else 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.my_favorites_summary,
                                    favoriteSeries.size,
                                    savedEpisodes.size
                                ),
                                color = RetroCream.copy(alpha = 0.8f),
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }

                        if (hasAnyFavorites) {
                            Spacer(modifier = Modifier.width(12.dp))
                            var marathonFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = onPlayFavoritesMarathon,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RetroGold,
                                    contentColor = CinemaBlack
                                ),
                                border = if (marathonFocused) BorderStroke(2.dp, Color.White) else null,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .focusRequester(primaryFocusRequester)
                                    .onFocusChanged { marathonFocused = it.isFocused }
                                    .testTag("favorites_marathon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.play_marathon),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Saved Favorite Cartoon Series (Room `favorite_series` table)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = null,
                            tint = RetroGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.favorite_series_section_title),
                            color = Color.White,
                            fontSize = if (isWideScreen) 18.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.series_count_label, favoriteSeries.size),
                        color = RetroGold,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (favoriteSeries.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontalPad),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.testTag("favorite_series_row")
                    ) {
                        items(favoriteSeries, key = { "fav_series_${it.id}" }) { series ->
                            CartoonSeriesCard(
                                series = series,
                                isWideScreen = isWideScreen,
                                onOpenSeries = { onOpenSeries(series) },
                                onPlayMarathon = { onPlaySeriesMarathon(series) },
                                onToggleFavorite = { onToggleFavoriteSeries(series) },
                                modifier = Modifier.width(if (isWideScreen) 275.dp else 240.dp)
                            )
                        }
                    }
                } else {
                    // Empty Favorite Series card with Quick-Add classic series strip
                    Surface(
                        color = CinemaDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CinemaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad)
                            .testTag("empty_favorite_series_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = RetroGold,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_favorite_series_title),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.empty_favorite_series_subtitle),
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center
                            )

                            if (recommendedSeries.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.quick_add_series_label),
                                    color = RetroGold,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(recommendedSeries, key = { "quick_${it.id}" }) { rec ->
                                        QuickAddSeriesChip(
                                            series = rec,
                                            onAdd = { onToggleFavoriteSeries(rec) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Saved Shorts / Watchlist (Room `watchlist` table)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = RetroGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.my_saved_cartoons, savedEpisodes.size),
                            color = Color.White,
                            fontSize = if (isWideScreen) 18.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (savedEpisodes.isNotEmpty()) {
                    val cardWidth = if (isWideScreen) 230.dp else 184.dp
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontalPad),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.testTag("saved_episodes_row")
                    ) {
                        items(savedEpisodes, key = { "fav_ep_${it.id}" }) { item ->
                            CartoonPosterCard(
                                cartoon = item,
                                isWideScreen = isWideScreen,
                                onClick = { onSelectDetail(item) },
                                onPlay = { onPlayEpisode(item) },
                                onToggleWatchlist = { onToggleWatchlist(item) },
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                } else {
                    Surface(
                        color = CinemaDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CinemaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.empty_watchlist_title),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.empty_watchlist_subtitle),
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            var browseFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = onExploreClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RetroGold,
                                    contentColor = CinemaBlack
                                ),
                                border = if (browseFocused) BorderStroke(2.dp, Color.White) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .then(if (!hasAnyFavorites) Modifier.focusRequester(primaryFocusRequester) else Modifier)
                                    .onFocusChanged { browseFocused = it.isFocused }
                                    .testTag("favorites_browse_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.browse_cartoons),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddSeriesChip(
    series: CartoonSeries,
    onAdd: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Color(series.accentHex)
    Surface(
        color = if (focused) RetroGold else CinemaSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) Color.White else accent.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(20.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onAdd)
            .testTag("quick_add_series_${series.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (focused) CinemaBlack else RetroRuby,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = series.title,
                color = if (focused) CinemaBlack else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Reusable Cinematic Cartoon Series Card used on Home Screen & My Favorites Screen.
 * Supports D-Pad focus scaling, spring-bouncing Favorite Heart toggle, and one-tap Series browsing/marathon.
 */
@Composable
fun CartoonSeriesCard(
    series: CartoonSeries,
    isWideScreen: Boolean,
    onOpenSeries: () -> Unit,
    onPlayMarathon: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "series_card_scale"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (series.isFavorite) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "series_heart_scale"
    )
    val heartTint by animateColorAsState(
        targetValue = if (series.isFavorite) RetroRuby else Color.White,
        label = "series_heart_tint"
    )
    val accentColor = Color(series.accentHex)

    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (isFocused) 10.dp else 3.dp,
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) RetroGold else accentColor.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onOpenSeries)
            .testTag("series_card_${series.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(CinemaDark)
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
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                // Era + Series badge top-left
                Surface(
                    color = accentColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = series.era,
                        color = CinemaBlack,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Episode count pill bottom-left
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.shorts_count, series.episodeCount),
                        color = RetroGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Animated Favorite Series Heart Button top-right (48dp touch target on touch devices)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(48.dp)
                        .testTag("series_favorite_toggle_${series.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .border(
                                1.dp,
                                if (series.isFavorite) RetroRuby else Color.White.copy(alpha = 0.3f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(
                                if (series.isFavorite) R.string.remove_favorite_series_desc else R.string.save_favorite_series_desc,
                                series.title
                            ),
                            tint = heartTint,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = series.title,
                    color = if (isFocused) RetroGold else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = series.studio,
                    color = RetroCream.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = RetroGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RetroGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onPlayMarathon)
                            .testTag("series_marathon_${series.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = RetroGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.play_marathon),
                                color = RetroGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        color = CinemaSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSeries)
                    ) {
                        Text(
                            text = stringResource(R.string.episodes_label),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
