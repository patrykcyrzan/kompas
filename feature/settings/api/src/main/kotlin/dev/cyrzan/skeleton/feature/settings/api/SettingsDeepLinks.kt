package dev.cyrzan.skeleton.feature.settings.api

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute

object SettingsDeepLinks {
    /** `kompas://settings`, `https://<host>/settings` */
    val settings: DeepLinkRoute = deepLinkRoute("settings") { SettingsNavKey }
}
