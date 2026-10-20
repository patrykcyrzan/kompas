package dev.cyrzan.skeleton.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkResolverTest {

    private data object Home : Destination
    private data class Item(val id: String) : Destination
    private data object NewItem : Destination
    private data class Search(val query: String?) : Destination

    private val routes = listOf(
        deepLinkRoute("items/{id}") { Item(it.path("id")) },
        deepLinkRoute("items/new") { NewItem },
        deepLinkRoute("search") { Search(it.query("q")) },
    )

    private fun resolver(
        routes: List<DeepLinkRoute> = this.routes,
    ) = DeepLinkResolver(
        origins = listOf(DeepLinkOrigin.Web("app.example.com"), DeepLinkOrigin.CustomScheme("myapp")),
        routes = routes,
        backStackPolicy = RootedBackStackPolicy(Home),
    )

    // --- matching ---

    @Test
    fun `web and custom scheme resolve to the same result`() {
        val expected = DeepLinkResult.Navigate(Item("42"), listOf(Home, Item("42")))
        assertEquals(expected, resolver().resolve("https://app.example.com/items/42"))
        assertEquals(expected, resolver().resolve("myapp://items/42"))
    }

    @Test
    fun `literal segment wins over placeholder regardless of declaration order`() {
        assertEquals(NewItem, (resolver().resolve("myapp://items/new") as DeepLinkResult.Navigate).destination)
    }

    @Test
    fun `query parameters are available to the factory`() {
        assertEquals(Search("kot"), (resolver().resolve("myapp://search?q=kot&utm=x") as DeepLinkResult.Navigate).destination)
    }

    @Test
    fun `unknown origin and unknown path are not matched`() {
        listOf(
            "https://evil.example.com/items/42",
            "http://app.example.com/items/42",
            "myapp://items/42/extra",
            "myapp://nope",
        ).forEach { uri -> assertEquals(uri, DeepLinkResult.NotMatched, resolver().resolve(uri)) }
    }

    @Test
    fun `malformed uri is invalid`() {
        assertTrue(resolver().resolve("myapp://items/%zz") is DeepLinkResult.Invalid)
    }

    // --- configuration errors ---

    @Test
    fun `root destination is not duplicated`() {
        val resolver = resolver(routes = listOf(deepLinkRoute("home") { Home }))
        assertEquals(DeepLinkResult.Navigate(Home, listOf(Home)), resolver.resolve("myapp://home"))
    }

    @Test
    fun `ambiguous routes fail fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolver(routes = listOf(deepLinkRoute("items/{id}") { Item(it.path("id")) }, deepLinkRoute("items/{itemId}") { Home }))
        }
    }

    @Test
    fun `invalid patterns fail fast`() {
        assertThrows(IllegalArgumentException::class.java) { deepLinkRoute("items/item_{id}") { Home } }
        assertThrows(IllegalArgumentException::class.java) { deepLinkRoute("{a}/{a}") { Home } }
    }
}
