package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.donation.DonationManager
import com.example.data.engagement.AppSuggestion
import com.example.data.engagement.EngagementManager
import com.example.data.model.CartoonItem
import com.example.data.repository.CartoonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FilmEffect(val title: String) {
    CLEAN("Clean Digital"),
    SEPIA_GRAIN("1930s Film Reel"),
    BLACK_WHITE("B&W Cinema"),
    CRT_GLOW("Retro CRT")
}

enum class AspectMode(val title: String) {
    ORIGINAL("4:3 Academy"),
    FIT("Fit Screen"),
    FILL("16:9 Fill")
}

data class UiState(
    val isSplashVisible: Boolean = true,
    val isInPipMode: Boolean = false,
    val currentTab: String = "home", // "home", "search", "watchlist"
    val categoryFilter: String = "ALL", // "ALL", "SUPERMAN", "POPEYE", "BETTY", "SERIALS"
    val heroItem: CartoonItem? = null,
    val featuredList: List<CartoonItem> = emptyList(),
    val supermanList: List<CartoonItem> = emptyList(),
    val popeyeList: List<CartoonItem> = emptyList(),
    val bettyList: List<CartoonItem> = emptyList(),
    val serialsList: List<CartoonItem> = emptyList(),
    val watchlistItems: List<CartoonItem> = emptyList(),
    val selectedDetailItem: CartoonItem? = null,
    val playingItem: CartoonItem? = null,
    val upNextItem: CartoonItem? = null,
    val resolvedStreamUrl: String? = null,
    val candidateStreamUrls: List<String> = emptyList(),
    val activeStreamIndex: Int = 0,
    val isResolvingStream: Boolean = false,
    val streamError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<CartoonItem> = emptyList(),
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
)

class RetroToonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CartoonRepository(application.applicationContext)
    private val engagementManager = EngagementManager(application.applicationContext)
    val donationManager = DonationManager(application.applicationContext)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        engagementManager.recordAppLaunch()
        donationManager.recordAppLaunch()
        updateSupporterAndBannerState()
        loadInitialCatalog()
        observeWatchlist()
        observeRecentlyWatched()
    }

    private fun loadInitialCatalog() {
        val featured = repository.getCuratedFeatured()
        val superman = repository.getCuratedSuperman()
        val popeye = repository.getCuratedPopeye()
        val betty = repository.getCuratedBettyBoop()
        val serials = repository.getCuratedSerials()

        _uiState.value = _uiState.value.copy(
            heroItem = featured.firstOrNull(),
            featuredList = repository.enrichWithUserData(featured),
            supermanList = repository.enrichWithUserData(superman),
            popeyeList = repository.enrichWithUserData(popeye),
            bettyList = repository.enrichWithUserData(betty),
            serialsList = repository.enrichWithUserData(serials)
        )

        // Async fetch more items from Archive.org in the background
        viewModelScope.launch {
            try {
                val remotePopeye = repository.fetchCategory("collection:(classic_cartoons) AND title:(popeye)", popeye)
                val remoteSuperman = repository.fetchCategory("collection:(classic_cartoons) AND title:(superman)", superman)
                val remoteBetty = repository.fetchCategory("title:(betty boop) AND mediatype:(movies)", betty)
                val remoteSerials = repository.fetchCategory("title:(buster keaton) AND mediatype:(movies)", serials)

                _uiState.value = _uiState.value.copy(
                    supermanList = remoteSuperman,
                    popeyeList = remotePopeye,
                    bettyList = remoteBetty,
                    serialsList = remoteSerials
                )
            } catch (_: Exception) {}
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                repository.watchlist,
                repository.roomWatchlist,
                repository.watchProgress,
                repository.watchDuration
            ) { ids, roomItems, progressMap, durationMap ->
                val allKnown = (_uiState.value.featuredList +
                        _uiState.value.supermanList +
                        _uiState.value.popeyeList +
                        _uiState.value.bettyList +
                        _uiState.value.serialsList +
                        _uiState.value.searchResults).distinctBy { it.id }

                val favFromKnown = allKnown.filter { ids.contains(it.id) }.map {
                    it.copy(
                        isFavorite = true,
                        watchProgressMs = progressMap[it.id] ?: it.watchProgressMs,
                        durationMs = durationMap[it.id] ?: it.durationMs
                    )
                }
                val combinedWatchlist = (roomItems + favFromKnown).distinctBy { it.id }

                _uiState.value = _uiState.value.copy(
                    watchlistItems = combinedWatchlist,
                    featuredList = repository.enrichWithUserData(_uiState.value.featuredList),
                    supermanList = repository.enrichWithUserData(_uiState.value.supermanList),
                    popeyeList = repository.enrichWithUserData(_uiState.value.popeyeList),
                    bettyList = repository.enrichWithUserData(_uiState.value.bettyList),
                    serialsList = repository.enrichWithUserData(_uiState.value.serialsList),
                    searchResults = repository.enrichWithUserData(_uiState.value.searchResults),
                    heroItem = _uiState.value.heroItem?.let {
                        it.copy(
                            isFavorite = ids.contains(it.id),
                            watchProgressMs = progressMap[it.id] ?: it.watchProgressMs,
                            durationMs = durationMap[it.id] ?: it.durationMs
                        )
                    },
                    selectedDetailItem = _uiState.value.selectedDetailItem?.let {
                        it.copy(
                            isFavorite = ids.contains(it.id),
                            watchProgressMs = progressMap[it.id] ?: it.watchProgressMs,
                            durationMs = durationMap[it.id] ?: it.durationMs
                        )
                    }
                )
            }.collect {}
        }
    }

    private fun observeRecentlyWatched() {
        viewModelScope.launch {
            repository.recentlyWatched.collect { recents ->
                _uiState.value = _uiState.value.copy(recentlyWatchedList = recents)
            }
        }
    }

    fun dismissSplash() {
        _uiState.value = _uiState.value.copy(isSplashVisible = false)
        // Gentle check after splash if eligible
        viewModelScope.launch {
            delay(1500)
            checkSuggestions()
        }
    }

    fun setInPipMode(inPip: Boolean) {
        _uiState.value = _uiState.value.copy(isInPipMode = inPip)
    }

    fun setCategoryFilter(filter: String) {
        _uiState.value = _uiState.value.copy(categoryFilter = filter)
    }

    fun selectTab(tab: String) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun setHero(item: CartoonItem) {
        _uiState.value = _uiState.value.copy(heroItem = item)
    }

    fun selectDetail(item: CartoonItem?) {
        _uiState.value = _uiState.value.copy(selectedDetailItem = item)
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
            state.supermanList,
            state.popeyeList,
            state.bettyList,
            state.serialsList,
            state.watchlistItems,
            state.searchResults,
            state.featuredList
        )
        val matchingList = candidateLists.firstOrNull { list -> list.size > 1 && list.any { it.id == item.id } }
            ?: (state.featuredList + state.supermanList + state.popeyeList + state.bettyList + state.serialsList).distinctBy { it.id }

        if (matchingList.isEmpty()) return null
        val idx = matchingList.indexOfFirst { it.id == item.id }
        return if (idx in 0 until matchingList.size - 1) {
            matchingList[idx + 1]
        } else {
            matchingList.firstOrNull { it.id != item.id } ?: matchingList.firstOrNull()
        }
    }

    fun playItem(item: CartoonItem) {
        val nextUp = resolveUpNextFor(item)
        _uiState.value = _uiState.value.copy(
            playingItem = item,
            upNextItem = nextUp,
            selectedDetailItem = null,
            activeSuggestion = null, // Close any suggestion during playback
            isResolvingStream = true,
            resolvedStreamUrl = null,
            candidateStreamUrls = emptyList(),
            activeStreamIndex = 0,
            streamError = null
        )

        viewModelScope.launch {
            // Track in local Room database
            try {
                repository.recordRecentlyWatched(item)
            } catch (_: Exception) {}

            val candidates = repository.resolveAllStreamUrls(item)
            val primaryUrl = candidates.firstOrNull() ?: item.getResolvedStreamUrl()
            _uiState.value = _uiState.value.copy(
                candidateStreamUrls = candidates,
                resolvedStreamUrl = primaryUrl,
                isResolvingStream = false
            )
        }
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
            _uiState.value = state.copy(
                activeStreamIndex = nextIndex,
                resolvedStreamUrl = candidates[nextIndex],
                streamError = null,
                isResolvingStream = false
            )
        } else {
            state.playingItem?.let { playItem(it) }
        }
    }

    fun closePlayer() {
        val previousPlaying = _uiState.value.playingItem
        _uiState.value = _uiState.value.copy(
            playingItem = null,
            upNextItem = null,
            resolvedStreamUrl = null,
            candidateStreamUrls = emptyList(),
            isResolvingStream = false,
            streamError = null
        )

        if (previousPlaying != null) {
            engagementManager.recordShortCompleted()
            donationManager.recordSuccessfulOperation()
            updateSupporterAndBannerState()

            if (donationManager.shouldTriggerAutoPrompt()) {
                _uiState.value = _uiState.value.copy(showDonationDialog = true)
            } else {
                viewModelScope.launch {
                    delay(800) // Graceful pause before offering milestone suggestion
                    checkSuggestions()
                }
            }
        }
    }

    fun playNext() {
        val current = _uiState.value.playingItem ?: return
        val nextItem = _uiState.value.upNextItem ?: resolveUpNextFor(current)
        if (nextItem != null) {
            playItem(nextItem)
        }
    }

    fun setFilmEffect(effect: FilmEffect) {
        _uiState.value = _uiState.value.copy(filmEffect = effect)
    }

    fun setAspectMode(mode: AspectMode) {
        _uiState.value = _uiState.value.copy(aspectMode = mode)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce
            _uiState.value = _uiState.value.copy(isSearching = true)
            val archiveQuery = if (query.contains(":")) {
                query
            } else {
                "title:($query) AND mediatype:(movies)"
            }
            val results = repository.searchCartoons(archiveQuery)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    // Engagement suggestion methods
    fun checkSuggestions() {
        val state = _uiState.value
        if (state.playingItem == null && !state.isSplashVisible && state.selectedDetailItem == null) {
            val suggestion = engagementManager.evaluatePendingSuggestion(isCurrentlyPlaying = false)
            if (suggestion != null) {
                _uiState.value = _uiState.value.copy(activeSuggestion = suggestion)
            }
        }
    }

    fun onSuggestionPrimary() {
        val current = _uiState.value.activeSuggestion ?: return
        engagementManager.onSuggestionActionTaken(current.type, isPositiveAction = true)
        _uiState.value = _uiState.value.copy(activeSuggestion = null)
    }

    fun onSuggestionSecondary() {
        val current = _uiState.value.activeSuggestion ?: return
        engagementManager.onSuggestionActionTaken(current.type, isPositiveAction = false)
        _uiState.value = _uiState.value.copy(activeSuggestion = null)
    }

    fun onSuggestionNever() {
        val current = _uiState.value.activeSuggestion ?: return
        engagementManager.dismissForever(current.type)
        _uiState.value = _uiState.value.copy(activeSuggestion = null)
    }

    fun dismissSuggestion() {
        _uiState.value = _uiState.value.copy(activeSuggestion = null)
    }

    // Donation methods
    fun updateSupporterAndBannerState() {
        _uiState.value = _uiState.value.copy(
            isSupporter = donationManager.isSupporter,
            supporterTier = donationManager.supporterTier,
            showHomeDonationBanner = donationManager.shouldShowHomeBanner()
        )
    }

    fun openDonationDialog() {
        _uiState.value = _uiState.value.copy(showDonationDialog = true)
    }

    fun closeDonationDialog() {
        _uiState.value = _uiState.value.copy(showDonationDialog = false)
        donationManager.dismissAutoPrompt()
        updateSupporterAndBannerState()
    }

    fun dismissHomeDonationBanner() {
        donationManager.dismissHomeBanner()
        _uiState.value = _uiState.value.copy(showHomeDonationBanner = false)
    }

    fun onSupporterStatusUpdated(tier: String) {
        donationManager.markAsSupporter(tier)
        updateSupporterAndBannerState()
    }
}
