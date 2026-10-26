package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * The whole navigation state of the app, in two levels:
 *
 * ```
 * flow:  [Login, ...]                  full-screen screens ABOVE the tabs (bottom bar hidden)
 * tabs:  Home    = [Home, Details(7)]
 *        Orders  = [Orders]            selected
 *        Profile = [Profile, About]
 * ```
 *
 * - [tabs] hold regular "drill down" navigation; the bottom bar stays visible.
 * - The flow holds screens that take over the whole app (login, onboarding, checkout...). While a
 *   flow is open everything is pushed onto it – the user finishes or leaves the flow first.
 *
 * Which keys open a flow is the app's decision ([isFullScreen]); features only call [navigate].
 * Pure state, no UI – rendered by [AppNavDisplay], unit-testable without Android.
 */
@Stable
class NavigationState(
    val tabs: TabbedBackStack,
    private val flow: MutableList<NavKey>,
    private val isFullScreen: (NavKey) -> Boolean,
    val results: NavResultStore = NavResultStore(),
) {
    /** Full-screen stack above the tabs; empty when no flow is open. */
    val flowBackStack: List<NavKey> get() = flow

    val isFlowOpen: Boolean get() = flow.isNotEmpty()

    /** The screen the user sees. */
    val current: NavKey? get() = flow.lastOrNull() ?: tabs.current

    /**
     * - tab root: closes any flow and opens that tab at its root,
     * - full-screen key, or any key while a flow is open: pushed onto the flow,
     * - anything else: pushed on the selected tab.
     */
    fun navigate(key: NavKey) {
        when {
            tabs.isTab(key) -> {
                flow.clear()
                tabs.navigate(key)
            }
            isFlowOpen || isFullScreen(key) -> flow.add(key)
            else -> tabs.navigate(key)
        }
    }

    /** Bottom bar click (the bar is hidden while a flow is open). */
    fun selectTab(tab: NavKey) = tabs.selectTab(tab)

    /** Pops the flow first, then the tabs. `false` = nothing left (close the app). */
    fun goBack(): Boolean {
        if (!isFlowOpen) return tabs.goBack()
        flow.removeAt(flow.lastIndex)
        return true
    }

    /** [goBack] and hand [value] to the screen that is on top afterwards (see [ResultEffect]). */
    fun <T : Any> goBackWithResult(key: ResultKey<T>, value: T): Boolean {
        if (!goBack()) return false
        results.retainOnly(entryIds())
        results.put(currentEntryId, key, value)
        return true
    }

    /** Identity of the screen the user sees – the same one [AppNavDisplay] gives its entry. */
    internal val currentEntryId: String
        get() = if (isFlowOpen) {
            entryId(FLOW_SCOPE, flow.lastIndex, flow.last())
        } else {
            val tabIndex = tabs.tabs.indexOf(tabs.currentTab)
            tabs.stackOf(tabs.currentTab).backStack.let { stack -> entryId(tabScope(tabIndex), stack.lastIndex, stack.last()) }
        }

    /** Every entry on every stack. */
    internal fun entryIds(): Set<String> = buildSet {
        tabs.tabs.forEachIndexed { tabIndex, tab ->
            tabs.stackOf(tab).backStack.forEachIndexed { index, key -> add(entryId(tabScope(tabIndex), index, key)) }
        }
        flow.forEachIndexed { index, key -> add(entryId(FLOW_SCOPE, index, key)) }
    }

    /** Leaves the whole flow at once (e.g. a multi-step sign-in that finished). */
    fun closeFlow() = flow.clear()

    /**
     * Replaces the state with a synthetic back stack (deep link from outside the app):
     * `[tab root, ...in-tab keys, ...full-screen keys]`. The tab part selects and replaces that tab
     * (other tabs keep their history), the rest becomes the flow.
     * A back stack made of full-screen keys only opens over whatever tab is selected.
     */
    fun resetTo(keys: List<NavKey>) {
        require(keys.isNotEmpty()) { "Back stack cannot be empty" }
        val (inTabs, inFlow) = splitBackStack(keys, isFullScreen)
        if (inTabs.isNotEmpty()) tabs.resetTo(inTabs)
        flow.replaceWith(inFlow)
    }

    /** Every tab back to its root and no flow – e.g. after sign-out nothing of the previous user remains. */
    fun resetAll() {
        flow.clear()
        tabs.resetAll()
        results.clear()
    }
}

/** `[A, B, Login, C]` → `[A, B]` (tabs) and `[Login, C]` (flow): the flow starts at the first full-screen key. */
internal fun splitBackStack(keys: List<NavKey>, isFullScreen: (NavKey) -> Boolean): Pair<List<NavKey>, List<NavKey>> {
    val flowStart = keys.indexOfFirst(isFullScreen).takeIf { it >= 0 } ?: keys.size
    return keys.take(flowStart) to keys.drop(flowStart)
}

/**
 * [startBackStack] (used only on first creation, e.g. from a cold-start deep link) is split like in
 * [NavigationState.resetTo]. Everything is saved across configuration changes and process death.
 */
@Composable
fun rememberNavigationState(
    tabs: List<NavKey>,
    startBackStack: List<NavKey>,
    isFullScreen: (NavKey) -> Boolean,
): NavigationState {
    val (inTabs, inFlow) = splitBackStack(startBackStack, isFullScreen)
    val tabbed = rememberTabbedBackStack(tabs, inTabs.ifEmpty { listOf(tabs.first()) })
    val flow = rememberNavBackStack(*inFlow.toTypedArray())
    val currentIsFullScreen by rememberUpdatedState(isFullScreen)
    // Retained across configuration changes, like the screens' ViewModels.
    val results = viewModel<NavResultStoreHolder>().store
    return remember(tabbed, flow, results) { NavigationState(tabbed, flow, { currentIsFullScreen(it) }, results) }
}

internal class NavResultStoreHolder : ViewModel() {
    val store = NavResultStore()
}
