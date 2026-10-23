package dev.cyrzan.skeleton.feature.orders.api

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute

object OrdersDeepLinks {
    /** `kompas://orders`, `https://<host>/orders` */
    val orders: DeepLinkRoute = deepLinkRoute("orders") { OrdersNavKey }

    /** `kompas://orders/1001`, `https://<host>/orders/1001` */
    val orderDetails: DeepLinkRoute = deepLinkRoute("orders/{orderId}") { params ->
        params.pathLong("orderId")?.let(::OrderDetailsNavKey)
    }
}
