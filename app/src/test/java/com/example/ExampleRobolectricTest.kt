package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ApplicationProvider
import com.example.data.engagement.AppSuggestion
import com.example.data.engagement.EngagementManager
import com.example.data.engagement.SuggestionType
import com.example.data.model.CartoonItem
import com.example.data.repository.CartoonRepository
import com.example.ui.UiState
import com.example.ui.detail.CartoonDetailDialog
import com.example.ui.engagement.SuggestionDialog
import com.example.ui.home.HomeScreen
import com.example.ui.splash.RetroSplashScreen
import com.example.ui.theme.RetroToonTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RetroToon TV", appName)
  }

  @Test
  fun `curated items load properly from repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)

    val featured = repository.getCuratedFeatured()
    assertTrue(featured.isNotEmpty())

    val superman = featured.first { it.id == "superman_the_mechanical_monsters" }
    assertEquals("Superman: The Mechanical Monsters", superman.title)
    assertTrue(superman.getResolvedStreamUrl().contains("archive.org"))
  }

  @Test
  fun `stream candidates resolution returns valid archive URLs`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)
    val featured = repository.getCuratedFeatured().first()

    val candidates = repository.resolveAllStreamUrls(featured)
    assertTrue(candidates.isNotEmpty())
    assertTrue(candidates.first().contains("archive.org"))
  }

  @Test
  fun `watchlist toggle persists correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)
    val featured = repository.getCuratedFeatured().first()

    assertFalse(repository.watchlist.value.contains(featured.id))
    repository.toggleWatchlist(featured)
    assertTrue(repository.watchlist.value.contains(featured.id))
    repository.toggleWatchlist(featured)
    assertFalse(repository.watchlist.value.contains(featured.id))
  }

  @Test
  fun `engagement manager does not prompt during playback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engagement = EngagementManager(context)

    engagement.recordShortCompleted()
    engagement.recordShortCompleted()
    engagement.recordShortCompleted()
    val promptWhilePlaying = engagement.evaluatePendingSuggestion(isCurrentlyPlaying = true)
    assertNull(promptWhilePlaying)

    val promptWhenIdle = engagement.evaluatePendingSuggestion(isCurrentlyPlaying = false)
    assertNotNull(promptWhenIdle)
    assertEquals(SuggestionType.RATE_APP, promptWhenIdle?.type)
  }

  @Test
  fun `engagement manager dismisses forever when requested`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engagement = EngagementManager(context)

    engagement.dismissForever(SuggestionType.RATE_APP)
    val prompt = engagement.evaluatePendingSuggestion(isCurrentlyPlaying = false)
    assertNull(prompt)
  }

  @Test
  fun `verify splash screen composes successfully`() {
    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    val activity = controller.get()
    var finished = false
    activity.setContent {
      RetroToonTheme {
        RetroSplashScreen(onFinished = { finished = true })
      }
    }
    assertNotNull(activity.window.decorView)
  }

  @Test
  fun `verify home screen and detail dialog compose successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)
    val curated = repository.getCuratedFeatured()

    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    val activity = controller.get()
    activity.setContent {
      RetroToonTheme {
        HomeScreen(
          uiState = UiState(
            isLoading = false,
            heroItem = curated.firstOrNull(),
            featuredList = curated,
            supermanList = curated.filter { it.category == "Superman" },
            popeyeList = curated.filter { it.category == "Popeye" },
            bettyList = curated.filter { it.category == "Betty Boop" },
            serialsList = curated.filter { it.category == "Classics" }
          ),
          onTabSelected = {},
          onCategoryFilterSelected = {},
          onPlay = {},
          onSelectDetail = {},
          onToggleWatchlist = {},
          onSetHero = {},
          onSearchQueryChanged = {}
        )

        curated.firstOrNull()?.let { cartoon ->
          CartoonDetailDialog(
            cartoon = cartoon,
            onDismiss = {},
            onPlay = {},
            onToggleWatchlist = {}
          )
        }
      }
    }
    assertNotNull(activity.window.decorView)
  }

  @Test
  fun `verify suggestion dialog composes successfully`() {
    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    val activity = controller.get()
    activity.setContent {
      RetroToonTheme {
        SuggestionDialog(
          suggestion = AppSuggestion(
            type = SuggestionType.RATE_APP,
            title = "Enjoying Vintage Cartoons?",
            subtitle = "Help us keep public domain animation free and accessible by rating RetroToon TV on Google Play!",
            primaryButtonText = "Rate 5 Stars ★",
            secondaryButtonText = "Remind Later"
          ),
          onPrimaryAction = {},
          onSecondaryAction = {},
          onNeverShowAgain = {},
          onDismiss = {}
        )
      }
    }
    assertNotNull(activity.window.decorView)
  }

  @Test
  fun `verify category filter renders all categories cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)
    val curated = repository.getCuratedFeatured()

    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    val activity = controller.get()

    listOf("ALL", "CARTOONS", "SERIALS", "VINTAGE", "SUPERMAN", "POPEYE", "BETTY").forEach { category ->
      var selectedCategory = category
      activity.setContent {
        RetroToonTheme {
          HomeScreen(
            uiState = UiState(
              categoryFilter = selectedCategory,
              heroItem = curated.firstOrNull(),
              featuredList = curated,
              supermanList = curated.filter { it.category == "Superman" },
              popeyeList = curated.filter { it.category == "Popeye" },
              bettyList = curated.filter { it.category == "Betty Boop" },
              serialsList = curated.filter { it.category == "Classics" }
            ),
            onTabSelected = {},
            onCategoryFilterSelected = { selectedCategory = it },
            onPlay = {},
            onSelectDetail = {},
            onToggleWatchlist = {},
            onSetHero = {},
            onSearchQueryChanged = {}
          )
        }
      }
      assertNotNull(activity.window.decorView)
    }
  }

  @Test
  fun `verify category strings are present in resources`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("All Classics", context.getString(R.string.category_all))
    assertEquals("Cartoons", context.getString(R.string.category_cartoons))
    assertEquals("Serials", context.getString(R.string.category_serials))
    assertEquals("Vintage", context.getString(R.string.category_vintage))
    assertEquals("Superman (1941)", context.getString(R.string.category_superman))
    assertEquals("Popeye the Sailor", context.getString(R.string.category_popeye))
    assertEquals("Betty Boop", context.getString(R.string.category_betty))
    assertEquals("Show All", context.getString(R.string.category_clear_filter))
    assertEquals("Browsing Collection", context.getString(R.string.category_filter_label))
  }

  @Test
  fun `verify Room record recently watched and clear recently watched`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)
    val item = repository.getCuratedFeatured().first()

    repository.recordRecentlyWatched(item, progressMs = 30000L, durationMs = 600000L)
    val recents = repository.recentlyWatched.first()
    assertTrue(recents.any { it.id == item.id })

    repository.clearRecentlyWatched()
    val cleared = repository.recentlyWatched.first()
    assertTrue(cleared.none { it.id == item.id })
  }

  @Test
  fun `verify watch progress and duration enrichment and completion reset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CartoonRepository(context)
    val item = repository.getCuratedFeatured().first()

    repository.saveWatchProgress(item.id, positionMs = 45000L, durationMs = 600000L)
    val enriched = repository.enrichWithUserData(listOf(item)).first()
    assertEquals(45000L, enriched.watchProgressMs)
    assertEquals(600000L, enriched.durationMs)

    // When position reaches near the end of duration, progress resets to 0L
    repository.saveWatchProgress(item.id, positionMs = 599000L, durationMs = 600000L)
    val completed = repository.enrichWithUserData(listOf(item)).first()
    assertEquals(0L, completed.watchProgressMs)
    assertEquals(600000L, completed.durationMs)
  }
}



