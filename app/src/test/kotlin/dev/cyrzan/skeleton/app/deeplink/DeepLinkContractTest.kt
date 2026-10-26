package dev.cyrzan.skeleton.app.deeplink

import dev.cyrzan.skeleton.app.navigation.TopLevelTab
import dev.cyrzan.skeleton.app.navigation.isFullScreen
import dev.cyrzan.skeleton.core.deeplink.AuthenticationGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.model.ItemCategory
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.core.model.OrderId
import dev.cyrzan.skeleton.feature.details.api.DetailsDeepLinks
import dev.cyrzan.skeleton.feature.auth.api.LoginNavKey
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.home.api.HomeDeepLinks
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrderDetailsNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrdersDeepLinks
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.feature.profile.api.AboutNavKey
import dev.cyrzan.skeleton.feature.profile.api.ProfileDeepLinks
import dev.cyrzan.skeleton.feature.profile.api.ProfileNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchDeepLinks
import dev.cyrzan.skeleton.feature.search.api.SearchNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchResultsNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchSort
import dev.cyrzan.skeleton.feature.settings.api.SettingsDeepLinks
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Public URLs are a contract (emails, push, web) – changing what they open must be deliberate. */
class DeepLinkContractTest {

    private val routes = listOf(
        HomeDeepLinks.home,
        DetailsDeepLinks.item,
        SettingsDeepLinks.settings,
        OrdersDeepLinks.orders,
        OrdersDeepLinks.orderDetails,
        ProfileDeepLinks.profile,
        ProfileDeepLinks.about,
        SearchDeepLinks.results,
    )

    private val resolver = createDeepLinkResolver(routes = routes)

    /** The first element is the tab the link opens on. */
    private val contract = mapOf<String, List<Destination>>(
        "https://kompas.example.com/home" to listOf(HomeNavKey),
        "https://kompas.example.com/items/42" to listOf(HomeNavKey, DetailsNavKey(ItemId("42"))),
        "kompas://items/42?utm_source=mail" to listOf(HomeNavKey, DetailsNavKey(ItemId("42"))),
        "kompas://orders" to listOf(OrdersNavKey),
        "kompas://orders/1001" to listOf(OrdersNavKey, OrderDetailsNavKey(OrderId(1001))),
        "kompas://profile" to listOf(ProfileNavKey),
        "kompas://settings" to listOf(ProfileNavKey, SettingsNavKey),
        "kompas://about" to listOf(ProfileNavKey, AboutNavKey),
        "kompas://search?q=item&category=books&maxPrice=100&sort=price_asc" to SearchCriteria(
            query = "item", category = ItemCategory.Books, maxPrice = 100, sort = SearchSort.PriceAscending,
        ).let { criteria -> listOf(HomeNavKey, SearchNavKey(criteria), SearchResultsNavKey(criteria)) },
        "kompas://search" to listOf(HomeNavKey, SearchNavKey(SearchCriteria()), SearchResultsNavKey(SearchCriteria())),
    )

    @Test
    fun `public urls open the expected tab and back stack`() {
        contract.forEach { (uri, backStack) ->
            assertEquals(uri, DeepLinkResult.Navigate(backStack.last(), backStack), resolver.resolve(uri, DeepLinkSource.External))
        }
    }

    @Test
    fun `every hierarchy ends at a tab root`() {
        contract.values.flatten().forEach { destination ->
            val root = generateSequence(destination, ::parentOf).last()
            assertTrue("$destination -> $root", TopLevelTab.roots.any { it == root })
        }
    }

    @Test
    fun `order details need a session - sign-in opens above the tabs and resumes the link`() {
        val guarded = createDeepLinkResolver(routes, guards = listOf(AuthenticationGuard({ false }, LoginNavKey)))

        assertEquals(
            DeepLinkResult.Redirect(LoginNavKey, listOf(LoginNavKey), resumable = true),
            guarded.resolve("kompas://orders/1001", DeepLinkSource.External),
        )
        assertTrue(isFullScreen(LoginNavKey))
    }

    @Test
    fun `ids are strictly validated`() {
        listOf(
            "kompas://items/..", "kompas://items/1%2F..%2Fadmin", "kompas://items/a.b", "kompas://items/%27%20OR%201%3D1",
            "kompas://orders/abc", "kompas://orders/-1", "kompas://orders/007",
            "kompas://search?category=cars", "kompas://search?maxPrice=-5", "kompas://search?sort=random",
            "kompas://search?q=" + "x".repeat(101),
        ).forEach { uri ->
            val result = resolver.resolve(uri, DeepLinkSource.External)
            assertTrue("$uri -> $result", result is DeepLinkResult.Invalid || result == DeepLinkResult.NotMatched)
        }
    }
}
