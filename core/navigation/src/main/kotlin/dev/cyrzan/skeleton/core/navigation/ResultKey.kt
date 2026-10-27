package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.ResultEventBus

/**
 * Typed key of a screen's result, declared next to its NavKey in the returning feature's `api`:
 *
 * ```
 * val ItemPickerResult = ResultKey<ItemId>("com.example.feature.details.itemPicker")
 *
 * navigator.goBackWithResult(ItemPickerResult, itemId)                // the screen that returns it
 * ResultEffect(ItemPickerResult) { itemId -> viewModel.onPicked(itemId) } // the screen that waits for it
 * ```
 *
 * The type is part of the key, so the sender and the receiver cannot disagree on it. [name] must be
 * unique in the app – use the feature's package. Underneath it is Nav3's [ResultEventBus].
 */
class ResultKey<T : Any>(val name: String) {
    override fun toString(): String = "ResultKey($name)"
}

/**
 * Receives results sent with [Navigator.goBackWithResult] under [key], while this screen is composed.
 * Results sent while it is not (e.g. still under the screen that returns them) are buffered and
 * delivered once it is back.
 */
@Composable
fun <T : Any> ResultEffect(key: ResultKey<T>, onResult: suspend (T) -> Unit) =
    ResultEffect<T>(resultKey = key.name, onResult = onResult)

/**
 * The app-wide [ResultEventBus], kept in a ViewModel so a result sent right before a configuration
 * change is not lost. Shared by the navigator (which sends) and [AppNavDisplay] (which provides it to screens).
 */
@Composable
fun rememberAppResultEventBus(): ResultEventBus = viewModel<ResultEventBusHolder>().bus

internal class ResultEventBusHolder : ViewModel() {
    val bus = ResultEventBus()
}
