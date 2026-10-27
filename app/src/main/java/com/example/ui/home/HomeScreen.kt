package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
    ) {
        val isWideScreen = maxWidth >= 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Adaptive Header (Clean logo + TV navigation pills on wide screens / quick shortcuts on mobile)
            RetroMarqueeHeader(
                activeTab = uiState.currentTab,
                watchlistCount = uiState.watchlistItems.size,
                isWideScreen = isWideScreen,
                onTabSelected = onTabSelected
            )

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentTab) {
                    "home" -> {
                        HomeCatalogView(
                            uiState = uiState,
                            isWideScreen = isWideScreen,
                            onCategoryFilterSelected = onCategoryFilterSelected,
                            onPlay = onPlay,
                            onSelectDetail = onSelectDetail,
                            onToggleWatchlist = onToggleWatchlist,
                            onSetHero = onSetHero
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
    onTabSelected: (String) -> Unit
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
            // Brand Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected("home") }
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
                }
            } else {
                // Compact mobile header quick actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
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
            .focusable()
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

@Composable
fun HomeCatalogView(
    uiState: UiState,
    isWideScreen: Boolean,
    onCategoryFilterSelected: (String) -> Unit,
    onPlay: (CartoonItem) -> Unit,
    onSelectDetail: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onSetHero: (CartoonItem) -> Unit
) {
    val categories = listOf(
        "ALL" to "All Classics",
        "SUPERMAN" to "Superman (1941)",
        "POPEYE" to "Popeye the Sailor",
        "BETTY" to "Betty Boop",
        "SERIALS" to "Buster Keaton & Slapstick"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_lazy_column"),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Hero Featured Billboard
        uiState.heroItem?.let { hero ->
            item {
                HeroBillboard(
                    cartoon = hero,
                    isWideScreen = isWideScreen,
                    onPlay = { onPlay(hero) },
                    onDetails = { onSelectDetail(hero) },
                    onToggleWatchlist = { onToggleWatchlist(hero) },
                    featuredOptions = uiState.featuredList.take(5),
                    onSelectHero = onSetHero
                )
            }
        }

        // Quick Category Filter Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isWideScreen) 28.dp else 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (key, label) ->
                        val isSelected = uiState.categoryFilter == key
                        Surface(
                            color = if (isSelected) RetroGold else CinemaSurfaceVariant,
                            contentColor = if (isSelected) CinemaBlack else Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) RetroGold else CinemaBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onCategoryFilterSelected(key) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Row 1: Featured Popular
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

        // Row 2: Fleischer Superman 1941
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "SUPERMAN") && uiState.supermanList.isNotEmpty()) {
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

        // Row 3: Popeye The Sailor
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "POPEYE") && uiState.popeyeList.isNotEmpty()) {
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

        // Row 4: Betty Boop
        if ((uiState.categoryFilter == "ALL" || uiState.categoryFilter == "BETTY") && uiState.bettyList.isNotEmpty()) {
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

        // Row 5: Buster Keaton & Slapstick Serials
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
    featuredOptions: List<CartoonItem>,
    onSelectHero: (CartoonItem) -> Unit
) {
    // Auto-cycle hero if idle
    LaunchedEffect(cartoon.id) {
        delay(9000)
        val currentIndex = featuredOptions.indexOfFirst { it.id == cartoon.id }
        if (currentIndex != -1 && featuredOptions.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % featuredOptions.size
            onSelectHero(featuredOptions[nextIndex])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isWideScreen) 400.dp else 340.dp)
            .background(CinemaDark)
            .testTag("hero_billboard")
    ) {
        // High-res Background Image
        AsyncImage(
            model = cartoon.thumbnailUrl,
            contentDescription = cartoon.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
                .padding(horizontal = if (isWideScreen) 32.dp else 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
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
                    text = "${cartoon.year} • ${cartoon.creator}",
                    color = RetroGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = cartoon.title,
                color = Color.White,
                fontSize = if (isWideScreen) 30.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = cartoon.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                maxLines = if (isWideScreen) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 620.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                                .testTag("hero_play_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stream Short", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onToggleWatchlist,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (cartoon.isFavorite) RetroGold else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (cartoon.isFavorite) RetroGold else Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (cartoon.isFavorite) "Saved" else "+ My List", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }

                        IconButton(
                            onClick = onDetails,
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Details",
                                tint = Color.White
                            )
                        }
                    }

                    // Switcher Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        featuredOptions.forEach { opt ->
                            val isCurrent = opt.id == cartoon.id
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) RetroGold else Color.White.copy(alpha = 0.35f))
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
                        horizontalArrangement = Arrangement.End
                    ) {
                        featuredOptions.forEach { opt ->
                            val isCurrent = opt.id == cartoon.id
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isCurrent) 9.dp else 7.dp)
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
                            onClick = onDetails,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
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
                letterSpacing = 0.3.sp
            )
            Text(
                text = "${items.size} shorts",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
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
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
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

                // Watchlist button top-right
                IconButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (cartoon.isFavorite) RetroGold else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play hover icon in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = RetroGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Info below thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = cartoon.title,
                    color = Color.White,
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

                // Dynamic watch progress bar
                if (cartoon.watchProgressMs > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val progressRatio = (cartoon.watchProgressMs.toFloat() /
                            (if (cartoon.durationMs > 0) cartoon.durationMs else 600000L).toFloat())
                        .coerceIn(0.05f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        color = RetroRuby,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isWideScreen) 28.dp else 16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search Internet Archive public domain cartoons, characters, serials...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = RetroGold)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
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
                .testTag("search_text_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Suggestions
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPills) { pill ->
                val isSelected = searchQuery.equals(pill, ignoreCase = true)
                Surface(
                    color = if (isSelected) RetroGold else CinemaSurfaceVariant,
                    contentColor = if (isSelected) CinemaBlack else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onQueryChange(pill) }
                ) {
                    Text(
                        text = pill,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
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
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = RetroGold, contentColor = CinemaBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
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
