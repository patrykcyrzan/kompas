package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabbedBackStackTest {

    private data class Key(val name: String) : NavKey

    private val home = Key("home")
    private val orders = Key("orders")
    private val profile = Key("profile")
    private val details = Key("details")
    private val settings = Key("settings")

    private val tabs = TabbedBackStack(
        tabs = listOf(home, orders, profile),
        stacks = listOf(home, orders, profile).associateWith { BackStackNavigator(NavBackStack(it)) },
        selectedIndex = mutableIntStateOf(0),
    )

    private fun stack(tab: NavKey) = tabs.stackOf(tab).backStack.toList()

    @Test
    fun `push goes to the selected tab only`() {
        tabs.selectTab(profile)
        tabs.navigate(settings)

        assertEquals(listOf(profile, settings), stack(profile))
        assertEquals(listOf(home), stack(home))
    }

    @Test
    fun `switching tabs keeps their history and reselecting pops to root`() {
        tabs.navigate(details)
        tabs.selectTab(orders)
        tabs.selectTab(home)
        assertEquals(listOf(home, details), stack(home))

        tabs.selectTab(home)
        assertEquals(listOf(home), stack(home))
    }

    @Test
    fun `navigating to a tab root switches tabs instead of stacking a copy`() {
        tabs.selectTab(profile)
        tabs.navigate(settings)
        tabs.navigate(home)

        assertEquals(home, tabs.currentTab)
        assertEquals(listOf(home), stack(home))
        assertEquals(listOf(profile, settings), stack(profile))
    }

    @Test
    fun `back pops the tab then returns to the start tab then gives up`() {
        tabs.selectTab(profile)
        tabs.navigate(settings)

        assertTrue(tabs.goBack())
        assertEquals(listOf(profile), stack(profile))
        assertTrue(tabs.goBack())
        assertEquals(home, tabs.currentTab)
        assertFalse(tabs.goBack())
    }

    @Test
    fun `reset selects the tab of the first key and leaves other tabs alone`() {
        tabs.navigate(details)
        tabs.resetTo(listOf(profile, settings))

        assertEquals(profile, tabs.currentTab)
        assertEquals(listOf(profile, settings), stack(profile))
        assertEquals(listOf(home, details), stack(home))
    }
}
