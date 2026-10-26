package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateTest {

    private data class Key(val name: String) : NavKey

    private val home = Key("home")
    private val orders = Key("orders")
    private val details = Key("details")
    private val login = Key("login")
    private val terms = Key("terms")

    private val flow = NavBackStack<NavKey>()
    private val state = NavigationState(
        tabs = TabbedBackStack(
            tabs = listOf(home, orders),
            stacks = listOf(home, orders).associateWith { BackStackNavigator(NavBackStack(it)) },
            selectedIndex = mutableIntStateOf(0),
        ),
        flow = flow,
        isFullScreen = { it == login },
    )

    private fun stack(tab: NavKey) = state.tabs.stackOf(tab).backStack.toList()

    @Test
    fun `full-screen key opens a flow above the tabs and captures further pushes`() {
        state.navigate(details)
        state.navigate(login)
        state.navigate(terms)

        assertEquals(listOf(login, terms), state.flowBackStack)
        assertEquals(listOf(home, details), stack(home))
        assertEquals(terms, state.current)
    }

    @Test
    fun `back pops the flow first, then the tabs`() {
        state.navigate(details)
        state.navigate(login)

        assertTrue(state.goBack())
        assertFalse(state.isFlowOpen)
        assertEquals(details, state.current)
        assertTrue(state.goBack())
        assertFalse(state.goBack())
    }

    @Test
    fun `tab root closes the flow`() {
        state.navigate(login)
        state.navigate(orders)

        assertFalse(state.isFlowOpen)
        assertEquals(orders, state.tabs.currentTab)
    }

    @Test
    fun `synthetic back stack is split into a tab part and a flow part`() {
        state.resetTo(listOf(orders, details, login))

        assertEquals(orders, state.tabs.currentTab)
        assertEquals(listOf(orders, details), stack(orders))
        assertEquals(listOf(login), state.flowBackStack)
    }

    @Test
    fun `flow-only back stack opens over the selected tab without touching it`() {
        state.navigate(details)
        state.resetTo(listOf(login))

        assertEquals(listOf(home, details), stack(home))
        assertEquals(listOf(login), state.flowBackStack)
    }

    @Test
    fun `reset all clears every tab and the flow`() {
        state.navigate(details)
        state.selectTab(orders)
        state.navigate(details)
        state.navigate(login)

        state.resetAll()

        assertEquals(home, state.tabs.currentTab)
        assertEquals(listOf(home), stack(home))
        assertEquals(listOf(orders), stack(orders))
        assertTrue(state.flowBackStack.isEmpty())
    }
}
