package dev.cyrzan.skeleton.feature.home.api

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute

object HomeDeepLinks {
    /** `kompas://home`, `https://<host>/home` */
    val home: DeepLinkRoute = deepLinkRoute("home") { HomeNavKey }
}
