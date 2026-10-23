package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavKey

/**
 * Bottom tabs: one [BackStackNavigator] per tab plus the selected tab.
 *
 * Each tab is identified by its root key (e.g. `HomeNavKey`), which is always the first entry of
 * that tab's stack. The first tab in [tabs] is the start tab – "back" from the root of any other
 * tab returns to it (Material guidance), and from its root the app closes.
 */
@Stable
class TabbedBackStack(
    val tabs: List<NavKey>,
    private val stacks: Map<NavKey, BackStackNavigator>,
    private val selectedIndex: MutableIntState,
) {
    init {
        require(tabs.isNotEmpty()) { "At least one tab is required" }
        require(stacks.keys == tabs.toSet()) { "Every tab needs exactly one back stack" }
    }

    val startTab: NavKey get() = tabs.first()

    val currentTab: NavKey get() = tabs[selectedIndex.intValue]

    /** Top of the selected tab. */
    val current: NavKey? get() = stackOf(currentTab).current

    fun stackOf(tab: NavKey): BackStackNavigator =
        requireNotNull(stacks[tab]) { "$tab is not a tab" }

    fun isTab(key: NavKey): Boolean = key in stacks

    /**
     * Bottom bar click: switches tabs and keeps each tab's history.
     * Selecting the tab you are already on pops it to its root.
     */
    fun selectTab(tab: NavKey) {
        if (tab == currentTab) {
            stackOf(tab).resetTo(listOf(tab))
        } else {
            selectedIndex.intValue = tabs.indexOf(tab).also { require(it >= 0) { "$tab is not a tab" } }
        }
    }

    /**
     * A tab root opens that tab at its root (never a second copy of a root on another tab).
     * Any other key is pushed on top of the selected tab.
     */
    fun navigate(key: NavKey) {
        if (isTab(key)) {
            selectedIndex.intValue = tabs.indexOf(key)
            stackOf(key).resetTo(listOf(key))
        } else {
            stackOf(currentTab).navigate(key)
        }
    }

    /** Pops the selected tab; at its root falls back to the start tab. `false` = nothing left (close the app). */
    fun goBack(): Boolean = when {
        stackOf(currentTab).goBack() -> true
        currentTab != startTab -> {
            selectedIndex.intValue = 0
            true
        }
        else -> false
    }

    /**
     * Replaces the stack of the tab [keys] start with and selects that tab. Other tabs are untouched.
     * [keys] must start with a tab root – that's how a deep link says which tab it belongs to.
     */
    fun resetTo(keys: List<NavKey>) {
        val tab = keys.first()
        require(isTab(tab)) { "Back stack must start with a tab root, was $tab" }
        selectedIndex.intValue = tabs.indexOf(tab)
        stackOf(tab).resetTo(keys)
    }
}

/**
 * [startBackStack] (used only on first creation) selects its tab – it must start with a tab root.
 * The other tabs start at their roots. Everything is saved across config changes and process death.
 */
@Composable
fun rememberTabbedBackStack(tabs: List<NavKey>, startBackStack: List<NavKey>): TabbedBackStack {
    val startTab = startBackStack.first()
    require(startTab in tabs) { "Start back stack must start with a tab root, was $startTab" }

    val stacks = tabs.associateWith { tab ->
        key(tab) { rememberBackStackNavigator(if (tab == startTab) startBackStack else listOf(tab)) }
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(tabs.indexOf(startTab)) }
    return remember(tabs, selectedIndex, *stacks.values.toTypedArray()) {
        TabbedBackStack(tabs, stacks, selectedIndex)
    }
}
