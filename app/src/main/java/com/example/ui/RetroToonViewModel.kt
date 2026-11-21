package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.donation.DonationManager
import com.example.data.download.DownloadHelper
import com.example.data.engagement.AppSuggestion
import com.example.data.engagement.EngagementManager
import com.example.data.live.LiveTvChannel
import com.example.data.live.LiveTvChannelEngine
import com.example.data.live.PublicDomainSuggestion
import com.example.data.model.CartoonItem
import com.example.data.model.CartoonSeries
import com.example.data.model.CartoonSeriesCatalog
import com.example.data.repository.CartoonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FilmEffect(@param:StringRes val titleRes: Int) {
    CLEAN(R.string.effect_clean),
    SEPIA_GRAIN(R.string.effect_sepia_grain),
    BLACK_WHITE(R.string.effect_black_white),
    CRT_GLOW(R.string.effect_crt_glow)
}

enum class AspectMode(@param:StringRes val titleRes: Int) {
    ORIGINAL(R.string.aspect_original),
    FIT(R.string.aspect_fit),
    FILL(R.string.aspect_fill)
}

data class UiState(
    val isSplashVisible: Boolean = true,
    val isInPipMode: Boolean = false,
    val currentTab: String = "home", // "home", "live", "search", "watchlist" (My Favorites)
    val categoryFilter: String = "ALL", // "ALL", "CARTOONS", "SLAPSTICK", "SERIALS", "VINTAGE", "SUPERMAN", "POPEYE", "BETTY"
    val heroItem: CartoonItem? = null,
    val featuredList: List<CartoonItem> = emptyList(),
    val supermanList: List<CartoonItem> = emptyList(),
    val popeyeList: List<CartoonItem> = emptyList(),
    val bettyList: List<CartoonItem> = emptyList(),
    val classicsSciFiList: List<CartoonItem> = emptyList(),
    val serialsList: List<CartoonItem> = emptyList(),
    val allSeries: List<CartoonSeries> = emptyList(),
    val favoriteSeries: List<CartoonSeries> = emptyList(),
    val selectedSeries: CartoonSeries? = null,
    val selectedSeriesEpisodes: List<CartoonItem> = emptyList(),
    val liveChannels: List<LiveTvChannel> = emptyList(),
    val activeLiveChannel: LiveTvChannel? = null,
    val liveStartOffsetMs: Long = 0L,
    val watchlistItems: List<CartoonItem> = emptyList(),
    val selectedDetailItem: CartoonItem? = null,
    val playingItem: CartoonItem? = null,
    val upNextItem: CartoonItem? = null,
    val resolvedStreamUrl: String? = null,
    val resolvedSubtitleUrl: String? = null,
    val candidateStreamUrls: List<String> = emptyList(),
    val activeStreamIndex: Int = 0,
    val isResolvingStream: Boolean = false,
    val streamError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<CartoonItem> = emptyList(),
    val searchFallbackSuggestion: PublicDomainSuggestion? = null,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val filmEffect: FilmEffect = FilmEffect.CLEAN,
    val aspectMode: AspectMode = AspectMode.ORIGINAL,
    val activeSuggestion: AppSuggestion? = null,
    val isSupporter: Boolean = false,
    val supporterTier: String? = null,
    val showHomeDonationBanner: Boolean = false,
    val showDonationDialog: Boolean = false,
    val recentlyWatchedList: List<CartoonItem> = emptyList()
) {
    val totalFavoritesCount: Int
        get() = favoriteSeries.size + watchlistItems.size
}

class RetroToonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CartoonRepository(application.applicationContext)
    private val engagementManager = EngagementManager(application.applicationContext)
    val donationManager = DonationManager(application.applicationContext)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var streamResolveJob: Job? = null
    private var latestFavoriteSeriesIds: Set<String> = emptySet()

    init {
        engagementManager.recordAppLaunch()
        donationManager.recordAppLaunch()
        updateSupporterAndBannerState()
        loadInitialCatalog()
        observeWatchlist()
        observeFavoriteSeries()
        observeRecentlyWatched()
    }

    private fun buildLiveChannelsFromState(state: UiState): List<LiveTvChannel> {
        return LiveTvChannelEngine.buildChannels(
            featured = state.featuredList,
            superman = state.supermanList,
            popeye = state.popeyeList,
            betty = state.bettyList,
            classicsAndSciFi = state.classicsSciFiList,
            serials = state.serialsList
        )
    }

    private fun buildSeriesFromState(state: UiState, favIds: Set<String>): List<CartoonSeries> {
        return CartoonSeriesCatalog.buildAllSeries(
            supermanList = state.supermanList,
            popeyeList = state.popeyeList,
            bettyList = state.bettyList,
            classicsSciFiList = state.classicsSciFiList,
            serialsList = state.serialsList,
            favoriteSeriesIds = favIds
        )
    }

    private fun allCatalogItemsFromState(state: UiState): List<CartoonItem> {
        return (state.featuredList +
            state.supermanList +
            state.popeyeList +
            state.bettyList +
            state.classicsSciFiList +
            state.serialsList +
            state.searchResults).distinctBy { it.id }
    }

    private fun loadInitialCatalog() {
        val featured = repository.enrichWithUserData(repository.getCuratedFeatured())
        val superman = repository.enrichWithUserData(repository.getCuratedSuperman())
        val popeye = repository.enrichWithUserData(repository.getCuratedPopeye())
        val betty = repository.enrichWithUserData(repository.getCuratedBettyBoop())
        val classicsSciFi = repository.enrichWithUserData(repository.getCuratedClassicsAndSciFi())
        val serials = repository.enrichWithUserData(repository.getCuratedSerials())

        _uiState.update { current ->
            val seeded = current.copy(
                heroItem = current.heroItem ?: featured.firstOrNull(),
                featuredList = featured,
                supermanList = superman,
                popeyeList = popeye,
                bettyList = betty,
                classicsSciFiList = classicsSciFi,
                serialsList = serials
            )
            seeded.copy(
                allSeries = buildSeriesFromState(seeded, latestFavoriteSeriesIds),
                liveChannels = buildLiveChannelsFromState(seeded)
            )
        }

        // Parallel async hydration from Internet Archive for faster startup
        viewModelScope.launch {
            try {
                coroutineScope {
                    val popeyeDeferred = async {
                        repository.fetchCategory("collection:(classic_cartoons) AND title:(popeye) AND NOT sound:(silent)", popeye)
                    }
                    val supermanDeferred = async {
                        repository.fetchCategory("collection:(classic_cartoons) AND title:(superman) AND NOT sound:(silent)", superman)
                    }
                    val bettyDeferred = async {
                        repository.fetchCategory("title:(betty boop) AND mediatype:(movies) AND NOT sound:(silent)", betty)
                    }
                    val classicsDeferred = async {
                        repository.fetchCategory("(title:(casper) OR title:(herman) OR title:(daffy) OR title:(porky) OR title:(colonel bleep)) AND mediatype:(movies) AND NOT sound:(silent)", classicsSciFi)
                    }
                    val serialsDeferred = async {
                        repository.fetchCategory("(title:(felix the cat) OR title:(three stooges) OR title:(woody woodpecker)) AND mediatype:(movies) AND NOT sound:(silent)", serials)
                    }

                    val hydratedSuperman = supermanDeferred.await()
                    val hydratedPopeye = popeyeDeferred.await()
                    val hydratedBetty = bettyDeferred.await()
                    val hydratedClassics = classicsDeferred.await()
                    val hydratedSerials = serialsDeferred.await()

                    _uiState.update { current ->
                        val updated = current.copy(
                            supermanList = repository.enrichWithUserData(hydratedSuperman),
                            popeyeList = repository.enrichWithUserData(hydratedPopeye),
                            bettyList = repository.enrichWithUserData(hydratedBetty),
                            classicsSciFiList = repository.enrichWithUserData(hydratedClassics),
                            serialsList = repository.enrichWithUserData(hydratedSerials)
                        )
                        val rebuiltSeries = buildSeriesFromState(updated, latestFavoriteSeriesIds)
                        val refreshedSelectedSeries = updated.selectedSeries?.let { sel ->
                            rebuiltSeries.firstOrNull { it.id == sel.id } ?: sel
                        }
                        val refreshedEpisodes = refreshedSelectedSeries?.let { sel ->
                            CartoonSeriesCatalog.filterEpisodesForSeries(sel.id, allCatalogItemsFromState(updated))
                        } ?: emptyList()

                        updated.copy(
                            allSeries = rebuiltSeries,
                            selectedSeries = refreshedSelectedSeries,
                            selectedSeriesEpisodes = refreshedEpisodes,
                            liveChannels = buildLiveChannelsFromState(updated)
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun observeFavoriteSeries() {
        viewModelScope.launch {
            repository.favoriteSeries.collect { savedSeries ->
                val savedIds = savedSeries.map { it.id }.toSet()
                latestFavoriteSeriesIds = savedIds
                _uiState.update { state ->
                    val updatedAllSeries = buildSeriesFromState(state, savedIds)
                    val catalogById = updatedAllSeries.associateBy { it.id }
                    // Keep saved Room ordering (most recently favorited first) while hydrating live episode counts
                    val enrichedFavorites = savedSeries.map { saved ->
                        val live = catalogById[saved.id]
                        if (live != null) {
                            live.copy(isFavorite = true, addedTimestamp = saved.addedTimestamp)
                        } else {
                            saved.copy(isFavorite = true)
                        }
                    }
                    val updatedSelected = state.selectedSeries?.let { sel ->
                        catalogById[sel.id] ?: sel.copy(isFavorite = savedIds.contains(sel.id))
                    }
                    state.copy(
                        allSeries = updatedAllSeries,
                        favoriteSeries = enrichedFavorites,
                        selectedSeries = updatedSelected
                    )
                }
            }
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            combine(
                repository.watchlist,
                repository.roomWatchlist,
                repository.watchProgress,
                repository.watchDuration
            ) { ids, roomItems, progressMap, durationMap ->
                _uiState.update { state ->
                    val allKnown = allCatalogItemsFromState(state)
                    val favFromKnown = allKnown.filter { ids.contains(it.id) }.map {
                        it.copy(
                            isFavorite = true,
                            watchProgressMs = progressMap[it.id] ?: it.watchProgressMs,
                            durationMs = durationMap[it.id] ?: it.durationMs
                        )
                    }
                    val combinedWatchlist = (roomItems + favFromKnown).distinctBy { it.id }
                    val updatedFeatured = repository.enrichWithUserData(state.featuredList)
                    val updatedSuperman = repository.enrichWithUserData(state.supermanList)
                    val updatedPopeye = repository.enrichWithUserData(state.popeyeList)
                    val updatedBetty = repository.enrichWithUserData(state.bettyList)
                    val updatedClassics = repository.enrichWithUserData(state.classicsSciFiList)
                    val updatedSerials = repository.enrichWithUserData(state.serialsList)
                    val updatedSearch = repository.enrichWithUserData(state.searchResults)
                    val updatedSeriesEpisodes = repository.enrichWithUserData(state.selectedSeriesEpisodes)

                    state.copy(
                        watchlistItems = combinedWatchlist,
                        featuredList = updatedFeatured,
                        supermanList = updatedSuperman,
                        popeyeList = updatedPopeye,
                        bettyList = updatedBetty,
                        classicsSciFiList = updatedClassics,
                        serialsList = updatedSerials,
                        searchResults = updatedSearch,
                        selectedSeriesEpisodes = updatedSeriesEpisodes,
                        heroItem = state.heroItem?.let {
                            it.copy(
                                isFavorite = ids.contains(it.id),
                                watchProgressMs = progressMap[it.id] ?: it.watchProgressMs,
                                durationMs = durationMap[it.id] ?: it.durationMs
                            )
                        },
                        selectedDetailItem = state.selectedDetailItem?.let {
                            it.copy(
                                isFavorite = ids.contains(it.id),
                                watchProgressMs = progressMap[it.id] ?: it.watchProgressMs,
                                durationMs = durationMap[it.id] ?: it.durationMs
                            )
                        },
                        playingItem = state.playingItem?.let {
                            it.copy(isFavorite = ids.contains(it.id))
                        }
                    )
                }
            }.collect {}
        }
    }

    private fun observeRecentlyWatched() {
        viewModelScope.launch {
            repository.recentlyWatched.collect { recents ->
                _uiState.update { it.copy(recentlyWatchedList = recents) }
            }
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(isSplashVisible = false) }
        viewModelScope.launch {
            delay(1500)
            checkSuggestions()
        }
    }

    fun setInPipMode(inPip: Boolean) {
        _uiState.update { it.copy(isInPipMode = inPip) }
    }

    fun setCategoryFilter(filter: String) {
        _uiState.update { it.copy(categoryFilter = filter) }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setHero(item: CartoonItem) {
        _uiState.update { it.copy(heroItem = item) }
    }

    fun selectDetail(item: CartoonItem?) {
        _uiState.update { it.copy(selectedDetailItem = item) }
    }

    fun resolveSeriesForItem(item: CartoonItem): CartoonSeries? {
        return CartoonSeriesCatalog.resolveSeriesForItem(item, _uiState.value.allSeries)
    }

    fun openSeries(series: CartoonSeries) {
        _uiState.update { state ->
            val liveSeries = state.allSeries.firstOrNull { it.id == series.id } ?: series
            val episodes = CartoonSeriesCatalog.filterEpisodesForSeries(
                seriesId = liveSeries.id,
                allItems = allCatalogItemsFromState(state)
            )
            state.copy(
                selectedSeries = liveSeries,
                selectedSeriesEpisodes = episodes,
                selectedDetailItem = null
            )
        }
    }

    fun closeSeries() {
        _uiState.update { it.copy(selectedSeries = null, selectedSeriesEpisodes = emptyList()) }
    }

    fun toggleFavoriteSeries(series: CartoonSeries) {
        val appContext = getApplication<Application>()
        viewModelScope.launch {
            val added = repository.toggleFavoriteSeries(series)
            if (added) {
                engagementManager.recordWatchlistAdd()
                donationManager.recordSuccessfulOperation()
                updateSupporterAndBannerState()
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.series_saved_toast, series.title),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.series_removed_toast, series.title),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun playSeriesMarathon(series: CartoonSeries) {
        val state = _uiState.value
        val episodes = CartoonSeriesCatalog.filterEpisodesForSeries(
            seriesId = series.id,
            allItems = allCatalogItemsFromState(state)
        )
        val first = episodes.firstOrNull() ?: return
        val second = episodes.getOrNull(1)
        closeSeries()
        startPlayback(
            item = first,
            upNext = second ?: resolveUpNextFor(first),
            liveChannel = null,
            liveOffsetMs = 0L
        )
    }

    fun playFavoritesMarathon() {
        val state = _uiState.value
        val allPool = allCatalogItemsFromState(state)
        val seriesEpisodes = state.favoriteSeries.flatMap { favSeries ->
            CartoonSeriesCatalog.filterEpisodesForSeries(favSeries.id, allPool)
        }
        val combinedPool = (state.watchlistItems + seriesEpisodes).distinctBy { it.id }
        val first = combinedPool.firstOrNull() ?: return
        val second = combinedPool.getOrNull(1)
        startPlayback(
            item = first,
            upNext = second ?: resolveUpNextFor(first),
            liveChannel = null,
            liveOffsetMs = 0L
        )
    }

    fun toggleWatchlist(item: CartoonItem) {
        val willBeAdded = !item.isFavorite
        repository.toggleWatchlist(item)
        if (willBeAdded) {
            engagementManager.recordWatchlistAdd()
            donationManager.recordSuccessfulOperation()
            updateSupporterAndBannerState()
            viewModelScope.launch {
                delay(600)
                checkSuggestions()
            }
        }
    }

    fun saveProgress(itemId: String, positionMs: Long, durationMs: Long = 0L) {
        repository.saveWatchProgress(itemId, positionMs, durationMs)
    }

    private fun resolveUpNextFor(item: CartoonItem): CartoonItem? {
        val state = _uiState.value
        val candidateLists = listOf(
            state.selectedSeriesEpisodes,
            state.supermanList,
            state.classicsSciFiList,
            state.popeyeList,
            state.bettyList,
            state.serialsList,
            state.watchlistItems,
            state.searchResults,
            state.featuredList
        )
        val matchingList = candidateLists.firstOrNull { list -> list.size > 1 && list.any { it.id == item.id } }
            ?: allCatalogItemsFromState(state)

        if (matchingList.isEmpty()) return null
        val idx = matchingList.indexOfFirst { it.id == item.id }
        return if (idx in 0 until matchingList.size - 1) {
            matchingList[idx + 1]
        } else {
            matchingList.firstOrNull { it.id != item.id } ?: matchingList.firstOrNull()
        }
    }

    fun playItem(item: CartoonItem) {
        startPlayback(
            item = item,
            upNext = resolveUpNextFor(item),
            liveChannel = null,
            liveOffsetMs = 0L
        )
    }

    /**
     * Tunes into a 24/7 deterministic Live TV channel at the exact live UTC broadcast offset.
     */
    fun tuneLiveChannel(channel: LiveTvChannel) {
        val program = LiveTvChannelEngine.getCurrentProgram(channel) ?: return
        startPlayback(
            item = program.currentItem,
            upNext = program.upNextItem,
            liveChannel = channel,
            liveOffsetMs = program.offsetMs
        )
    }

    /**
     * Channel surfing (CH+ / CH-) inside the full-screen Live TV player.
     */
    fun surfLiveChannel(delta: Int) {
        val channels = _uiState.value.liveChannels
        if (channels.isEmpty()) return
        val currentId = _uiState.value.activeLiveChannel?.id
        val currentIdx = channels.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        val nextIdx = Math.floorMod(currentIdx + delta, channels.size)
        tuneLiveChannel(channels[nextIdx])
    }

    private fun startPlayback(
        item: CartoonItem,
        upNext: CartoonItem?,
        liveChannel: LiveTvChannel?,
        liveOffsetMs: Long
    ) {
        streamResolveJob?.cancel()
        _uiState.update {
            it.copy(
                playingItem = item,
                upNextItem = upNext,
                activeLiveChannel = liveChannel,
                liveStartOffsetMs = liveOffsetMs,
                selectedDetailItem = null,
                selectedSeries = null,
                selectedSeriesEpisodes = emptyList(),
                activeSuggestion = null,
                isResolvingStream = true,
                resolvedStreamUrl = null,
                resolvedSubtitleUrl = null,
                candidateStreamUrls = emptyList(),
                activeStreamIndex = 0,
                streamError = null
            )
        }

        val expectedItemId = item.id
        val expectedChannelId = liveChannel?.id
        streamResolveJob = viewModelScope.launch {
            if (liveChannel == null) {
                try {
                    repository.recordRecentlyWatched(item)
                } catch (_: Exception) {}
            }

            val candidates = repository.resolveAllStreamUrls(item)
            val primaryUrl = candidates.firstOrNull() ?: item.getResolvedStreamUrl()
            val subtitleUrl = repository.getSubtitleUrl(item.id)

            // Guard against stale async completion when user switched items or surfed channels rapidly
            _uiState.update { currentState ->
                if (currentState.playingItem?.id == expectedItemId &&
                    currentState.activeLiveChannel?.id == expectedChannelId
                ) {
                    currentState.copy(
                        candidateStreamUrls = candidates,
                        resolvedStreamUrl = primaryUrl,
                        resolvedSubtitleUrl = subtitleUrl,
                        isResolvingStream = false
                    )
                } else {
                    currentState
                }
            }
        }
    }

    fun playRandomShuffle() {
        val state = _uiState.value
        val allPool = (allCatalogItemsFromState(state) + state.watchlistItems).distinctBy { it.id }
        if (allPool.isEmpty()) return
        val currentId = state.playingItem?.id ?: state.heroItem?.id
        val candidates = allPool.filter { it.id != currentId }.ifEmpty { allPool }
        playItem(candidates.random())
    }

    fun clearRecentlyWatched() {
        viewModelScope.launch {
            repository.clearRecentlyWatched()
        }
    }

    fun removeRecentlyWatched(id: String) {
        viewModelScope.launch {
            repository.removeRecentlyWatched(id)
        }
    }

    fun retryStreamWithAlternative() {
        val state = _uiState.value
        val candidates = state.candidateStreamUrls
        if (candidates.size > 1) {
            val nextIndex = (state.activeStreamIndex + 1) % candidates.size
            _uiState.update {
                it.copy(
                    activeStreamIndex = nextIndex,
                    resolvedStreamUrl = candidates[nextIndex],
                    streamError = null,
                    isResolvingStream = false
                )
            }
        } else {
            val liveChannel = state.activeLiveChannel
            val item = state.playingItem ?: return
            if (liveChannel != null) {
                startPlayback(
                    item = item,
                    upNext = state.upNextItem,
                    liveChannel = liveChannel,
                    liveOffsetMs = state.liveStartOffsetMs
                )
            } else {
                playItem(item)
            }
        }
    }

    fun closePlayer() {
        streamResolveJob?.cancel()
        val previousPlaying = _uiState.value.playingItem
        _uiState.update {
            it.copy(
                playingItem = null,
                upNextItem = null,
                activeLiveChannel = null,
                liveStartOffsetMs = 0L,
                resolvedStreamUrl = null,
                resolvedSubtitleUrl = null,
                candidateStreamUrls = emptyList(),
                isResolvingStream = false,
                streamError = null
            )
        }

        if (previousPlaying != null) {
            engagementManager.recordShortCompleted()
            donationManager.recordSuccessfulOperation()
            updateSupporterAndBannerState()

            if (donationManager.shouldTriggerAutoPrompt()) {
                _uiState.update { it.copy(showDonationDialog = true) }
            } else {
                viewModelScope.launch {
                    delay(800)
                    checkSuggestions()
                }
            }
        }
    }

    fun playNext() {
        val state = _uiState.value
        val current = state.playingItem ?: return
        val liveChannel = state.activeLiveChannel
        if (liveChannel != null) {
            val nextInChannel = state.upNextItem
                ?: LiveTvChannelEngine.getNextItemInChannel(liveChannel, current.id)
                ?: return
            val afterNext = LiveTvChannelEngine.getNextItemInChannel(liveChannel, nextInChannel.id)
            startPlayback(
                item = nextInChannel,
                upNext = afterNext,
                liveChannel = liveChannel,
                liveOffsetMs = 0L
            )
            return
        }
        val nextItem = state.upNextItem ?: resolveUpNextFor(current)
        if (nextItem != null) {
            playItem(nextItem)
        }
    }

    fun setFilmEffect(effect: FilmEffect) {
        _uiState.update { it.copy(filmEffect = effect) }
    }

    fun setAspectMode(mode: AspectMode) {
        _uiState.update { it.copy(aspectMode = mode) }
    }

    fun onSearchQueryChanged(query: String) {
        val fallback = LiveTvChannelEngine.detectCopyrightedFranchiseQuery(query)
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchFallbackSuggestion = fallback
            )
        }
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchFallbackSuggestion = null,
                    isSearching = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(360) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            val trimmed = query.trim()
            val state = _uiState.value
            val localMatches = (state.featuredList +
                state.supermanList +
                state.classicsSciFiList +
                state.popeyeList +
                state.bettyList +
                state.serialsList)
                .distinctBy { it.id }
                .filter {
                    it.title.contains(trimmed, ignoreCase = true) ||
                        it.creator.contains(trimmed, ignoreCase = true) ||
                        it.category.contains(trimmed, ignoreCase = true) ||
                        it.description.contains(trimmed, ignoreCase = true)
                }
            val archiveQuery = if (trimmed.contains(":")) {
                trimmed
            } else {
                "title:($trimmed) AND mediatype:(movies) AND (collection:(animationandcartoons) OR collection:(classic_cartoons) OR collection:(classic_tv) OR collection:(feature_films) OR subject:(cartoon) OR subject:(animation))"
            }
            val remoteResults = repository.searchCartoons(archiveQuery)
            val combinedResults = (localMatches + remoteResults).distinctBy { it.id }
            _uiState.update {
                it.copy(
                    searchResults = combinedResults,
                    isSearching = false
                )
            }
        }
    }

    // Engagement suggestion methods
    fun checkSuggestions() {
        val state = _uiState.value
        if (state.playingItem == null && !state.isSplashVisible && state.selectedDetailItem == null && state.selectedSeries == null) {
            val suggestion = engagementManager.evaluatePendingSuggestion(isCurrentlyPlaying = false)
            if (suggestion != null) {
                _uiState.update { it.copy(activeSuggestion = suggestion) }
            }
        }
    }

    fun onSuggestionPrimary() {
        val current = _uiState.value.activeSuggestion ?: return
        engagementManager.onSuggestionActionTaken(current.type, isPositiveAction = true)
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    fun onSuggestionSecondary() {
        val current = _uiState.value.activeSuggestion ?: return
        engagementManager.onSuggestionActionTaken(current.type, isPositiveAction = false)
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    fun onSuggestionNever() {
        val current = _uiState.value.activeSuggestion ?: return
        engagementManager.dismissForever(current.type)
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    fun dismissSuggestion() {
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    // Donation methods
    fun updateSupporterAndBannerState() {
        _uiState.update {
            it.copy(
                isSupporter = donationManager.isSupporter,
                supporterTier = donationManager.supporterTier,
                showHomeDonationBanner = donationManager.shouldShowHomeBanner()
            )
        }
    }

    fun openDonationDialog() {
        _uiState.update { it.copy(showDonationDialog = true) }
    }

    fun closeDonationDialog() {
        _uiState.update { it.copy(showDonationDialog = false) }
        donationManager.dismissAutoPrompt()
        updateSupporterAndBannerState()
    }

    fun dismissHomeDonationBanner() {
        donationManager.dismissHomeBanner()
        _uiState.update { it.copy(showHomeDonationBanner = false) }
    }

    fun onSupporterStatusUpdated(tier: String) {
        donationManager.markAsSupporter(tier)
        updateSupporterAndBannerState()
    }

    fun downloadCartoon(item: CartoonItem) {
        val appContext = getApplication<Application>()
        viewModelScope.launch {
            val candidates = repository.resolveAllStreamUrls(item)
            val resolvedUrl = candidates.firstOrNull() ?: item.getResolvedStreamUrl()
            DownloadHelper(appContext).downloadCartoon(item, resolvedUrl)
        }
    }

    fun copyStreamUrl(item: CartoonItem) {
        val appContext = getApplication<Application>()
        viewModelScope.launch {
            val candidates = repository.resolveAllStreamUrls(item)
            val resolvedUrl = candidates.firstOrNull() ?: item.getResolvedStreamUrl()
            try {
                val clip = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("Stream URL", resolvedUrl))
                Toast.makeText(appContext, appContext.getString(R.string.mp4_link_copied_toast), Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }
}
