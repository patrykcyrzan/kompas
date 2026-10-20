package dev.cyrzan.skeleton.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Contract through which a `:feature:*:impl` module contributes its screens to the app's
 * navigation graph. Each impl binds an implementation `@IntoSet`; `:app` collects the whole
 * `Set<EntryProviderInstaller>` and installs it into a single `entryProvider { }`.
 *
 * Thanks to this `:app` does not know any concrete screen and features do not know each other –
 * they only share navigation keys from `:feature:*:api`.
 */
interface EntryProviderInstaller {
    fun EntryProviderScope<NavKey>.install(navigator: Navigator)
}
