package dev.cyrzan.skeleton.feature.orders.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.deeplink.RequiresAuthentication
import dev.cyrzan.skeleton.core.model.OrderId
import kotlinx.serialization.Serializable

/** Root of the Orders tab. */
@Serializable
data object OrdersNavKey : NavKey, Destination

/** Personal data: a link to it without a session redirects to sign-in first. */
@Serializable
data class OrderDetailsNavKey(val orderId: OrderId) : NavKey, Destination, RequiresAuthentication
