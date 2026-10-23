package dev.cyrzan.skeleton.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.feature.profile.api.ProfileNavKey

/** Bottom bar tabs, in display order. The first one is the start tab. */
internal enum class TopLevelTab(val root: NavKey, val label: String, val icon: ImageVector) {
    Home(HomeNavKey, "Home", Icons.Default.Home),
    Orders(OrdersNavKey, "Orders", Icons.Default.ShoppingCart),
    Profile(ProfileNavKey, "Profile", Icons.Default.Person),
    ;

    companion object {
        val roots: List<NavKey> = entries.map(TopLevelTab::root)
    }
}
