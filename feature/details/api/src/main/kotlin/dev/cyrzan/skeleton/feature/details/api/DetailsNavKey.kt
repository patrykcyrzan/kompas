package dev.cyrzan.skeleton.feature.details.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.model.ItemId
import kotlinx.serialization.Serializable

@Serializable
data class DetailsNavKey(val itemId: ItemId) : NavKey, Destination
