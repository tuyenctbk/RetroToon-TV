package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ArchiveApiClient
import com.example.data.local.RecentlyWatchedDao
import com.example.data.local.RecentlyWatchedEntity
import com.example.data.local.RetroToonDatabase
import com.example.data.model.CartoonItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.example.data.local.FavoriteSeriesDao
import com.example.data.local.FavoriteSeriesEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import com.example.data.model.CartoonSeries
import kotlinx.coroutines.flow.map

class CartoonRepository(
    context: Context,
    private val apiClient: ArchiveApiClient = ArchiveApiClient(),
    private val recentlyWatchedDao: RecentlyWatchedDao = RetroToonDatabase.getInstance(context).recentlyWatchedDao(),
    private val watchlistDao: WatchlistDao = RetroToonDatabase.getInstance(context).watchlistDao(),
    private val favoriteSeriesDao: FavoriteSeriesDao = RetroToonDatabase.getInstance(context).favoriteSeriesDao()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("retrotoon_prefs", Context.MODE_PRIVATE)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    private val _watchProgress = MutableStateFlow<Map<String, Long>>(emptyMap())
    val watchProgress: StateFlow<Map<String, Long>> = _watchProgress.asStateFlow()

    private val _watchDuration = MutableStateFlow<Map<String, Long>>(emptyMap())
    val watchDuration: StateFlow<Map<String, Long>> = _watchDuration.asStateFlow()

    // Reactive Watchlist flow directly from Room database
    val roomWatchlist: Flow<List<CartoonItem>> = combine(
        watchlistDao.getAllWatchlist(),
        _watchProgress,
        _watchDuration
    ) { entities, progress, durations ->
        entities.map { entity ->
            entity.toCartoonItem().copy(
                watchProgressMs = progress[entity.id] ?: entity.watchProgressMs,
                durationMs = durations[entity.id] ?: entity.durationMs
            )
        }
    }

    // Reactive stream of recently watched items from Room DB enriched with favorite & progress state
    val recentlyWatched: Flow<List<CartoonItem>> =
        combine(recentlyWatchedDao.getRecentlyWatched(), _watchlist, _watchProgress, _watchDuration) { entities, favs, progress, durations ->
            entities.map { entity ->
                entity.toCartoonItem(isFavorite = favs.contains(entity.id)).copy(
                    watchProgressMs = progress[entity.id] ?: entity.watchProgressMs,
                    durationMs = durations[entity.id] ?: entity.durationMs
                )
            }
        }

    // Reactive Favorite Cartoon Series flows directly from local Room database
    val favoriteSeries: Flow<List<CartoonSeries>> = favoriteSeriesDao
        .getAllFavoriteSeries()
        .map { entities -> entities.map { it.toCartoonSeries() } }

    val favoriteSeriesIds: Flow<Set<String>> = favoriteSeriesDao
        .getAllFavoriteSeriesIds()
        .map { it.toSet() }

    // Resolved stream URL cache (id -> direct mp4 url)
    private val streamUrlCache = ConcurrentHashMap<String, String>()

    // Guards optimistic watchlist toggles against stale in-flight Room Flow emissions
    private val watchlistMutex = Mutex()
    private val pendingWatchlistWrites = AtomicInteger(0)

    init {
        loadPreferences()
        syncWatchlistFromRoom()
    }

    private fun syncWatchlistFromRoom() {
        repositoryScope.launch {
            var didInitialSync = false
            watchlistDao.getAllWatchlistIds().collect { roomIds ->
                val roomSet = roomIds.toSet()
                if (!didInitialSync) {
                    didInitialSync = true
                    if (roomSet.isEmpty() && _watchlist.value.isNotEmpty() &&
                        pendingWatchlistWrites.get() == 0 &&
                        !prefs.getBoolean("watchlist_migrated_to_room", false)
                    ) {
                        // One-time migration from legacy SharedPreferences favorites into Room
                        val curatedLookup = (getCuratedFeatured() + getCuratedSuperman() + getCuratedPopeye() +
                            getCuratedBettyBoop() + getCuratedClassicsAndSciFi() + getCuratedSerials())
                            .associateBy { it.id }
                        watchlistMutex.withLock {
                            _watchlist.value.forEach { legacyId ->
                                curatedLookup[legacyId]?.let { item ->
                                    try {
                                        watchlistDao.insertToWatchlist(WatchlistEntity.fromCartoonItem(item))
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        prefs.edit().putBoolean("watchlist_migrated_to_room", true).apply()
                        return@collect
                    }
                    prefs.edit().putBoolean("watchlist_migrated_to_room", true).apply()
                }
                if (pendingWatchlistWrites.get() == 0) {
                    _watchlist.value = roomSet
                    prefs.edit().putStringSet("favorites_set", roomSet).apply()
                }
            }
        }
    }

    private fun loadPreferences() {
        val savedFavorites = prefs.getStringSet("favorites_set", emptySet()) ?: emptySet()
        _watchlist.value = savedFavorites

        val progressJson = prefs.getString("watch_progress_json", "{}") ?: "{}"
        try {
            val obj = JSONObject(progressJson)
            val map = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optLong(k, 0L)
            }
            _watchProgress.value = map
        } catch (e: Exception) {
            _watchProgress.value = emptyMap()
        }

        val durationJson = prefs.getString("watch_duration_json", "{}") ?: "{}"
        try {
            val obj = JSONObject(durationJson)
            val map = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optLong(k, 0L)
            }
            _watchDuration.value = map
        } catch (e: Exception) {
            _watchDuration.value = emptyMap()
        }

        val streamCacheJson = prefs.getString("stream_url_cache_json", "{}") ?: "{}"
        try {
            val obj = JSONObject(streamCacheJson)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "")
                if (v.isNotBlank()) {
                    streamUrlCache[k] = v
                }
            }
        } catch (_: Exception) {}
    }

    fun toggleWatchlist(item: CartoonItem) {
        val current = _watchlist.value.toMutableSet()
        val willRemove = current.contains(item.id)
        if (willRemove) {
            current.remove(item.id)
        } else {
            current.add(item.id)
        }
        pendingWatchlistWrites.incrementAndGet()
        _watchlist.value = current
        prefs.edit().putStringSet("favorites_set", current).apply()

        repositoryScope.launch {
            try {
                watchlistMutex.withLock {
                    if (willRemove) {
                        watchlistDao.removeFromWatchlist(item.id)
                    } else {
                        watchlistDao.insertToWatchlist(WatchlistEntity.fromCartoonItem(item))
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingWatchlistWrites.decrementAndGet()
            }
        }
    }

    suspend fun clearWatchlist() {
        pendingWatchlistWrites.incrementAndGet()
        _watchlist.value = emptySet()
        prefs.edit().putStringSet("favorites_set", emptySet()).apply()
        try {
            watchlistMutex.withLock {
                watchlistDao.clearWatchlist()
            }
        } finally {
            pendingWatchlistWrites.decrementAndGet()
        }
    }

    /**
     * Toggles a CartoonSeries in the local Room `favorite_series` table.
     * @return true if the series was added to favorites, false if removed.
     */
    suspend fun toggleFavoriteSeries(series: CartoonSeries): Boolean {
        val existing = favoriteSeriesDao.getFavoriteSeriesById(series.id)
        return if (existing != null) {
            favoriteSeriesDao.deleteFavoriteSeries(series.id)
            false
        } else {
            favoriteSeriesDao.insertFavoriteSeries(FavoriteSeriesEntity.fromCartoonSeries(series))
            true
        }
    }

    suspend fun saveFavoriteSeries(series: CartoonSeries) {
        favoriteSeriesDao.insertFavoriteSeries(FavoriteSeriesEntity.fromCartoonSeries(series))
    }

    suspend fun removeFavoriteSeries(seriesId: String) {
        favoriteSeriesDao.deleteFavoriteSeries(seriesId)
    }

    suspend fun clearFavoriteSeries() {
        favoriteSeriesDao.clearAllFavoriteSeries()
    }

    fun saveWatchProgress(itemId: String, positionMs: Long, durationMs: Long = 0L) {
        val effectivePos = if (durationMs > 5000L && positionMs >= durationMs - 2500L) 0L else positionMs.coerceAtLeast(0L)
        val current = _watchProgress.value.toMutableMap()
        current[itemId] = effectivePos
        _watchProgress.value = current

        if (durationMs > 0L) {
            val durMap = _watchDuration.value.toMutableMap()
            durMap[itemId] = durationMs
            _watchDuration.value = durMap
            try {
                val durObj = JSONObject()
                durMap.forEach { (k, v) -> durObj.put(k, v) }
                prefs.edit().putString("watch_duration_json", durObj.toString()).apply()
            } catch (_: Exception) {}
        }

        try {
            val obj = JSONObject()
            current.forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString("watch_progress_json", obj.toString()).apply()
        } catch (_: Exception) {}

        repositoryScope.launch {
            try {
                if (durationMs > 0L) {
                    recentlyWatchedDao.updateProgressAndDuration(itemId, effectivePos, durationMs)
                } else {
                    recentlyWatchedDao.updateProgress(itemId, effectivePos)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun recordRecentlyWatched(
        item: CartoonItem,
        progressMs: Long = item.watchProgressMs,
        durationMs: Long = item.durationMs
    ) {
        val currentProgress = _watchProgress.value[item.id] ?: progressMs
        val currentDuration = _watchDuration.value[item.id] ?: durationMs
        val entity = RecentlyWatchedEntity.fromCartoonItem(
            item = item,
            progressMs = currentProgress,
            durationMs = currentDuration
        )
        recentlyWatchedDao.upsertRecent(entity)
    }

    suspend fun clearRecentlyWatched() {
        recentlyWatchedDao.clearAllRecent()
    }

    suspend fun removeRecentlyWatched(id: String) {
        recentlyWatchedDao.deleteRecent(id)
    }

    suspend fun resolveStreamUrl(item: CartoonItem): String {
        return resolveAllStreamUrls(item).firstOrNull()
            ?: "https://archive.org/download/${item.id}/${item.id}_512kb.mp4"
    }

    private fun encodeArchivePath(fileName: String): String {
        return URLEncoder.encode(fileName, "UTF-8")
            .replace("+", "%20")
            .replace("%2F", "/")
    }

    suspend fun resolveAllStreamUrls(item: CartoonItem): List<String> {
        val candidates = mutableListOf<String>()
        streamUrlCache[item.id]?.takeIf { it.isNotBlank() && !it.contains(" ") }?.let { cached ->
            candidates.add(cached)
        }
        if (!item.videoStreamUrl.isNullOrBlank() && !candidates.contains(item.videoStreamUrl)) {
            candidates.add(item.videoStreamUrl)
        }
        if (!item.mp4Filename.isNullOrBlank()) {
            val mp4Url = "https://archive.org/download/${item.id}/${encodeArchivePath(item.mp4Filename)}"
            if (!candidates.contains(mp4Url)) {
                candidates.add(mp4Url)
            }
        }
        val remoteFiles = apiClient.resolveMp4Files(item.id)
        for (f in remoteFiles) {
            val u = "https://archive.org/download/${item.id}/${encodeArchivePath(f)}"
            if (!candidates.contains(u)) {
                candidates.add(u)
            }
        }
        if (candidates.isEmpty()) {
            candidates.add("https://archive.org/download/${item.id}/${encodeArchivePath("${item.id}_512kb.mp4")}")
            candidates.add("https://archive.org/download/${item.id}/${encodeArchivePath("${item.id}.mp4")}")
        }
        if (candidates.isNotEmpty()) {
            streamUrlCache[item.id] = candidates.first()
            if (streamUrlCache.size > 60) {
                streamUrlCache.keys.take(streamUrlCache.size - 60).forEach { streamUrlCache.remove(it) }
            }
            try {
                val cacheObj = JSONObject()
                streamUrlCache.forEach { (k, v) -> cacheObj.put(k, v) }
                prefs.edit().putString("stream_url_cache_json", cacheObj.toString()).apply()
            } catch (_: Exception) {}
        }
        return candidates
    }

    fun getSubtitleUrl(itemId: String): String? = apiClient.getCachedSubtitleUrl(itemId)

    suspend fun searchCartoons(query: String): List<CartoonItem> {
        val searchResults = apiClient.searchCartoons(query)
        return enrichWithUserData(searchResults)
    }

    suspend fun fetchCategory(categoryTag: String, fallbackItems: List<CartoonItem>): List<CartoonItem> {
        val remoteItems = apiClient.searchCartoons(categoryTag, rows = 25)
        val combined = if (remoteItems.isNotEmpty()) {
            // Combine with curated items to ensure top titles always appear first without duplicates
            (fallbackItems + remoteItems).distinctBy { it.id }
        } else {
            fallbackItems
        }
        return enrichWithUserData(combined)
    }

    fun enrichWithUserData(items: List<CartoonItem>): List<CartoonItem> {
        val favs = _watchlist.value
        val progress = _watchProgress.value
        val durations = _watchDuration.value
        return items.map { item ->
            item.copy(
                isFavorite = favs.contains(item.id),
                watchProgressMs = progress[item.id] ?: item.watchProgressMs,
                durationMs = durations[item.id] ?: item.durationMs
            )
        }
    }

    // Curated high quality catalog to guarantee incredible zero-buffering experience
    fun getCuratedFeatured(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "superman_the_mechanical_monsters",
            title = "Superman: The Mechanical Monsters",
            year = "1941",
            category = "Superman",
            creator = "Fleischer Studios / Paramount",
            description = "A criminal mastermind unleashes giant flying robots to loot the city's gold vault. Clark Kent and Lois Lane investigate, prompting Superman into thrilling aerial combat.",
            runtime = "11:05",
            downloads = 450000L,
            mp4Filename = "superman_the_mechanical_monsters_512kb.mp4"
        ),
        CartoonItem(
            id = "popeye_patriotic_popeye",
            title = "Popeye: Patriotic Popeye",
            year = "1957",
            category = "Popeye",
            creator = "Famous Studios",
            description = "Popeye tries to keep his mischievous nephews safe during a boisterous Fourth of July holiday, leading to explosive spinach-fueled comedy.",
            runtime = "6:08",
            downloads = 320000L,
            mp4Filename = "popeye_patriotic_popeye_512kb.mp4"
        ),
        CartoonItem(
            id = "Betty_Boop_Poor_Cinderella_1934",
            title = "Betty Boop: Poor Cinderella",
            year = "1934",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "The Fleischer brothers' first Cinecolor masterpiece. Betty Boop dreams of escaping her wicked stepfamily and attending Prince Charming's grand ball.",
            runtime = "10:11",
            downloads = 280000L,
            mp4Filename = "Betty_Boop_Poor_Cinderella_1934_512kb.mp4"
        ),
        CartoonItem(
            id = "CopsbusterKeaton",
            title = "Buster Keaton: Cops (Scored Edition)",
            year = "1922",
            category = "Buster Keaton",
            creator = "Buster Keaton Productions",
            description = "Accompanied by a lively musical score, Buster accidentally triggers a city-wide police parade chase with slapstick genius.",
            runtime = "18:15",
            downloads = 190000L,
            mp4Filename = "cops_512kb.mp4"
        ),
        CartoonItem(
            id = "superman_eleventh_hour",
            title = "Superman: Eleventh Hour",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "Superman carries out daring nocturnal sabotage missions in Yokohama harbor, outsmarting enemy searchlights and gun emplacements.",
            runtime = "8:57",
            downloads = 210000L,
            mp4Filename = "superman_eleventh_hour_512kb.mp4"
        ),
        CartoonItem(
            id = "popeye_fright_to_the_finish",
            title = "Popeye: Fright to the Finish",
            year = "1954",
            category = "Popeye",
            creator = "Famous Studios",
            description = "On Halloween night, Bluto plots spooky tricks to terrify Olive Oyl and frame Popeye, until a can of spinach turns the haunted tables.",
            runtime = "6:24",
            downloads = 275000L,
            mp4Filename = "popeye_fright_to_the_finish_512kb.mp4"
        ),
        CartoonItem(
            id = "felix_the_cat_the_goos_that_laid_the_golden_egg",
            title = "Felix the Cat: The Goose That Laid the Golden Egg",
            year = "1936",
            category = "Felix the Cat",
            creator = "Van Beuren Studios",
            description = "Felix discovers Goldie, a fabulous goose who produces solid golden eggs. When greedy Captain Kidd captures her, Felix launches a daring rescue.",
            runtime = "7:18",
            downloads = 224000L,
            mp4Filename = "felix_the_cat_the_goos_that_laid_the_golden_egg_512kb.mp4"
        ),
        CartoonItem(
            id = "popeye_shuteye_popeye",
            title = "Popeye: Shuteye Popeye",
            year = "1952",
            category = "Popeye",
            creator = "Famous Studios",
            description = "Exhausted Popeye tries desperately to get a good night's sleep, but a relentless mouse keeps sabotaging his bedroom.",
            runtime = "6:33",
            downloads = 230000L,
            mp4Filename = "popeye_shuteye_popeye_512kb.mp4"
        ),
        CartoonItem(
            id = "woody_woodpecker_pantry_panic",
            title = "Woody Woodpecker: Pantry Panic",
            year = "1941",
            category = "Classic",
            creator = "Walter Lantz Productions",
            description = "Snowed in for winter with an empty larder, Woody Woodpecker and a hungry alley cat engage in manic slapstick hijinks.",
            runtime = "6:52",
            downloads = 195000L,
            mp4Filename = "woody_woodpecker_pantry_panic_512kb.mp4"
        )
    )

    fun getCuratedSuperman(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "superman_the_mechanical_monsters",
            title = "Superman: The Mechanical Monsters",
            year = "1941",
            category = "Superman",
            creator = "Fleischer Studios / Paramount",
            description = "Robots fly across the metropolis looting jewels and bullion. Lois Lane is trapped inside a mechanical monster until Superman arrives.",
            runtime = "11:05",
            mp4Filename = "superman_the_mechanical_monsters_512kb.mp4"
        ),
        CartoonItem(
            id = "superman_1941",
            title = "Superman: The Mad Scientist",
            year = "1941",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "The Academy Award-nominated debut. A mad scientist threatens to melt the city with his electro-magnetic death beam.",
            runtime = "10:27",
            mp4Filename = "superman_1941_512kb.mp4"
        ),
        CartoonItem(
            id = "superman_billion_dollar_limited",
            title = "Superman: Billion Dollar Limited",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "Armored bandits hijack a high-speed gold bullion express train with Lois Lane aboard, forcing Superman into a breathtaking locomotive rescue.",
            runtime = "8:38"
        ),
        CartoonItem(
            id = "superman_the_arctic_giant",
            title = "Superman: The Arctic Giant",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "A prehistoric dinosaur frozen in Siberian ice thaws inside Metropolis Museum and rampages downtown until Superman intervenes.",
            runtime = "8:45"
        ),
        CartoonItem(
            id = "superman_the_bulleteers",
            title = "Superman: The Bulleteers",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "Masked criminals pilot a jet-powered bullet car through city skyscrapers, challenging Superman to stop the armored rocket mid-flight.",
            runtime = "8:05"
        ),
        CartoonItem(
            id = "superman_magnetic_telescope",
            title = "Superman: The Magnetic Telescope",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "A rogue astronomer's giant magnetic beam pulls a blazing comet straight toward Metropolis, sparking an apocalyptic race against time.",
            runtime = "7:40"
        ),
        CartoonItem(
            id = "superman_electric_earthquake",
            title = "Superman: Electric Earthquake",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "Undersea seismic generators threaten to sink Manhattan beneath the waves unless Superman destroys the subterranean power cables.",
            runtime = "8:40"
        ),
        CartoonItem(
            id = "superman_eleventh_hour",
            title = "Superman: Eleventh Hour",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "Clark Kent escapes a POW camp to execute precision sabotage across harbor shipyards, dodging torpedo nets and sirens.",
            runtime = "8:57",
            mp4Filename = "superman_eleventh_hour_512kb.mp4"
        )
    )

    fun getCuratedClassicsAndSciFi(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "a_tale_of_two_kitties",
            title = "A Tale of Two Kitties (1942)",
            year = "1942",
            category = "Slapstick & Sci-Fi",
            creator = "Leon Schlesinger / Merrie Melodies",
            description = "Two scheming alley cats, Babbit and Catstello, try every slapstick trick in the book to catch a clever little bird in this classic golden-age chase.",
            runtime = "6:45",
            downloads = 310000L
        ),
        CartoonItem(
            id = "falling_hare",
            title = "Falling Hare (1943)",
            year = "1943",
            category = "Slapstick & Sci-Fi",
            creator = "Leon Schlesinger / Merrie Melodies",
            description = "A mischievous aviation gremlin sabotages a bomber mid-flight, triggering dizzying high-altitude cartoon chaos.",
            runtime = "8:25",
            downloads = 285000L
        ),
        CartoonItem(
            id = "wackiki_wabbit",
            title = "Wackiki Wabbit (1943)",
            year = "1943",
            category = "Slapstick & Sci-Fi",
            creator = "Leon Schlesinger / Merrie Melodies",
            description = "Two shipwrecked castaways on a tropical island try to turn a witty rabbit into dinner, only to be outsmarted at every turn.",
            runtime = "6:54",
            downloads = 260000L
        ),
        CartoonItem(
            id = "yankee_doodle_daffy",
            title = "Yankee Doodle Daffy (1943)",
            year = "1943",
            category = "Slapstick & Sci-Fi",
            creator = "Leon Schlesinger / Looney Tunes",
            description = "A fast-talking talent agent goes to outrageous musical and acrobatic lengths to pitch his star client before a big producer leaves town.",
            runtime = "6:48",
            downloads = 240000L
        ),
        CartoonItem(
            id = "casper_the_friendly_ghost_1945",
            title = "Casper: The Friendly Ghost",
            year = "1945",
            category = "Slapstick & Sci-Fi",
            creator = "Famous Studios",
            description = "The heartwarming debut of Casper, a gentle young ghost who would much rather make friends than haunt houses.",
            runtime = "7:50",
            downloads = 290000L
        ),
        CartoonItem(
            id = "herman_and_katnip_mice_meeting_you",
            title = "Herman & Katnip: Mice Meeting You",
            year = "1950",
            category = "Slapstick & Sci-Fi",
            creator = "Famous Studios",
            description = "Classic cat-and-mouse slapstick mayhem! Streetwise mouse Herman defends his pantry pals from Katnip's elaborate traps.",
            runtime = "6:42",
            downloads = 185000L
        ),
        CartoonItem(
            id = "woody_woodpecker_pantry_panic",
            title = "Woody Woodpecker: Pantry Panic",
            year = "1941",
            category = "Slapstick & Sci-Fi",
            creator = "Walter Lantz Productions",
            description = "Snowed in for winter with an empty larder, Woody Woodpecker and a hungry alley cat engage in manic slapstick hijinks.",
            runtime = "6:52",
            downloads = 195000L,
            mp4Filename = "woody_woodpecker_pantry_panic_512kb.mp4"
        ),
        CartoonItem(
            id = "granite_hotel",
            title = "Stone Age Cartoons: Granite Hotel",
            year = "1940",
            category = "Slapstick & Sci-Fi",
            creator = "Fleischer Studios",
            description = "Twenty years before TV's prehistoric sitcoms, Fleischer Studios imagined dinosaurs as bellhops, woolly mammoths as showers, and rocky hotel comedy!",
            runtime = "6:38",
            downloads = 162000L
        ),
        CartoonItem(
            id = "colonel_bleep_1",
            title = "Colonel Bleep: Retro Space Adventures",
            year = "1957",
            category = "Slapstick & Sci-Fi",
            creator = "Soundac Productions",
            description = "The first color cartoon made for television! Futurist extraterrestrial Colonel Bleep and his sidekicks Scratch and Squeak patrol Zero Zero Island in space.",
            runtime = "5:30",
            downloads = 148000L
        )
    )

    fun getCuratedPopeye(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "popeye_patriotic_popeye",
            title = "Patriotic Popeye",
            year = "1957",
            category = "Popeye",
            creator = "Famous Studios",
            description = "Popeye babysits his rowdy nephews on Independence Day with hilarious pyrotechnic mishaps.",
            runtime = "6:08",
            mp4Filename = "popeye_patriotic_popeye_512kb.mp4"
        ),
        CartoonItem(
            id = "popeye_fright_to_the_finish",
            title = "Fright to the Finish",
            year = "1954",
            category = "Popeye",
            creator = "Famous Studios",
            description = "Halloween hijinks with Bluto pretending to be a headless ghost to woo Olive Oyl.",
            runtime = "6:24",
            mp4Filename = "popeye_fright_to_the_finish_512kb.mp4"
        ),
        CartoonItem(
            id = "popeye_taxi-turvey",
            title = "Taxi-Turvy",
            year = "1954",
            category = "Popeye",
            creator = "Famous Studios",
            description = "Popeye and Bluto operate competing taxicab businesses and fight over Olive Oyl's fare across downtown traffic.",
            runtime = "6:12",
            mp4Filename = "popeye_taxi-turvey_512kb.mp4"
        ),
        CartoonItem(
            id = "popeye_shuteye_popeye",
            title = "Shuteye Popeye",
            year = "1952",
            category = "Popeye",
            creator = "Famous Studios",
            description = "Popeye's quest for sleep turns into an epic miniature war against a defiant mouse.",
            runtime = "6:33",
            mp4Filename = "popeye_shuteye_popeye_512kb.mp4"
        )
    )

    fun getCuratedBettyBoop(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "Betty_Boop_Poor_Cinderella_1934",
            title = "Betty Boop: Poor Cinderella",
            year = "1934",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "Color classic! Betty gets help from her Fairy Godmother to meet Prince Charming in a dazzling castle ballroom.",
            runtime = "10:11",
            mp4Filename = "Betty_Boop_Poor_Cinderella_1934_512kb.mp4"
        ),
        CartoonItem(
            id = "bb_snow_white",
            title = "Betty Boop: Snow White",
            year = "1933",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "Surreal jazz masterpiece featuring Cab Calloway's iconic rotoscoped vocal performance of St. James Infirmary Blues.",
            runtime = "7:08",
            mp4Filename = "bb_snow_white_512kb.mp4"
        ),
        CartoonItem(
            id = "bb_minnie_the_moocher",
            title = "Betty Boop: Minnie the Moocher",
            year = "1932",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "Betty and Bimbo venture into a haunted cave where Cab Calloway and His Orchestra perform Minnie the Moocher.",
            runtime = "7:45",
            mp4Filename = "bb_minnie_the_moocher_512kb.mp4"
        ),
        CartoonItem(
            id = "bb_and_grampy",
            title = "Betty Boop and Grampy",
            year = "1935",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "Betty visits eccentric inventor Grampy, whose musical contraptions turn his house into a swinging dance party.",
            runtime = "7:05",
            mp4Filename = "bb_and_grampy_512kb.mp4"
        )
    )

    fun getCuratedSerials(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "felix_the_cat_the_goos_that_laid_the_golden_egg",
            title = "Felix: The Goose That Laid The Golden Egg",
            year = "1936",
            category = "Serials & Slapstick",
            creator = "Van Beuren Studios",
            description = "Felix defends his prize egg-laying goose against pirates and castle guards in vivid early Technicolor with orchestral sound.",
            runtime = "7:18",
            mp4Filename = "felix_the_cat_the_goos_that_laid_the_golden_egg_512kb.mp4"
        ),
        CartoonItem(
            id = "Felix_BoldKingCole",
            title = "Felix: Bold King Cole",
            year = "1936",
            category = "Serials & Slapstick",
            creator = "Van Beuren Studios",
            description = "Felix encounters the boastful King Cole, who must learn true humility when singing castle ghosts appear.",
            runtime = "7:34",
            mp4Filename = "Felix_BoldKingCole_512kb.mp4"
        ),
        CartoonItem(
            id = "CopsbusterKeaton",
            title = "Buster Keaton: Cops (Scored Edition)",
            year = "1922",
            category = "Serials & Slapstick",
            creator = "Buster Keaton",
            description = "Keaton's legendary slapstick chase classic accompanied by a full ragtime-era musical score.",
            runtime = "18:15",
            mp4Filename = "cops_512kb.mp4"
        ),
        CartoonItem(
            id = "disorder_in_the_court",
            title = "Three Stooges: Disorder in the Court",
            year = "1936",
            category = "Serials & Slapstick",
            creator = "Columbia Pictures",
            description = "Moe, Larry, and Curly turn a solemn courtroom trial into musical and slapstick pandemonium.",
            runtime = "16:51",
            mp4Filename = "disorder_in_the_court_512kb.mp4"
        )
    )
}
