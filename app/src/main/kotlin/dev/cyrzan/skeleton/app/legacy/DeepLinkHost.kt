package dev.cyrzan.skeleton.app.legacy

import androidx.fragment.app.Fragment
import dev.cyrzan.skeleton.app.deeplink.LegacyDeepLinkNavigator

/**
 * Implemented by the hosting Activity. Typical for legacy code: Fragments reach Activity-scoped
 * objects through their host (the NavController-bound navigator can't come from Hilt directly).
 */
internal interface DeepLinkHost {
    val deepLinks: LegacyDeepLinkNavigator
}

internal val Fragment.deepLinks: LegacyDeepLinkNavigator
    get() = (requireActivity() as DeepLinkHost).deepLinks
