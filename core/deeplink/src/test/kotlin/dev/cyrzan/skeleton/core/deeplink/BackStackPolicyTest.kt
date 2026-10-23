package dev.cyrzan.skeleton.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackStackPolicyTest {

    private data class Screen(val name: String) : Destination

    private val home = Screen("home")
    private val profile = Screen("profile")
    private val settings = Screen("settings")
    private val about = Screen("about")

    @Test
    fun `parent policy walks up to the root`() {
        val parents = mapOf(settings to profile, about to settings)
        val policy = ParentBackStackPolicy { parents[it] }

        assertEquals(listOf(profile, settings, about), policy.backStackFor(about))
        assertEquals(listOf(profile), policy.backStackFor(profile))
        assertEquals(listOf(home), policy.backStackFor(home))
    }

    @Test
    fun `parent policy fails fast on a cycle`() {
        val policy = ParentBackStackPolicy { if (it == settings) about else settings }
        assertThrows(IllegalStateException::class.java) { policy.backStackFor(settings) }
    }

    @Test
    fun `rooted policy puts the root underneath`() {
        val policy = RootedBackStackPolicy(home)
        assertEquals(listOf(home, about), policy.backStackFor(about))
        assertEquals(listOf(home), policy.backStackFor(home))
    }
}
