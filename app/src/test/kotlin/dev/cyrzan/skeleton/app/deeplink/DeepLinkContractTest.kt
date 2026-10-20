package dev.cyrzan.skeleton.app.deeplink

import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.feature.details.api.DetailsDeepLinks
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.home.api.HomeDeepLinks
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import dev.cyrzan.skeleton.feature.settings.api.SettingsDeepLinks
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Public URLs are a contract (emails, push, web) – changing what they open must be deliberate. */
class DeepLinkContractTest {

    private val resolver = createDeepLinkResolver(
        routes = listOf(HomeDeepLinks.home, DetailsDeepLinks.item, SettingsDeepLinks.settings),
    )

    private val contract = mapOf<String, List<Destination>>(
        "https://kompas.example.com/home" to listOf(HomeNavKey),
        "https://kompas.example.com/items/42" to listOf(HomeNavKey, DetailsNavKey("42")),
        "kompas://items/42?utm_source=mail" to listOf(HomeNavKey, DetailsNavKey("42")),
        "kompas://settings" to listOf(HomeNavKey, SettingsNavKey),
    )

    @Test
    fun `public urls open the expected back stack`() {
        contract.forEach { (uri, backStack) ->
            assertEquals(uri, DeepLinkResult.Navigate(backStack.last(), backStack), resolver.resolve(uri, DeepLinkSource.External))
        }
    }

    @Test
    fun `item ids are strictly validated`() {
        listOf("kompas://items/..", "kompas://items/1%2F..%2Fadmin", "kompas://items/a.b", "kompas://items/%27%20OR%201%3D1")
            .forEach { uri ->
                val result = resolver.resolve(uri, DeepLinkSource.External)
                assertTrue("$uri -> $result", result is DeepLinkResult.Invalid || result == DeepLinkResult.NotMatched)
            }
    }
}
