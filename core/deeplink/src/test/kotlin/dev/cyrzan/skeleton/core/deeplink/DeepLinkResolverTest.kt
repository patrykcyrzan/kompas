package dev.cyrzan.skeleton.core.deeplink

import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource.External
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource.InApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkResolverTest {

    private data object Home : Destination
    private data class Item(val id: String) : Destination
    private data object NewItem : Destination
    private data class Search(val query: String?) : Destination
    private data class Order(val id: Long) : Destination
    private data class Secret(val id: String) : Destination, DebugOnly
    private data class Payment(val id: String) : Destination, InAppOnly
    private data class Profile(val id: String) : Destination, RequiresAuthentication
    private data object Login : Destination

    private val routes = listOf(
        deepLinkRoute("items/{id}") { p -> p.pathId("id")?.let(::Item) },
        deepLinkRoute("items/new") { NewItem },
        deepLinkRoute("search") { Search(it.query("q")) },
        deepLinkRoute("orders/{id}") { p -> p.pathLong("id")?.let(::Order) },
    )

    private fun resolver(
        routes: List<DeepLinkRoute> = this.routes,
        guards: List<DeepLinkGuard> = emptyList(),
        limits: DeepLinkLimits = DeepLinkLimits(),
    ) = DeepLinkResolver(
        origins = listOf(DeepLinkOrigin.Web("app.example.com"), DeepLinkOrigin.CustomScheme("myapp")),
        routes = routes,
        backStackPolicy = RootedBackStackPolicy(Home),
        guards = guards,
        limits = limits,
    )

    // --- matching ---

    @Test
    fun `web and custom scheme resolve to the same result`() {
        val expected = DeepLinkResult.Navigate(Item("42"), listOf(Home, Item("42")))
        assertEquals(expected, resolver().resolve("https://app.example.com/items/42", External))
        assertEquals(expected, resolver().resolve("myapp://items/42", External))
    }

    @Test
    fun `literal segment wins over placeholder regardless of declaration order`() {
        assertEquals(NewItem, (resolver().resolve("myapp://items/new", External) as DeepLinkResult.Navigate).destination)
    }

    @Test
    fun `query parameters are available to the factory`() {
        assertEquals(Search("kot"), (resolver().resolve("myapp://search?q=kot&utm=x", External) as DeepLinkResult.Navigate).destination)
    }

    @Test
    fun `unknown origin and unknown path are not matched`() {
        listOf(
            "https://evil.example.com/items/42",
            "http://app.example.com/items/42",
            "myapp://items/42/extra",
            "myapp://nope",
        ).forEach { uri -> assertEquals(uri, DeepLinkResult.NotMatched, resolver().resolve(uri, External)) }
    }

    // --- input validation ---

    @Test
    fun `malformed and suspicious input is invalid before matching`() {
        listOf(
            "myapp://items/%zz",                       // broken escape
            "myapp://items/..",                         // traversal
            "myapp://items/..%2F..%2Fadmin",            // encoded slash inside a segment
            "myapp://items/a%5Cb",                      // encoded backslash
            "myapp://items/a%00b",                      // NUL
            "myapp://search?q=a%0Ab",                   // newline in query value
            "myapp://search?q=%E2%80%AEevil",           // bidi override
            "myapp://items/4 2",                        // raw whitespace
            "myapp://items/" + "a".repeat(300),         // segment too long
            "myapp://" + List(20) { "a" }.joinToString("/"),   // too many segments
            "myapp://search?" + List(40) { "p$it=1" }.joinToString("&"), // too many params
        ).forEach { uri ->
            assertTrue("Expected Invalid for $uri", resolver().resolve(uri, External) is DeepLinkResult.Invalid)
        }
    }

    @Test
    fun `uri length limit is enforced`() {
        val limits = DeepLinkLimits(maxUriLength = 20)
        assertEquals(DeepLinkResult.Invalid("URI too long"), resolver(limits = limits).resolve("myapp://search?q=0123456789", External))
    }

    @Test
    fun `typed params reject unexpected formats`() {
        listOf("myapp://items/a.b", "myapp://items/" + "x".repeat(65), "myapp://orders/0", "myapp://orders/-1", "myapp://orders/+1", "myapp://orders/007")
            .forEach { uri -> assertEquals(uri, DeepLinkResult.NotMatched, resolver().resolve(uri, External)) }
        assertEquals(Order(7), (resolver().resolve("myapp://orders/7", External) as DeepLinkResult.Navigate).destination)
    }

    // --- guards ---

    @Test
    fun `guard can reject a destination`() {
        val guard = DeepLinkGuard { if (it.destination == NewItem) DeepLinkGuard.Decision.Reject("internal") else DeepLinkGuard.Decision.Allow }
        assertEquals(DeepLinkResult.Rejected(NewItem, "internal"), resolver(guards = listOf(guard)).resolve("myapp://items/new", External))
    }

    @Test
    fun `guard sees source and origin`() {
        var seen: DeepLinkRequest? = null
        val guard = DeepLinkGuard { seen = it; DeepLinkGuard.Decision.Allow }
        resolver(guards = listOf(guard)).resolve("https://app.example.com/items/1", InApp)
        assertEquals(DeepLinkRequest(Item("1"), InApp, DeepLinkOrigin.Web("app.example.com")), seen)
    }

    @Test
    fun `guards run by priority and first non-allow decision wins`() {
        val calls = mutableListOf<String>()
        fun guard(name: String, priority: Int, decision: DeepLinkGuard.Decision) = object : DeepLinkGuard {
            override val priority = priority
            override fun check(request: DeepLinkRequest): DeepLinkGuard.Decision { calls += name; return decision }
        }
        val result = resolver(
            guards = listOf(
                guard("late", GuardPriority.DEFAULT, DeepLinkGuard.Decision.Reject("late")),
                guard("auth", GuardPriority.AUTHENTICATION, DeepLinkGuard.Decision.Redirect(NewItem)),
                guard("kill", GuardPriority.KILL_SWITCH, DeepLinkGuard.Decision.Allow),
            ),
        ).resolve("myapp://items/1", External)

        assertEquals(listOf("kill", "auth"), calls)
        assertEquals(DeepLinkResult.Redirect(NewItem, listOf(Home, NewItem), resumable = true), result)
    }

    @Test
    fun `built-in guards enforce destination markers`() {
        var loggedIn = false
        fun resolver(debug: Boolean) = resolver(
            routes = listOf(
                deepLinkRoute("secret/{id}") { Secret(it.path("id")) },
                deepLinkRoute("pay/{id}") { Payment(it.path("id")) },
                deepLinkRoute("profile/{id}") { Profile(it.path("id")) },
            ),
            guards = listOf(DebugOnlyGuard(debug), InAppOnlyGuard(), AuthenticationGuard({ loggedIn }, Login)),
        )

        assertTrue(resolver(debug = false).resolve("myapp://secret/1", External) is DeepLinkResult.Rejected)
        assertTrue(resolver(debug = true).resolve("myapp://secret/1", External) is DeepLinkResult.Navigate)

        assertTrue(resolver(debug = false).resolve("myapp://pay/1", External) is DeepLinkResult.Rejected)
        assertTrue(resolver(debug = false).resolve("myapp://pay/1", InApp) is DeepLinkResult.Navigate)

        assertEquals(
            DeepLinkResult.Redirect(Login, listOf(Home, Login), resumable = true),
            resolver(debug = false).resolve("myapp://profile/1", External),
        )
        loggedIn = true
        assertTrue(resolver(debug = false).resolve("myapp://profile/1", External) is DeepLinkResult.Navigate)
    }

    // --- configuration errors ---

    @Test
    fun `root destination is not duplicated`() {
        val resolver = resolver(routes = listOf(deepLinkRoute("home") { Home }))
        assertEquals(DeepLinkResult.Navigate(Home, listOf(Home)), resolver.resolve("myapp://home", External))
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
