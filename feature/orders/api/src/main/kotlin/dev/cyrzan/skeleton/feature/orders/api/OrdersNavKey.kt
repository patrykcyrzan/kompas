package dev.cyrzan.skeleton.feature.orders.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import kotlinx.serialization.Serializable

/** Root of the Orders tab. */
@Serializable
data object OrdersNavKey : NavKey, Destination

@Serializable
data class OrderDetailsNavKey(val orderId: Long) : NavKey, Destination
