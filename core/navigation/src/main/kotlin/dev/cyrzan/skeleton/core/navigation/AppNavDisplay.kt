package dev.cyrzan.skeleton.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

/**
 * Renders [NavigationState] with Nav3: one [NavDisplay] over all stacks, [bottomBar] below it.
 *
 * - Every stack (each tab and the flow) is decorated all the time, also when hidden, so switching
 *   tabs keeps each screen's saved UI state and ViewModels.
 * - Every entry has its own identity (see [entryId]): the same key on two tabs, or twice on one
 *   stack (item #7 → item #8 → item #7), is two independent screens with their own state.
 * - The NavDisplay shows `start tab + selected tab + flow`, so predictive back previews exactly what
 *   [NavigationState.goBack] will show (from another tab's root: the start tab).
 * - The bottom bar is hidden while a flow is open.
 */
@Composable
fun AppNavDisplay(
    state: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { state.goBack() },
) {
    val tabs = state.tabs
    val entriesByTab = tabs.tabs.mapIndexed { index, tab ->
        key(tab) { rememberStackEntries(tabScope(index), tabs.stackOf(tab).backStack, entryProvider) }
    }.let { tabs.tabs.zip(it).toMap() }
    val flowEntries = rememberStackEntries(FLOW_SCOPE, state.flowBackStack, entryProvider)

    val entries = buildList {
        addAll(entriesByTab.getValue(tabs.startTab))
        if (tabs.currentTab != tabs.startTab) addAll(entriesByTab.getValue(tabs.currentTab))
        addAll(flowEntries)
    }
    val showBottomBar = !state.isFlowOpen

    CompositionLocalProvider(LocalNavResultStore provides state.results) {
        Column(modifier) {
            NavDisplay(
                entries = entries,
                // The bottom bar draws behind the system navigation bar – screens above it must not pad for it again.
                modifier = Modifier
                    .weight(1f)
                    .then(if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) else Modifier),
                onBack = onBack,
            )
            AnimatedVisibility(visible = showBottomBar, enter = expandVertically(), exit = shrinkVertically()) {
                bottomBar()
            }
        }
    }
}

@Composable
private fun rememberStackEntries(
    scope: String,
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    // Own decorators per stack: saved state and ViewModels are never shared between stacks.
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )
    val entries = remember(backStack.toList()) {
        backStack.mapIndexed { index, key ->
            val entry = entryProvider(key)
            val id = entryId(scope, index, key)
            NavEntry(key, contentKey = id, metadata = entry.metadata) {
                CompositionLocalProvider(LocalNavEntryId provides id) { entry.Content() }
            }
        }
    }
    return rememberDecoratedNavEntries(entries, decorators)
}
