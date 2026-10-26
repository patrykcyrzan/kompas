package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.filter

/**
 * A typed value a screen hands back to the screen below it ("pick an item and return it").
 * Declared in the api module of the feature that RETURNS it, next to its key:
 *
 * ```
 * data object ItemPickerResult : ResultKey<ItemId>("details.itemPicker")
 * ```
 *
 * Return it with [Navigator.goBackWithResult], receive it with [ResultEffect].
 * For state that outlives the two screens (a saved address, a rating) prefer a repository – results
 * are for transient choices the caller decides what to do with.
 */
abstract class ResultKey<T : Any>(val name: String)

/**
 * Results waiting for the entry they are addressed to. Delivery is near-instant (the receiver is the
 * new top of the stack), so results are not saved across process death – only retained across
 * configuration changes.
 */
class NavResultStore {
    private val pending = mutableStateMapOf<Pair<String, String>, Any>()

    internal fun put(entryId: String, key: ResultKey<*>, value: Any) {
        pending[entryId to key.name] = value
    }

    internal fun has(entryId: String, key: ResultKey<*>): Boolean = (entryId to key.name) in pending

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> take(entryId: String, key: ResultKey<T>): T? = pending.remove(entryId to key.name) as T?

    /** Drops results addressed to entries that are gone (popped before they could receive them). */
    internal fun retainOnly(liveEntryIds: Set<String>) {
        pending.keys.filter { (entryId, _) -> entryId !in liveEntryIds }.forEach(pending::remove)
    }

    internal fun clear() = pending.clear()
}

internal val LocalNavEntryId = staticCompositionLocalOf<String?> { null }
internal val LocalNavResultStore = staticCompositionLocalOf<NavResultStore?> { null }

/**
 * Calls [onResult] once for every [key] result returned to THIS screen (and only this one – the
 * same screen elsewhere on the stack, or on another tab, does not receive it).
 * Typically forwards it to the ViewModel, which keeps it (e.g. in its SavedStateHandle).
 */
@Composable
fun <T : Any> ResultEffect(key: ResultKey<T>, onResult: (T) -> Unit) {
    val entryId = checkNotNull(LocalNavEntryId.current) { "ResultEffect must be called inside a screen shown by AppNavDisplay" }
    val store = checkNotNull(LocalNavResultStore.current) { "ResultEffect must be called inside AppNavDisplay" }
    val currentOnResult by rememberUpdatedState(onResult)
    LaunchedEffect(store, entryId, key) {
        snapshotFlow { store.has(entryId, key) }
            .filter { it }
            .collect { store.take(entryId, key)?.let(currentOnResult) }
    }
}
