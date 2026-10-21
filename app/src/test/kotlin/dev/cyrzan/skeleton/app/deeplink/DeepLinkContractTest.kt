package dev.cyrzan.skeleton.app.deeplink

import dev.cyrzan.skeleton.core.deeplink.AuthenticationGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.feature.details.api.DetailsDeepLinks
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.settings.api.SettingsDeepLinks
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Public URLs are a contract (emails, push, web) – changing what they open must be deliberate. */
class DeepLinkContractTest {

    private var loggedIn = true

    // Legacy routes and feature routes in one resolver – exactly like the Hilt Set in the app.
    private val resolver = createDeepLinkResolver(
        routes = LegacyDeepLinks.all + DetailsDeepLinks.item + SettingsDeepLinks.settings,
        guards = listOf(AuthenticationGuard(isAuthenticated = { loggedIn }, loginDestination = LegacyDestination.Login)),
    )

    private val contract = mapOf<String, List<Destination>>(
        "https://kompas.example.com/home" to listOf(LegacyDestination.Home),
        "kompas://orders/42" to listOf(LegacyDestination.Home, LegacyDestination.OrderDetails(42)),
        "kompas://profile" to listOf(LegacyDestination.Home, LegacyDestination.Profile),
        "https://kompas.example.com/items/42" to listOf(LegacyDestination.Home, DetailsNavKey("42")),
        "kompas://items/42?utm_source=mail" to listOf(LegacyDestination.Home, DetailsNavKey("42")),
        "kompas://settings" to listOf(LegacyDestination.Home, SettingsNavKey),
    )

    @Test
    fun `public urls open the expected back stack`() {
        contract.forEach { (uri, backStack) ->
            assertEquals(uri, DeepLinkResult.Navigate(backStack.last(), backStack), resolver.resolve(uri, DeepLinkSource.External))
        }
    }

    @Test
    fun `guarded legacy screen redirects to legacy login when logged out`() {
        loggedIn = false
        assertEquals(
            DeepLinkResult.Redirect(LegacyDestination.Login, listOf(LegacyDestination.Home, LegacyDestination.Login), resumable = true),
            resolver.resolve("kompas://profile", DeepLinkSource.External),
        )
    }

    @Test
    fun `ids are strictly validated`() {
        listOf(
            "kompas://items/..", "kompas://items/1%2F..%2Fadmin", "kompas://items/a.b", "kompas://items/%27%20OR%201%3D1",
            "kompas://orders/abc", "kompas://orders/-1", "kompas://orders/007", "kompas://orders/99999999999999999999",
        ).forEach { uri ->
            val result = resolver.resolve(uri, DeepLinkSource.External)
            assertTrue("$uri -> $result", result is DeepLinkResult.Invalid || result == DeepLinkResult.NotMatched)
        }
    }
}
