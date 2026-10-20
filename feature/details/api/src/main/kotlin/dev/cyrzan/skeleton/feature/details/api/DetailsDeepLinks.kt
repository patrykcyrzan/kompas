package dev.cyrzan.skeleton.feature.details.api

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute

object DetailsDeepLinks {
    /** `kompas://items/42`, `https://<host>/items/42` */
    val item: DeepLinkRoute = deepLinkRoute("items/{itemId}") { params ->
        // pathId: only [A-Za-z0-9_-]{1,64} – the id ends up in data/API calls.
        val itemId = params.pathId("itemId")
        itemId?.let { DetailsNavKey(itemId = it) }
    }
}
