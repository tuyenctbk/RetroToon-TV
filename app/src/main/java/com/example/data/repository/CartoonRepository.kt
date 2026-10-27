package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ArchiveApiClient
import com.example.data.model.CartoonItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class CartoonRepository(
    context: Context,
    private val apiClient: ArchiveApiClient = ArchiveApiClient()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("retrotoon_prefs", Context.MODE_PRIVATE)

    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    private val _watchProgress = MutableStateFlow<Map<String, Long>>(emptyMap())
    val watchProgress: StateFlow<Map<String, Long>> = _watchProgress.asStateFlow()

    // Resolved stream URL cache (id -> direct mp4 url)
    private val streamUrlCache = mutableMapOf<String, String>()

    init {
        loadPreferences()
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
    }

    fun toggleWatchlist(item: CartoonItem) {
        val current = _watchlist.value.toMutableSet()
        if (current.contains(item.id)) {
            current.remove(item.id)
        } else {
            current.add(item.id)
        }
        _watchlist.value = current
        prefs.edit().putStringSet("favorites_set", current).apply()
    }

    fun saveWatchProgress(itemId: String, positionMs: Long) {
        val current = _watchProgress.value.toMutableMap()
        current[itemId] = positionMs
        _watchProgress.value = current

        try {
            val obj = JSONObject()
            current.forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString("watch_progress_json", obj.toString()).apply()
        } catch (_: Exception) {}
    }

    suspend fun resolveStreamUrl(item: CartoonItem): String {
        return resolveAllStreamUrls(item).firstOrNull()
            ?: "https://archive.org/download/${item.id}/${item.id}_512kb.mp4"
    }

    suspend fun resolveAllStreamUrls(item: CartoonItem): List<String> {
        val candidates = mutableListOf<String>()
        if (!item.videoStreamUrl.isNullOrBlank()) {
            candidates.add(item.videoStreamUrl)
        }
        if (!item.mp4Filename.isNullOrBlank()) {
            candidates.add("https://archive.org/download/${item.id}/${item.mp4Filename}")
        }
        val remoteFiles = apiClient.resolveMp4Files(item.id)
        for (f in remoteFiles) {
            val u = "https://archive.org/download/${item.id}/$f"
            if (!candidates.contains(u)) {
                candidates.add(u)
            }
        }
        if (candidates.isEmpty()) {
            candidates.add("https://archive.org/download/${item.id}/${item.id}_512kb.mp4")
            candidates.add("https://archive.org/download/${item.id}/${item.id}.mp4")
        }
        if (candidates.isNotEmpty()) {
            streamUrlCache[item.id] = candidates.first()
        }
        return candidates
    }

    suspend fun searchCartoons(query: String): List<CartoonItem> {
        val searchResults = apiClient.searchCartoons(query)
        return enrichWithUserData(searchResults)
    }

    suspend fun fetchCategory(categoryTag: String, fallbackItems: List<CartoonItem>): List<CartoonItem> {
        val remoteItems = apiClient.searchCartoons(categoryTag, rows = 25)
        val combined = if (remoteItems.isNotEmpty()) {
            // Combine with curated items to ensure top titles always appear first
            val remoteIds = remoteItems.map { it.id }.toSet()
            val curatedFiltered = fallbackItems.filter { !remoteIds.contains(it.id) }
            fallbackItems + remoteItems
        } else {
            fallbackItems
        }
        return enrichWithUserData(combined)
    }

    fun enrichWithUserData(items: List<CartoonItem>): List<CartoonItem> {
        val favs = _watchlist.value
        val progress = _watchProgress.value
        return items.map { item ->
            item.copy(
                isFavorite = favs.contains(item.id),
                watchProgressMs = progress[item.id] ?: 0L
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
            id = "BusterKeatonTheElectricHouse",
            title = "Buster Keaton: The Electric House",
            year = "1923",
            category = "Buster Keaton",
            creator = "Buster Keaton Productions",
            description = "Mistaken for an electrical engineer, Buster automates an eccentric mansion with hilarious mechanical escalators, sliding dining tables, and chaotic gadgetry.",
            runtime = "22:45",
            downloads = 190000L,
            mp4Filename = "Buster Keaton - The Electric House.mp4"
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
            id = "Felix_The_Goose_That_Laid_The_Golden_Egg_1936",
            title = "Felix the Cat: The Goose That Laid the Golden Egg",
            year = "1936",
            category = "Felix the Cat",
            creator = "Van Beuren Studios",
            description = "Felix discovers Goldie, a fabulous goose who produces solid golden eggs. When greedy Captain Kidd captures her, Felix launches a daring rescue.",
            runtime = "7:18",
            downloads = 160000L,
            mp4Filename = "Felix_The_Goose_That_Laid_The_Golden_Egg_1936_512kb.mp4"
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
            id = "superman_eleventh_hour",
            title = "Superman: Eleventh Hour",
            year = "1942",
            category = "Superman",
            creator = "Fleischer Studios",
            description = "Clark Kent escapes a POW camp to execute precision sabotage across harbor shipyards, dodging torpedo nets and sirens.",
            runtime = "8:57",
            mp4Filename = "superman_eleventh_hour_512kb.mp4"
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
            id = "BettyBoopMorePep",
            title = "Betty Boop: More Pep",
            year = "1936",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "Grampy invents a pep-gas machine that supercharges everything in town into hyperspeed animation.",
            runtime = "6:14",
            mp4Filename = "BettyBoopMorePep_512kb.mp4"
        ),
        CartoonItem(
            id = "Betty_Boop_Rhythm_on_the_Reservation_1939",
            title = "Rhythm on the Reservation",
            year = "1939",
            category = "Betty Boop",
            creator = "Fleischer Studios",
            description = "Betty Boop introduces swing jazz rhythms and big band horns to a festive outdoor gathering.",
            runtime = "5:48",
            mp4Filename = "Betty_Boop_Rhythm_on_the_Reservation_1939_512kb.mp4"
        )
    )

    fun getCuratedSerials(): List<CartoonItem> = listOf(
        CartoonItem(
            id = "BusterKeatonTheElectricHouse",
            title = "Buster Keaton: The Electric House",
            year = "1923",
            category = "Serials & Slapstick",
            creator = "Buster Keaton",
            description = "Keaton's legendary gadget comedy featuring mechanical escalators, automated billiards, and comic chaos.",
            runtime = "22:45",
            mp4Filename = "Buster Keaton - The Electric House.mp4"
        ),
        CartoonItem(
            id = "Felix_The_Goose_That_Laid_The_Golden_Egg_1936",
            title = "Felix: The Goose That Laid The Golden Egg",
            year = "1936",
            category = "Serials & Slapstick",
            creator = "Van Beuren Studios",
            description = "Felix defends his prize egg-laying goose against pirates and castle guards in vivid early technicolor.",
            runtime = "7:18",
            mp4Filename = "Felix_The_Goose_That_Laid_The_Golden_Egg_1936_512kb.mp4"
        ),
        CartoonItem(
            id = "FelixTheCatBoldKingCole1936",
            title = "Felix: Bold King Cole",
            year = "1936",
            category = "Serials & Slapstick",
            creator = "Van Beuren Studios",
            description = "Felix encounters the boastful King Cole, who must learn true humility when castle ghosts appear.",
            runtime = "7:34",
            mp4Filename = "FelixTheCatBoldKingCole1936_512kb.mp4"
        )
    )
}
