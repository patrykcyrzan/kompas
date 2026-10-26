package dev.cyrzan.skeleton.feature.details.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.model.ItemId
import kotlinx.serialization.Serializable

/**
 * Lets the user choose an item and returns its [ItemId] under [ItemPickerResult.KEY].
 * Not a [dev.cyrzan.skeleton.core.deeplink.Destination]: a picker only makes sense opened by a screen
 * that waits for its result, never from a link.
 *
 * @param selected highlighted as the current choice (optional argument – default `null`).
 */
@Serializable
data class ItemPickerNavKey(val selected: ItemId? = null) : NavKey

/**
 * Result of [ItemPickerNavKey], sent through Nav3's ResultEventBus:
 *
 * ```
 * ResultEffect<ItemId>(ItemPickerResult.KEY) { itemId -> ... }
 * ```
 *
 * An explicit key (not the default one derived from the type): other screens may return an [ItemId] too.
 */
object ItemPickerResult {
    const val KEY: String = "dev.cyrzan.skeleton.feature.details.itemPicker"
}
