package dev.cyrzan.skeleton.core.navigation

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
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.ui.NavDisplay

/**
 * Renders [NavigationState] with Nav3: one [NavDisplay] over all stacks. Only the mechanics –
 * the app shell around it (bottom bar, rail, insets) belongs to the app.
 *
 * - Every stack (each tab and the flow) is decorated all the time, also when hidden, so switching
 *   tabs keeps each screen's saved UI state and ViewModels.
 * - Every entry has its own identity (see [entryId]): the same key on two tabs, or twice on one
 *   stack (item #7 → item #8 → item #7), is two independent screens with their own state.
 * - The NavDisplay shows `start tab + selected tab + flow`, so predictive back previews exactly what
 *   [NavigationState.goBack] will show (from another tab's root: the start tab).
 * - Results between screens go through Nav3's [ResultEventBus] ([resultEventBus] – the one the
 *   navigator sends to with [Navigator.goBackWithResult]).
 */
@Composable
fun NavigationDisplay(
    state: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
    resultEventBus: ResultEventBus = rememberAppResultEventBus(),
    onBack: () -> Unit = { state.goBack() },
) {
    val tabs = state.tabs
    val tabStacks = tabs.tabs.mapIndexed { index, tab ->
        key(tab) { rememberStackEntries(tabScope(index), tabs.stackOf(tab).backStack, entryProvider) }
    }.let { tabs.tabs.zip(it).toMap() }
    val flowStack = rememberStackEntries(FLOW_SCOPE, state.flowBackStack, entryProvider)

    val entries = buildList {
        addAll(tabStacks.getValue(tabs.startTab))
        if (tabs.currentTab != tabs.startTab) addAll(tabStacks.getValue(tabs.currentTab))
        addAll(flowStack)
    }

    CompositionLocalProvider(LocalResultEventBus provides resultEventBus) {
        NavDisplay(entries = entries, modifier = modifier, onBack = onBack)
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
            NavEntry(key, contentKey = entryId(scope, index, key), metadata = entry.metadata) { entry.Content() }
        }
    }
    return rememberDecoratedNavEntries(entries, decorators)
}
