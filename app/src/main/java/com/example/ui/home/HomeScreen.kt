package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.example.R
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartoonItem
import com.example.ui.UiState
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
fun HomeScreen(
    uiState: UiState,
    onTabSelected: (String) -> Unit,
    onCategoryFilterSelected: (String) -> Unit,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onSetHero: (CartoonItem) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearRecentlyWatched: () -> Unit = {},
    onRemoveRecentlyWatched: (String) -> Unit = {},
    onShufflePlay: () -> Unit = {},
    onOpenDonation: () -> Unit = {},
    onDismissDonationBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // TV Remote & Mobile Back navigation: return to Featured Home tab or clear active Category filter before exiting app
    BackHandler(enabled = uiState.currentTab != "home" || uiState.categoryFilter != "ALL") {
        if (uiState.currentTab != "home") {
            onTabSelected("home")
        } else if (uiState.categoryFilter != "ALL") {
            onCategoryFilterSelected("ALL")
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val isTelevision = remember(context) {
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        }
        val isWideScreen = maxWidth >= 680.dp || isTelevision

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Adaptive Header (Clean logo + TV navigation pills on wide screens / quick shortcuts on mobile)
            RetroMarqueeHeader(
                activeTab = uiState.currentTab,
                watchlistCount = uiState.watchlistItems.size,
                isWideScreen = isWideScreen,
                isSupporter = uiState.isSupporter,
                onTabSelected = onTabSelected,
                onOpenDonation = onOpenDonation
            )

            // Top-Level Instant Search Bar (shown on mobile portrait when not on Search tab; on TV/wide screens the Header Search pill is used to avoid D-Pad keyboard traps)
            AnimatedVisibility(
                visible = !isWideScreen && uiState.currentTab != "search",
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                Surface(
                    color = CinemaDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isWideScreen) 28.dp else 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { query ->
                            onSearchQueryChanged(query)
                            if (query.isNotBlank() && uiState.currentTab != "search") {
                                onTabSelected("search")
                            }
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = RetroGold,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.close),
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RetroGold,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = CinemaSurfaceVariant,
                            unfocusedContainerColor = CinemaSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("top_level_search_bar")
                    )
                }
            }

            // Main Content Area with smooth tab transitions
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "tab_content_transition"
                ) { tab ->
                    when (tab) {
                        "home" -> {
                            HomeCatalogView(
                                uiState = uiState,
                                isWideScreen = isWideScreen,
                                onCategoryFilterSelected = onCategoryFilterSelected,
                                onPlay = onPlay,
                                onSelectDetail = onSelectDetail,
                                onToggleWatchlist = onToggleWatchlist,
                                onSetHero = onSetHero,
                                onClearRecentlyWatched = onClearRecentlyWatched,
                                onRemoveRecentlyWatched = onRemoveRecentlyWatched,
                                onShufflePlay = onShufflePlay,
                                onOpenDonation = onOpenDonation,
                                onDismissDonationBanner = onDismissDonationBanner
                            )
                        }
                        "search" -> {
                            SearchView(
                                searchQuery = uiState.searchQuery,
                                searchResults = uiState.searchResults,
                                isSearching = uiState.isSearching,
                                isWideScreen = isWideScreen,
                                onQueryChange = onSearchQueryChanged,
                                onPlay = onPlay,
                                onSelectDetail = onSelectDetail,
                                onToggleWatchlist = onToggleWatchlist
                            )
                        }
                        "watchlist" -> {
                            WatchlistView(
                                items = uiState.watchlistItems,
                                isWideScreen = isWideScreen,
                                onPlay = onPlay,
                                onSelectDetail = onSelectDetail,
                                onToggleWatchlist = onToggleWatchlist,
                                onExploreClick = { onTabSelected("home") }
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Bar for Mobile Portrait / Compact screens
            if (!isWideScreen) {
                RetroBottomNavBar(
                    activeTab = uiState.currentTab,
                    watchlistCount = uiState.watchlistItems.size,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}

@Composable
fun RetroMarqueeHeader(
    activeTab: String,
    watchlistCount: Int,
    isWideScreen: Boolean,
    isSupporter: Boolean = false,
    onTabSelected: (String) -> Unit,
    onOpenDonation: () -> Unit = {}
) {
    Surface(
        color = CinemaDark,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isWideScreen) 28.dp else 16.dp, vertical = if (isWideScreen) 12.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo (clickable on touch devices; on TV, "Featured" nav pill handles Home navigation so the logo doesn't steal D-Pad focus)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (!isWideScreen) Modifier.clickable { onTabSelected("home") }
                        else Modifier
                    )
                    .testTag("brand_logo_button")
            ) {
                Surface(
                    color = RetroRuby,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(if (isWideScreen) 40.dp else 34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "RetroToon",
                            tint = Color.White,
                            modifier = Modifier.size(if (isWideScreen) 24.dp else 20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RETROTOON",
                            color = RetroGold,
                            fontSize = if (isWideScreen) 19.sp else 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TV",
                            color = RetroRuby,
                            fontSize = if (isWideScreen) 19.sp else 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = if (isWideScreen) "PUBLIC DOMAIN • INTERNET ARCHIVE" else "PUBLIC DOMAIN",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = if (isWideScreen) 10.sp else 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isWideScreen) {
                // TV / Tablet Navigation Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavPillButton(
                        title = "Featured",
                        icon = Icons.Default.Tv,
                        selected = activeTab == "home",
                        onClick = { onTabSelected("home") },
                        tag = "nav_home_tab"
                    )

                    NavPillButton(
                        title = "Search",
                        icon = Icons.Default.Search,
                        selected = activeTab == "search",
                        onClick = { onTabSelected("search") },
                        tag = "nav_search_tab"
                    )

                    NavPillButton(
                        title = if (watchlistCount > 0) "Watchlist ($watchlistCount)" else "Watchlist",
                        icon = Icons.Default.Bookmark,
                        selected = activeTab == "watchlist",
                        onClick = { onTabSelected("watchlist") },
                        tag = "nav_watchlist_tab"
                    )

                    if (isSupporter) {
                        Surface(
                            color = RetroGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold),
                            modifier = Modifier.clickable { onOpenDonation() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = RetroGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("★ Supporter", color = RetroGold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        var supportFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = onOpenDonation,
                            modifier = Modifier
                                .size(38.dp)
                                .onFocusChanged { supportFocused = it.isFocused }
                                .background(
                                    if (supportFocused) RetroGold else RetroRuby.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .then(
                                    if (supportFocused) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .testTag("nav_support_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = "Support Development",
                                tint = if (supportFocused) CinemaBlack else RetroGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // Compact mobile header quick actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSupporter) {
                        Surface(
                            color = RetroGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold),
                            modifier = Modifier.clickable { onOpenDonation() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = RetroGold, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("★", color = RetroGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        var supportFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = onOpenDonation,
                            modifier = Modifier
                                .size(38.dp)
                                .onFocusChanged { supportFocused = it.isFocused }
                                .background(
                                    if (supportFocused) RetroGold else RetroRuby.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .then(
                                    if (supportFocused) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .testTag("nav_support_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = "Support Development",
                                tint = if (supportFocused) CinemaBlack else RetroGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onTabSelected("search") },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (activeTab == "search") RetroGold.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                            .testTag("nav_search_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (activeTab == "search") RetroGold else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onTabSelected("watchlist") },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (activeTab == "watchlist") RetroGold.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                            .testTag("nav_watchlist_quick_button")
                    ) {
                        if (watchlistCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = RetroRuby,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = watchlistCount.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Watchlist",
                                    tint = if (activeTab == "watchlist") RetroGold else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (activeTab == "watchlist") RetroGold else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetroBottomNavBar(
    activeTab: String,
    watchlistCount: Int,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = CinemaDark,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = CinemaDark,
            contentColor = Color.White,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = activeTab == "home",
                onClick = { onTabSelected("home") },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Featured",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = "Featured",
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == "home") FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CinemaBlack,
                    selectedTextColor = RetroGold,
                    indicatorColor = RetroGold,
                    unselectedIconColor = Color.White.copy(alpha = 0.65f),
                    unselectedTextColor = Color.White.copy(alpha = 0.65f)
                ),
                modifier = Modifier.testTag("nav_home_tab")
            )

            NavigationBarItem(
                selected = activeTab == "search",
                onClick = { onTabSelected("search") },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = "Search",
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == "search") FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CinemaBlack,
                    selectedTextColor = RetroGold,
                    indicatorColor = RetroGold,
                    unselectedIconColor = Color.White.copy(alpha = 0.65f),
                    unselectedTextColor = Color.White.copy(alpha = 0.65f)
                ),
                modifier = Modifier.testTag("nav_search_tab")
            )

            NavigationBarItem(
                selected = activeTab == "watchlist",
                onClick = { onTabSelected("watchlist") },
                icon = {
                    if (watchlistCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = RetroRuby,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = watchlistCount.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Watchlist",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Watchlist",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = "Watchlist",
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == "watchlist") FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CinemaBlack,
                    selectedTextColor = RetroGold,
                    indicatorColor = RetroGold,
                    unselectedIconColor = Color.White.copy(alpha = 0.65f),
                    unselectedTextColor = Color.White.copy(alpha = 0.65f)
                ),
                modifier = Modifier.testTag("nav_watchlist_tab")
            )
        }
    }
}

@Composable
fun NavPillButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(if (selected) RetroGold else CinemaSurfaceVariant, label = "pill_bg")
    val contentColor by animateColorAsState(if (selected) CinemaBlack else Color.White, label = "pill_content")

    Surface(
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
        border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(16.dp),
                tint = if (selected) CinemaBlack else RetroGold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class CategoryFilterItem(
    val key: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun HomeCatalogView(
    uiState: UiState,
    isWideScreen: Boolean,
    onCategoryFilterSelected: (String) -> Unit,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onSetHero: (CartoonItem) -> Unit,
    onClearRecentlyWatched: () -> Unit = {},
    onRemoveRecentlyWatched: (String) -> Unit = {},
    onShufflePlay: () -> Unit = {},
    onOpenDonation: () -> Unit = {},
    onDismissDonationBanner: () -> Unit = {}
) {
    val categories = remember {
        listOf(
            CategoryFilterItem("ALL", R.string.category_all, Icons.Default.Movie),
            CategoryFilterItem("CARTOONS", R.string.category_cartoons, Icons.Default.Tv),
            CategoryFilterItem("SERIALS", R.string.category_serials, Icons.Default.VideoLibrary),
            CategoryFilterItem("VINTAGE", R.string.category_vintage, Icons.Default.History),
            CategoryFilterItem("SUPERMAN", R.string.category_superman, Icons.Default.Bolt),
            CategoryFilterItem("POPEYE", R.string.category_popeye, Icons.Default.Anchor),
            CategoryFilterItem("BETTY", R.string.category_betty, Icons.Default.Star)
        )
    }

    val vintageList = remember(uiState.featuredList, uiState.supermanList, uiState.popeyeList, uiState.bettyList, uiState.serialsList) {
        (uiState.featuredList + uiState.supermanList + uiState.popeyeList + uiState.bettyList + uiState.serialsList)
            .distinctBy { it.id }
            .filter { item ->
                val yearNum = item.year.filter { it.isDigit() }.toIntOrNull() ?: 1950
                yearNum < 1940
            }
    }

    val cartoonsList = remember(uiState.supermanList, uiState.popeyeList, uiState.bettyList) {
        (uiState.supermanList + uiState.popeyeList + uiState.bettyList).distinctBy { it.id }
    }

    fun getCategoryCount(key: String): Int {
        return when (key) {
            "ALL" -> (uiState.featuredList + uiState.supermanList + uiState.popeyeList + uiState.bettyList + uiState.serialsList).distinctBy { it.id }.size
            "CARTOONS" -> cartoonsList.size
            "SERIALS" -> uiState.serialsList.size
            "VINTAGE" -> vintageList.size
            "SUPERMAN" -> uiState.supermanList.size
            "POPEYE" -> uiState.popeyeList.size
            "BETTY" -> uiState.bettyList.size
            else -> 0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_lazy_column"),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Hero Featured Billboard (shown on ALL or when not filtering out hero)
        if (uiState.categoryFilter == "ALL") {
            uiState.heroItem?.let { hero ->
                item {
                    HeroBillboard(
                        cartoon = hero,
                        isWideScreen = isWideScreen,
                        onPlay = { onPlay(hero) },
                        onDetails = { onSelectDetail(hero) },
                        onToggleWatchlist = { onToggleWatchlist(hero) },
                        onShufflePlay = onShufflePlay,
                        featuredOptions = uiState.featuredList.take(5),
                        onSelectHero = onSetHero
                    )
                }
            }
        }

        // Home Screen Compact Donation Banner (Triggers after verified satisfaction)
        if (uiState.showHomeDonationBanner) {
            item {
                HomeDonationBanner(
                    onSupportClick = onOpenDonation,
                    onDismiss = onDismissDonationBanner,
                    modifier = Modifier.padding(
                        horizontal = if (isWideScreen) 28.dp else 16.dp,
                        vertical = 8.dp
                    )
                )
            }
        }

        // Horizontal Category Filter Chips Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_filter_chips_row"),
                    contentPadding = PaddingValues(horizontal = if (isWideScreen) 28.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = uiState.categoryFilter == cat.key
                        var isFocused by remember { mutableStateOf(false) }
                        val count = getCategoryCount(cat.key)

                        Surface(
                            color = when {
                                isSelected -> RetroGold
                                isFocused -> CinemaDark
                                else -> CinemaSurfaceVariant
                            },
                            contentColor = when {
                                isSelected -> CinemaBlack
                                else -> Color.White
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected || isFocused) 1.5.dp else 1.dp,
                                color = when {
                                    isSelected -> RetroGold
                                    isFocused -> Color.White
                                    else -> CinemaBorder
                                }
                            ),
                            tonalElevation = if (isSelected) 4.dp else 0.dp,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable { onCategoryFilterSelected(cat.key) }
                                .testTag("category_chip_${cat.key.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) CinemaBlack else RetroGold,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = stringResource(cat.labelRes),
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (isSelected) CinemaBlack.copy(alpha = 0.2f) else CinemaBlack.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) CinemaBlack else RetroCream,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Filter Status & Clear Button (when not ALL)
                if (uiState.categoryFilter != "ALL") {
                    val activeCategory = categories.firstOrNull { it.key == uiState.categoryFilter }
                    if (activeCategory != null) {
                        Surface(
                            color = CinemaDark,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetroGold.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isWideScreen) 28.dp else 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = activeCategory.icon,
                                        contentDescription = null,
                                        tint = RetroGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${stringResource(R.string.category_filter_label)}: ${stringResource(activeCategory.labelRes)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                var clearFilterFocused by remember { mutableStateOf(false) }
                                TextButton(
                                    onClick = { onCategoryFilterSelected("ALL") },
                                    colors = ButtonDefaults.textButtonColors(contentColor = RetroGold),
                                    border = if (clearFilterFocused) androidx.compose.foundation.BorderStroke(1.5.dp, RetroGold) else null,
                                    modifier = Modifier
                                        .onFocusChanged { clearFilterFocused = it.isFocused }
                                        .testTag("clear_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.category_clear_filter),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Recently Watched / Continue Watching History (from local Room database)
        if (uiState.recentlyWatchedList.isNotEmpty()) {
            item {
                RecentlyWatchedCarouselRow(
                    items = uiState.recentlyWatchedList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist,
                    onClearHistory = onClearRecentlyWatched,
                    onRemoveItem = onRemoveRecentlyWatched
                )
            }
        }

        // Section: Popular Classics (shown on ALL)
        if (uiState.categoryFilter == "ALL" && uiState.featuredList.isNotEmpty()) {
            item {
                CartoonCarouselRow(
                    title = "🔥 Popular Classic Cartoons",
                    items = uiState.featuredList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Section: Vintage Pre-1940 Treasures (shown on VINTAGE)
        if (uiState.categoryFilter == "VINTAGE" && vintageList.isNotEmpty()) {
            item {
                CartoonCarouselRow(
                    title = "🕰️ Golden Age Vintage (Pre-1940 Gems)",
                    items = vintageList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Section: Superman Serials (shown on ALL, CARTOONS, SUPERMAN)
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "CARTOONS" || uiState.categoryFilter == "SUPERMAN") && uiState.supermanList.isNotEmpty()) {
            item {
                CartoonCarouselRow(
                    title = "⚡ Fleischer Superman Animated Serials (1941-1943)",
                    items = uiState.supermanList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Section: Popeye The Sailor (shown on ALL, CARTOONS, POPEYE)
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "CARTOONS" || uiState.categoryFilter == "POPEYE") && uiState.popeyeList.isNotEmpty()) {
            item {
                CartoonCarouselRow(
                    title = "⚓ Popeye the Sailor",
                    items = uiState.popeyeList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Section: Betty Boop (shown on ALL, CARTOONS, VINTAGE, BETTY)
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "CARTOONS" || uiState.categoryFilter == "VINTAGE" || uiState.categoryFilter == "BETTY") && uiState.bettyList.isNotEmpty()) {
            item {
                CartoonCarouselRow(
                    title = "💃 Betty Boop & Fleischer Jazz Gems",
                    items = uiState.bettyList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Section: Buster Keaton & Slapstick Serials (shown on ALL, SERIALS, VINTAGE)
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "SERIALS") && uiState.serialsList.isNotEmpty()) {
            item {
                CartoonCarouselRow(
                    title = "🎩 Buster Keaton & Silent Comedy Serials",
                    items = uiState.serialsList,
                    isWideScreen = isWideScreen,
                    onPlay = onPlay,
                    onSelectDetail = onSelectDetail,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }
    }
}

@Composable
fun HeroBillboard(
    cartoon: CartoonItem,
    isWideScreen: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onShufflePlay: () -> Unit = {},
    featuredOptions: List<CartoonItem>,
    onSelectHero: (CartoonItem) -> Unit
) {
    val heroPlayFocusRequester = remember { FocusRequester() }
    var playFocused by remember { mutableStateOf(false) }
    var listFocused by remember { mutableStateOf(false) }
    var shuffleFocused by remember { mutableStateOf(false) }
    var detailsFocused by remember { mutableStateOf(false) }
    val isSecondaryActionFocused = listFocused || shuffleFocused || detailsFocused

    LaunchedEffect(isWideScreen) {
        if (isWideScreen) {
            delay(280)
            try {
                heroPlayFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-cycle hero if idle (pauses if TV user is actively inspecting secondary hero buttons)
    LaunchedEffect(cartoon.id, isSecondaryActionFocused) {
        if (!isSecondaryActionFocused) {
            delay(9500)
            val currentIndex = featuredOptions.indexOfFirst { it.id == cartoon.id }
            if (currentIndex != -1 && featuredOptions.isNotEmpty()) {
                val nextIndex = (currentIndex + 1) % featuredOptions.size
                onSelectHero(featuredOptions[nextIndex])
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isWideScreen) 248.dp else 310.dp)
            .background(CinemaDark)
            .testTag("hero_billboard")
    ) {
        // High-res Background Image with smooth Crossfade when hero changes
        Crossfade(
            targetState = cartoon.thumbnailUrl,
            animationSpec = tween(550),
            label = "hero_image_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { thumbUrl ->
            AsyncImage(
                model = thumbUrl,
                contentDescription = cartoon.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Vignette & Cinematic Dark Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CinemaBlack.copy(alpha = 0.65f),
                            CinemaBlack
                        )
                    )
                )
        )

        if (isWideScreen) {
            // Horizontal gradient overlay for wide TV screens
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CinemaBlack.copy(alpha = 0.9f),
                                CinemaBlack.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Billboard Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWideScreen) 32.dp else 20.dp, vertical = if (isWideScreen) 14.dp else 18.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            AnimatedContent(
                targetState = cartoon,
                contentKey = { it.id },
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(110))
                },
                label = "hero_text_transition"
            ) { activeHero ->
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = RetroRuby,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LEANBACK FEATURED",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "${activeHero.year} • ${activeHero.creator}",
                            color = RetroGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = activeHero.title,
                        color = Color.White,
                        fontSize = if (isWideScreen) 24.sp else 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = if (isWideScreen) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Description
                    Text(
                        text = activeHero.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 620.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isWideScreen) 12.dp else 14.dp))

            // Action Buttons & Switcher
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onPlay,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RetroGold,
                                contentColor = CinemaBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .focusRequester(heroPlayFocusRequester)
                                .onFocusChanged { playFocused = it.isFocused }
                                .then(
                                    if (playFocused) Modifier.border(2.5.dp, Color.White, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .testTag("hero_play_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stream Short", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onToggleWatchlist,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (cartoon.isFavorite || listFocused) RetroGold else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (listFocused) 2.dp else 1.dp,
                                color = if (listFocused) RetroGold else Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .onFocusChanged { listFocused = it.isFocused }
                        ) {
                            Icon(
                                imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (cartoon.isFavorite || listFocused) RetroGold else Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (cartoon.isFavorite) "Saved" else "+ My List", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onShufflePlay,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (shuffleFocused) RetroGold else RetroCream
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (shuffleFocused) 2.dp else 1.dp,
                                color = if (shuffleFocused) RetroGold else Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .onFocusChanged { shuffleFocused = it.isFocused }
                                .testTag("hero_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle Marathon",
                                tint = RetroGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }

                        IconButton(
                            onClick = onDetails,
                            modifier = Modifier
                                .size(46.dp)
                                .onFocusChanged { detailsFocused = it.isFocused }
                                .background(
                                    if (detailsFocused) RetroGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .then(
                                    if (detailsFocused) Modifier.border(2.dp, RetroGold, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .testTag("hero_details_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Details",
                                tint = if (detailsFocused) RetroGold else Color.White
                            )
                        }
                    }

                    // Animated Switcher Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        featuredOptions.forEach { opt ->
                            val isCurrent = opt.id == cartoon.id
                            val dotWidth by animateDpAsState(
                                targetValue = if (isCurrent) 22.dp else 8.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "hero_dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) RetroGold else Color.White.copy(alpha = 0.35f))
                                    .focusProperties { canFocus = false }
                                    .clickable { onSelectHero(opt) }
                            )
                        }
                    }
                }
            } else {
                // Compact mobile hero actions
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        featuredOptions.forEach { opt ->
                            val isCurrent = opt.id == cartoon.id
                            val dotWidth by animateDpAsState(
                                targetValue = if (isCurrent) 20.dp else 7.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "hero_mobile_dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(7.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) RetroGold else Color.White.copy(alpha = 0.35f))
                                    .clickable { onSelectHero(opt) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlay,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RetroGold,
                                contentColor = CinemaBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("hero_play_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onToggleWatchlist,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (cartoon.isFavorite) RetroGold else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (cartoon.isFavorite) RetroGold else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (cartoon.isFavorite) "Saved" else "List", fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                        }

                        IconButton(
                            onClick = onShufflePlay,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .testTag("hero_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle Marathon",
                                tint = RetroGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDetails,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .testTag("hero_details_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Details",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentlyWatchedCarouselRow(
    items: List<CartoonItem>,
    isWideScreen: Boolean,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveItem: (String) -> Unit = {}
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp)
            .testTag("recently_watched_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isWideScreen) 28.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = RetroGold.copy(alpha = 0.18f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = RetroGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.recently_watched),
                        color = Color.White,
                        fontSize = if (isWideScreen) 18.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = stringResource(R.string.continue_watching),
                        color = RetroGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            var clearHistoryFocused by remember { mutableStateOf(false) }
            TextButton(
                onClick = onClearHistory,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (clearHistoryFocused) RetroGold else Color.White.copy(alpha = 0.55f)
                ),
                border = if (clearHistoryFocused) androidx.compose.foundation.BorderStroke(1.5.dp, RetroGold) else null,
                modifier = Modifier
                    .onFocusChanged { clearHistoryFocused = it.isFocused }
                    .testTag("clear_history_button")
            ) {
                Text(
                    text = stringResource(R.string.clear_history),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val cardWidth = if (isWideScreen) 230.dp else 180.dp
        LazyRow(
            contentPadding = PaddingValues(horizontal = if (isWideScreen) 28.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { "recent_${it.id}" }) { item ->
                CartoonPosterCard(
                    cartoon = item,
                    isWideScreen = isWideScreen,
                    onClick = { onSelectDetail(item) },
                    onPlay = { onPlay(item) },
                    onToggleWatchlist = { onToggleWatchlist(item) },
                    onRemoveRecent = { onRemoveItem(item.id) },
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}

@Composable
fun CartoonCarouselRow(
    title: String,
    items: List<CartoonItem>,
    isWideScreen: Boolean,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit
) {
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isWideScreen) 28.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (isWideScreen) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = if (isWideScreen) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${items.size} shorts",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val cardWidth = if (isWideScreen) 230.dp else 180.dp
        LazyRow(
            contentPadding = PaddingValues(horizontal = if (isWideScreen) 28.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.id }) { item ->
                CartoonPosterCard(
                    cartoon = item,
                    isWideScreen = isWideScreen,
                    onClick = { onSelectDetail(item) },
                    onPlay = { onPlay(item) },
                    onToggleWatchlist = { onToggleWatchlist(item) },
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}

@Composable
fun CartoonPosterCard(
    cartoon: CartoonItem,
    isWideScreen: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onRemoveRecent: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "poster_scale"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CinemaSurface,
        tonalElevation = if (isFocused) 12.dp else 3.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) RetroGold else CinemaBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("poster_card_${cartoon.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 16:9 Landscape Video Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(CinemaDark)
            ) {
                AsyncImage(
                    model = cartoon.thumbnailUrl,
                    contentDescription = cartoon.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle gradient for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Duration badge bottom-right
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = cartoon.runtime,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Resume badge top-left when watch progress exists
                if (cartoon.watchProgressMs > 0) {
                    Surface(
                        color = RetroRuby.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "RESUME",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    val progressFraction = if (cartoon.durationMs > 0) {
                        (cartoon.watchProgressMs.toFloat() / cartoon.durationMs.toFloat()).coerceIn(0.08f, 1f)
                    } else {
                        0.35f
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .align(Alignment.BottomCenter),
                        color = RetroGold,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }

                // Watchlist & optional Remove from History compact circular badges top-right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRemoveRecent != null && !isWideScreen) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.68f))
                                .border(0.75.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable(onClick = onRemoveRecent)
                                .testTag("remove_recent_${cartoon.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove from History",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .border(
                                0.75.dp,
                                if (cartoon.isFavorite) RetroGold.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.22f),
                                CircleShape
                            )
                            .then(
                                if (!isWideScreen) Modifier.clickable(onClick = onToggleWatchlist)
                                else Modifier
                            )
                    ) {
                        Icon(
                            imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Watchlist",
                            tint = if (cartoon.isFavorite) RetroGold else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Play hover icon in center with animated focus highlight
                val playBtnBg by animateColorAsState(
                    targetValue = if (isFocused) RetroGold else Color.Black.copy(alpha = 0.65f),
                    label = "play_btn_bg"
                )
                val playIconTint by animateColorAsState(
                    targetValue = if (isFocused) CinemaBlack else RetroGold,
                    label = "play_icon_tint"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (isFocused) 42.dp else 36.dp)
                        .clip(CircleShape)
                        .background(playBtnBg)
                        .border(1.dp, RetroGold.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = playIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Info below thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = cartoon.title,
                    color = if (isFocused) RetroGold else Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cartoon.year,
                        color = RetroGold,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = cartoon.creator.take(18),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SearchView(
    searchQuery: String,
    searchResults: List<CartoonItem>,
    isSearching: Boolean,
    isWideScreen: Boolean,
    onQueryChange: (String) -> Unit,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit
) {
    val quickPills = listOf("Superman", "Popeye", "Betty Boop", "Felix the Cat", "Buster Keaton", "Casper", "Little Audrey")
    val firstPillFocusRequester = remember { FocusRequester() }
    val tvTextFieldFocusRequester = remember { FocusRequester() }
    var isTvKeyboardActive by remember { mutableStateOf(false) }
    var tvSearchBarFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isWideScreen) {
        if (isWideScreen) {
            delay(280)
            try {
                firstPillFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isTvKeyboardActive) {
        if (isTvKeyboardActive) {
            delay(80)
            try {
                tvTextFieldFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isWideScreen) 28.dp else 16.dp)
    ) {
        // Search Input (on TV, activates keyboard only on D-Pad Center so Up/Down navigation never gets trapped by IME)
        if (isWideScreen && !isTvKeyboardActive) {
            Surface(
                color = CinemaDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (tvSearchBarFocused) 2.dp else 1.dp,
                    color = if (tvSearchBarFocused) RetroGold else CinemaBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .onFocusChanged { tvSearchBarFocused = it.isFocused }
                    .clickable { isTvKeyboardActive = true }
                    .testTag("search_text_field")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = RetroGold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = searchQuery.ifEmpty {
                                "Press OK to type, or select a quick tag below..."
                            },
                            color = if (searchQuery.isEmpty()) Color.White.copy(alpha = 0.6f) else Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.focusProperties { canFocus = false }
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search Internet Archive public domain cartoons, characters, serials...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = RetroGold)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.focusProperties { canFocus = false }
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RetroGold,
                    unfocusedBorderColor = CinemaBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CinemaDark,
                    unfocusedContainerColor = CinemaDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isWideScreen) Modifier.focusRequester(tvTextFieldFocusRequester) else Modifier)
                    .onFocusChanged {
                        if (isWideScreen && !it.isFocused) {
                            isTvKeyboardActive = false
                        }
                    }
                    .testTag("search_text_field")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Suggestions (D-Pad & Touch focusable)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPills) { pill ->
                val isSelected = searchQuery.equals(pill, ignoreCase = true)
                val isFirst = pill == quickPills.first()
                var pillFocused by remember { mutableStateOf(false) }
                Surface(
                    color = when {
                        isSelected -> RetroGold
                        pillFocused -> CinemaDark
                        else -> CinemaSurfaceVariant
                    },
                    contentColor = if (isSelected) CinemaBlack else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = if (pillFocused || isSelected) {
                        androidx.compose.foundation.BorderStroke(1.5.dp, if (pillFocused) Color.White else RetroGold)
                    } else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .then(if (isFirst) Modifier.focusRequester(firstPillFocusRequester) else Modifier)
                        .onFocusChanged { pillFocused = it.isFocused }
                        .clickable { onQueryChange(pill) }
                        .testTag("search_pill_${pill.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = pill,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || pillFocused) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> CinemaBlack
                            pillFocused -> RetroGold
                            else -> Color.White
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Search Results State
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RetroGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Searching Internet Archive public domain archives...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cartoon shorts found for '$searchQuery'. Try 'Popeye', 'Superman', or 'Betty Boop'!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        } else if (searchResults.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = RetroGold.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Stream Thousands of Pre-1960s Public Domain Shorts",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Search by character, studio (Fleischer, Famous, Van Beuren), or short title.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        } else {
            Text(
                text = "${searchResults.size} titles found from Archive.org",
                color = RetroGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isWideScreen) 220.dp else 150.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { it.id }) { item ->
                    CartoonPosterCard(
                        cartoon = item,
                        isWideScreen = isWideScreen,
                        onClick = { onSelectDetail(item) },
                        onPlay = { onPlay(item) },
                        onToggleWatchlist = { onToggleWatchlist(item) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun WatchlistView(
    items: List<CartoonItem>,
    isWideScreen: Boolean,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onExploreClick: () -> Unit
) {
    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = RetroGold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Watchlist is Empty",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bookmark your favorite Betty Boop, Popeye, and Superman serials to watch anytime.",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            var browseFocused by remember { mutableStateOf(false) }
            val browseFocusRequester = remember { FocusRequester() }
            LaunchedEffect(isWideScreen) {
                if (isWideScreen) {
                    delay(200)
                    try {
                        browseFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = RetroGold, contentColor = CinemaBlack),
                border = if (browseFocused) androidx.compose.foundation.BorderStroke(2.5.dp, Color.White) else null,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .focusRequester(browseFocusRequester)
                    .onFocusChanged { browseFocused = it.isFocused }
            ) {
                Text("Browse Classic Cartoons", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isWideScreen) 28.dp else 20.dp)
        ) {
            Text(
                text = "My Saved Cartoons (${items.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isWideScreen) 220.dp else 150.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    CartoonPosterCard(
                        cartoon = item,
                        isWideScreen = isWideScreen,
                        onClick = { onSelectDetail(item) },
                        onPlay = { onPlay(item) },
                        onToggleWatchlist = { onToggleWatchlist(item) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
