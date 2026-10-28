package dev.cyrzan.skeleton.feature.support.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.model.OrderId
import kotlinx.serialization.Serializable

/**
 * Opens the "contact support" form (3 steps, sent at the end). The only public key – the other steps
 * are internal to the feature. [orderId] prefills the request when opened from an order.
 */
@Serializable
data class SupportRequestNavKey(val orderId: OrderId? = null) : NavKey
