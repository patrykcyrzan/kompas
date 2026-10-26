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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

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
 * - Screens of one multi-screen flow can share ViewModels (see [sharedViewModelScope]).
 * - Results between screens go through Nav3's [ResultEventBus].
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
    val tabStacks = tabs.tabs.mapIndexed { index, tab ->
        key(tab) { rememberStackEntries(tabScope(index), tabs.stackOf(tab).backStack, entryProvider) }
    }.let { tabs.tabs.zip(it).toMap() }
    val flowStack = rememberStackEntries(FLOW_SCOPE, state.flowBackStack, entryProvider)

    // Shared ViewModel scopes of every stack – also hidden tabs, a form may wait there half-filled.
    val sharedScopes = rememberSharedScopes(liveScopeIds = tabStacks.values.flatMap { it.sharedScopeIds }.toSet() + flowStack.sharedScopeIds)

    val entries = buildList {
        addAll(tabStacks.getValue(tabs.startTab).entries)
        if (tabs.currentTab != tabs.startTab) addAll(tabStacks.getValue(tabs.currentTab).entries)
        addAll(flowStack.entries)
    }
    val showBottomBar = !state.isFlowOpen

    // Nav3's result bus for "pick and return" (ResultEffect / sendResult), one for the whole app.
    // Kept in a ViewModel: a result sent right before a configuration change is not lost.
    val resultEventBus = viewModel<ResultEventBusHolder>().bus

    CompositionLocalProvider(
        LocalResultEventBus provides resultEventBus,
        LocalSharedScopes provides sharedScopes,
    ) {
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

internal class ResultEventBusHolder : ViewModel() {
    val bus = ResultEventBus()
}

private class StackEntries(val entries: List<NavEntry<NavKey>>, val sharedScopeIds: Set<String>)

@Composable
private fun rememberStackEntries(
    scope: String,
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): StackEntries {
    // Own decorators per stack: saved state and ViewModels are never shared between stacks.
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )
    val (entries, scopeIds) = remember(backStack.toList()) {
        val provided = backStack.map(entryProvider)
        val sharedScopes = resolveSharedScopes(scope, backStack, provided.map { it.metadata.sharedScopeFirst() })
        val entries = backStack.mapIndexed { index, key ->
            val entry = provided[index]
            val id = entryId(scope, index, key)
            NavEntry(key, contentKey = id, metadata = entry.metadata) {
                CompositionLocalProvider(LocalSharedScopeRef provides sharedScopes[index]) { entry.Content() }
            }
        }
        entries to sharedScopes.mapNotNullTo(mutableSetOf()) { it?.id }
    }
    return StackEntries(rememberDecoratedNavEntries(entries, decorators), scopeIds)
}

/**
 * Keeps one ViewModel store and one saved state per live shared scope. The saved state is composed
 * here – always, as long as the scope lives – so it keeps saving even when the step that created
 * the ViewModel is no longer on screen. A scope that disappears from every stack is cleared.
 */
@Composable
private fun rememberSharedScopes(liveScopeIds: Set<String>): SharedScopes {
    val provider = rememberViewModelStoreProvider(key = "dev.cyrzan.skeleton.core.navigation.SharedScopes")
    val savedStates = rememberSaveableStateHolder()
    val scopes = remember(provider) { SharedScopes(provider) }

    liveScopeIds.forEach { id ->
        key(id) {
            savedStates.SaveableStateProvider(id) {
                scopes.savedStateOwners[id] = LocalSavedStateRegistryOwner.current
            }
        }
    }

    val previous = remember { mutableSetOf<String>() }
    SideEffect {
        (previous - liveScopeIds).forEach { ended ->
            // ViewModels are cleared once the last step using them leaves the composition (after its pop animation).
            provider.clearKey(ended)
            scopes.savedStateOwners.remove(ended)
            savedStates.removeState(ended)
        }
        previous.clear()
        previous.addAll(liveScopeIds)
    }
    return scopes
}
