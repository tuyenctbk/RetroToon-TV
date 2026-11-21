package com.example.data.model

data class CartoonSeries(
    val id: String,
    val title: String,
    val era: String,
    val studio: String,
    val categoryKey: String,
    val description: String,
    val thumbnailUrl: String,
    val episodeCount: Int,
    val accentHex: Long,
    val isFavorite: Boolean = false,
    val addedTimestamp: Long = 0L
)

/**
 * Canonical classic Public Domain cartoon series definitions and episode-to-series matching.
 */
object CartoonSeriesCatalog {

    const val SERIES_SUPERMAN = "series_superman"
    const val SERIES_POPEYE = "series_popeye"
    const val SERIES_BETTY_BOOP = "series_betty_boop"
    const val SERIES_LOONEY_PD = "series_looney_pd"
    const val SERIES_CASPER_FRIENDS = "series_casper_friends"
    const val SERIES_STONE_AGE_SCIFI = "series_stone_age_scifi"
    const val SERIES_FELIX_CAT = "series_felix_cat"
    const val SERIES_CLASSIC_SLAPSTICK = "series_classic_slapstick"

    fun buildAllSeries(
        supermanList: List<CartoonItem>,
        popeyeList: List<CartoonItem>,
        bettyList: List<CartoonItem>,
        classicsSciFiList: List<CartoonItem>,
        serialsList: List<CartoonItem>,
        favoriteSeriesIds: Set<String> = emptySet()
    ): List<CartoonSeries> {
        val allPool = (supermanList + popeyeList + bettyList + classicsSciFiList + serialsList).distinctBy { it.id }

        val looneyEpisodes = filterEpisodesForSeries(SERIES_LOONEY_PD, allPool)
        val casperEpisodes = filterEpisodesForSeries(SERIES_CASPER_FRIENDS, allPool)
        val stoneAgeSciFiEpisodes = filterEpisodesForSeries(SERIES_STONE_AGE_SCIFI, allPool)
        val felixEpisodes = filterEpisodesForSeries(SERIES_FELIX_CAT, allPool)
        val slapstickEpisodes = filterEpisodesForSeries(SERIES_CLASSIC_SLAPSTICK, allPool)

        val definitions = listOf(
            CartoonSeries(
                id = SERIES_SUPERMAN,
                title = "Fleischer's Superman",
                era = "1941–1943",
                studio = "Fleischer Studios",
                categoryKey = "SUPERMAN",
                description = "Academy Award-nominated Technicolor superhero adventures featuring pioneering rotoscoped animation.",
                thumbnailUrl = supermanList.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/superman_the_mechanical_monsters",
                episodeCount = supermanList.size.coerceAtLeast(8),
                accentHex = 0xFFE63946,
                isFavorite = favoriteSeriesIds.contains(SERIES_SUPERMAN)
            ),
            CartoonSeries(
                id = SERIES_POPEYE,
                title = "Popeye the Sailor",
                era = "1933–1957",
                studio = "Fleischer / Famous Studios",
                categoryKey = "POPEYE",
                description = "Spinach-fueled maritime brawls, Olive Oyl rescues, and classic golden-age physical comedy.",
                thumbnailUrl = popeyeList.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/popeye_patriotic_popeye",
                episodeCount = popeyeList.size.coerceAtLeast(4),
                accentHex = 0xFFFFC857,
                isFavorite = favoriteSeriesIds.contains(SERIES_POPEYE)
            ),
            CartoonSeries(
                id = SERIES_BETTY_BOOP,
                title = "Betty Boop",
                era = "1932–1939",
                studio = "Fleischer Studios",
                categoryKey = "BETTY",
                description = "Jazz-age surrealist animation featuring Cab Calloway, rotoscoped dance, and timeless musical shorts.",
                thumbnailUrl = bettyList.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/Betty_Boop_Poor_Cinderella_1934",
                episodeCount = bettyList.size.coerceAtLeast(4),
                accentHex = 0xFFC084FC,
                isFavorite = favoriteSeriesIds.contains(SERIES_BETTY_BOOP)
            ),
            CartoonSeries(
                id = SERIES_LOONEY_PD,
                title = "Golden Age Merrie Melodies",
                era = "1941–1945",
                studio = "Leon Schlesinger / Classic PD",
                categoryKey = "SLAPSTICK",
                description = "Classic public-domain golden-age chase cartoons, witty rabbits, daffy antics, and musical gags.",
                thumbnailUrl = looneyEpisodes.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/a_tale_of_two_kitties",
                episodeCount = looneyEpisodes.size.coerceAtLeast(4),
                accentHex = 0xFF38BDF8,
                isFavorite = favoriteSeriesIds.contains(SERIES_LOONEY_PD)
            ),
            CartoonSeries(
                id = SERIES_CASPER_FRIENDS,
                title = "Casper & Herman & Katnip",
                era = "1945–1955",
                studio = "Famous Studios",
                categoryKey = "SLAPSTICK",
                description = "Heartwarming debut of Casper the Friendly Ghost plus classic cat-and-mouse slapstick duels.",
                thumbnailUrl = casperEpisodes.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/casper_the_friendly_ghost_1945",
                episodeCount = casperEpisodes.size.coerceAtLeast(2),
                accentHex = 0xFF2A9D8F,
                isFavorite = favoriteSeriesIds.contains(SERIES_CASPER_FRIENDS)
            ),
            CartoonSeries(
                id = SERIES_STONE_AGE_SCIFI,
                title = "Stone Age & Colonel Bleep",
                era = "1940–1957",
                studio = "Fleischer / Soundac",
                categoryKey = "SLAPSTICK",
                description = "Prehistoric dinosaur hotels and TV's very first color retro-futuristic space cartoon adventures.",
                thumbnailUrl = stoneAgeSciFiEpisodes.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/granite_hotel",
                episodeCount = stoneAgeSciFiEpisodes.size.coerceAtLeast(2),
                accentHex = 0xFFF59E0B,
                isFavorite = favoriteSeriesIds.contains(SERIES_STONE_AGE_SCIFI)
            ),
            CartoonSeries(
                id = SERIES_FELIX_CAT,
                title = "Felix the Cat",
                era = "1936",
                studio = "Van Beuren Studios",
                categoryKey = "SERIALS",
                description = "Vivid early Technicolor musical fairy tales starring animation's first international feline superstar.",
                thumbnailUrl = felixEpisodes.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/felix_the_cat_the_goos_that_laid_the_golden_egg",
                episodeCount = felixEpisodes.size.coerceAtLeast(2),
                accentHex = 0xFFFB7185,
                isFavorite = favoriteSeriesIds.contains(SERIES_FELIX_CAT)
            ),
            CartoonSeries(
                id = SERIES_CLASSIC_SLAPSTICK,
                title = "Keaton & Three Stooges",
                era = "1922–1936",
                studio = "Classic Comedy Serials",
                categoryKey = "SERIALS",
                description = "Legendary slapstick chase comedies and courtroom chaos from cinema's greatest physical comedians.",
                thumbnailUrl = slapstickEpisodes.firstOrNull()?.thumbnailUrl
                    ?: "https://archive.org/services/img/CopsbusterKeaton",
                episodeCount = slapstickEpisodes.size.coerceAtLeast(2),
                accentHex = 0xFF60A5FA,
                isFavorite = favoriteSeriesIds.contains(SERIES_CLASSIC_SLAPSTICK)
            )
        )
        return definitions
    }

    fun filterEpisodesForSeries(seriesId: String, allItems: List<CartoonItem>): List<CartoonItem> {
        val distinct = allItems.distinctBy { it.id }
        return when (seriesId) {
            SERIES_SUPERMAN -> distinct.filter {
                it.title.contains("superman", ignoreCase = true) ||
                    it.category.equals("Superman", ignoreCase = true) ||
                    it.id.contains("superman", ignoreCase = true)
            }
            SERIES_POPEYE -> distinct.filter {
                it.title.contains("popeye", ignoreCase = true) ||
                    it.category.equals("Popeye", ignoreCase = true) ||
                    it.id.contains("popeye", ignoreCase = true)
            }
            SERIES_BETTY_BOOP -> distinct.filter {
                it.title.contains("betty", ignoreCase = true) ||
                    it.category.equals("Betty Boop", ignoreCase = true) ||
                    it.id.startsWith("bb_", ignoreCase = true)
            }
            SERIES_LOONEY_PD -> distinct.filter {
                val combined = "${it.title} ${it.creator} ${it.id}".lowercase()
                combined.contains("kitties") || combined.contains("hare") ||
                    combined.contains("wabbit") || combined.contains("daffy") ||
                    combined.contains("porky") || combined.contains("looney") ||
                    combined.contains("merrie") || combined.contains("woody")
            }
            SERIES_CASPER_FRIENDS -> distinct.filter {
                val combined = "${it.title} ${it.id}".lowercase()
                combined.contains("casper") || combined.contains("herman") || combined.contains("katnip")
            }
            SERIES_STONE_AGE_SCIFI -> distinct.filter {
                val combined = "${it.title} ${it.id}".lowercase()
                combined.contains("stone age") || combined.contains("granite") ||
                    combined.contains("colonel bleep") || combined.contains("bleep")
            }
            SERIES_FELIX_CAT -> distinct.filter {
                val combined = "${it.title} ${it.id}".lowercase()
                combined.contains("felix")
            }
            SERIES_CLASSIC_SLAPSTICK -> distinct.filter {
                val combined = "${it.title} ${it.creator} ${it.id}".lowercase()
                combined.contains("keaton") || combined.contains("stooges") || combined.contains("disorder")
            }
            else -> emptyList()
        }
    }

    /**
     * Resolves which canonical CartoonSeries a given CartoonItem belongs to (for one-tap "Follow Series" in Detail Dialog).
     */
    fun resolveSeriesForItem(item: CartoonItem, allSeries: List<CartoonSeries>): CartoonSeries? {
        val text = "${item.id} ${item.title} ${item.category} ${item.creator}".lowercase()
        val targetId = when {
            text.contains("superman") -> SERIES_SUPERMAN
            text.contains("popeye") -> SERIES_POPEYE
            text.contains("betty") || item.id.startsWith("bb_", ignoreCase = true) -> SERIES_BETTY_BOOP
            text.contains("casper") || text.contains("herman") || text.contains("katnip") -> SERIES_CASPER_FRIENDS
            text.contains("stone age") || text.contains("granite") || text.contains("bleep") -> SERIES_STONE_AGE_SCIFI
            text.contains("felix") -> SERIES_FELIX_CAT
            text.contains("keaton") || text.contains("stooges") -> SERIES_CLASSIC_SLAPSTICK
            text.contains("looney") || text.contains("merrie") || text.contains("daffy") ||
                text.contains("porky") || text.contains("wabbit") || text.contains("hare") ||
                text.contains("kitties") || text.contains("woody") -> SERIES_LOONEY_PD
            else -> null
        } ?: return null

        return allSeries.firstOrNull { it.id == targetId }
    }
}
