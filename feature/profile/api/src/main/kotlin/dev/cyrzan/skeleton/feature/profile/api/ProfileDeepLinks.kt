package dev.cyrzan.skeleton.feature.profile.api

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute

object ProfileDeepLinks {
    /** `kompas://profile`, `https://<host>/profile` */
    val profile: DeepLinkRoute = deepLinkRoute("profile") { ProfileNavKey }

    /** `kompas://about`, `https://<host>/about` */
    val about: DeepLinkRoute = deepLinkRoute("about") { AboutNavKey }
}
