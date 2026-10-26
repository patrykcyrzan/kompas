package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KClass

/*
 * ViewModels shared by several screens of one multi-screen flow (e.g. a 3-step form).
 *
 * Nav3 scopes ViewModels to a single entry. A shared scope:
 * - starts at the first step's entry and lives exactly as long as that entry is on its stack
 *   (leaving the form – back from step 1 or closeFlow() – clears it),
 * - has its OWN saved state that stays active while any step is on the stack, so SavedStateHandle
 *   changes made on step 3 survive process death (borrowing step 1's per-entry state would freeze it
 *   the moment step 1 leaves the composition),
 * - is identified by the first step's entry, so two forms open at once (e.g. on two tabs) are independent.
 */

/**
 * Entry metadata: this screen shares ViewModels with the nearest [first]-step entry at or below it
 * on the same stack. Put it on every step, including the first:
 *
 * ```
 * entry<FormStep2NavKey>(metadata = sharedViewModelScope(FormStep1NavKey::class)) { ... }
 * ```
 */
fun sharedViewModelScope(first: KClass<out NavKey>): Map<String, Any> = mapOf(SHARED_SCOPE_METADATA to first)

/** What a step gets: the shared [owner] (pass it to `hiltViewModel(...)`) and the [firstKey] – the arguments the flow was opened with. */
class SharedViewModelScope<K : NavKey> internal constructor(
    val owner: ViewModelStoreOwner,
    val firstKey: K,
)

/**
 * The shared scope of the current entry. The first step's key is typed by the caller:
 *
 * ```
 * val scope = rememberSharedViewModelScope<FormStep1NavKey>()
 * val viewModel = hiltViewModel<FormViewModel, FormViewModel.Factory>(scope.owner) { it.create(scope.firstKey) }
 * ```
 *
 * Any step can be the one that creates the ViewModel (after process death it is the visible one),
 * so the ViewModel takes its arguments from [SharedViewModelScope.firstKey], never from the current step.
 */
@Composable
fun <K : NavKey> rememberSharedViewModelScope(): SharedViewModelScope<K> {
    val ref = checkNotNull(LocalSharedScopeRef.current) { "This entry has no sharedViewModelScope(...) metadata" }
    val scopes = checkNotNull(LocalSharedScopes.current) { "rememberSharedViewModelScope must be called inside AppNavDisplay" }
    val savedState = checkNotNull(scopes.savedStateOwners[ref.id]) { "No saved state for shared scope ${ref.id}" }
    val owner = rememberViewModelStoreOwner(ref.id, scopes.provider, savedState)
    @Suppress("UNCHECKED_CAST")
    return remember(owner, ref) { SharedViewModelScope(owner, ref.firstKey as K) }
}

private const val SHARED_SCOPE_METADATA = "dev.cyrzan.skeleton.core.navigation.sharedViewModelScope"

internal fun Map<String, Any>.sharedScopeFirst(): KClass<*>? = this[SHARED_SCOPE_METADATA] as? KClass<*>

/** Shared scope an entry belongs to: the first step's entry id and key. */
internal data class SharedScopeRef(val id: String, val firstKey: NavKey)

/**
 * For each entry of one stack: the shared scope it belongs to, or `null`. Pure – unit-tested.
 * [firstSteps] holds, per entry, the first-step type declared by its metadata.
 */
internal fun resolveSharedScopes(
    stackScope: String,
    keys: List<NavKey>,
    firstSteps: List<KClass<*>?>,
): List<SharedScopeRef?> = keys.mapIndexed { index, key ->
    val first = firstSteps[index] ?: return@mapIndexed null
    val firstIndex = (index downTo 0).firstOrNull { first.isInstance(keys[it]) }
        ?: error("$key shares ViewModels with ${first.simpleName}, which is not below it on the stack")
    SharedScopeRef(entryId(stackScope, firstIndex, keys[firstIndex]), keys[firstIndex])
}

/** Owned by AppNavDisplay: the ViewModel stores of all shared scopes and their (always active) saved state. */
internal class SharedScopes(val provider: ViewModelStoreProvider) {
    val savedStateOwners = HashMap<String, SavedStateRegistryOwner>()
}

internal val LocalSharedScopeRef = staticCompositionLocalOf<SharedScopeRef?> { null }
internal val LocalSharedScopes = staticCompositionLocalOf<SharedScopes?> { null }
