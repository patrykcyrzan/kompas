package dev.cyrzan.skeleton.feature.orders.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrderDetailsNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.feature.orders.impl.OrderDetailsRoute
import dev.cyrzan.skeleton.feature.orders.impl.OrdersRoute
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey
import javax.inject.Inject

internal class OrdersEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<OrdersNavKey> {
            OrdersRoute(onOrderClick = { order -> navigator.navigate(OrderDetailsNavKey(order.id)) })
        }
        entry<OrderDetailsNavKey> { key ->
            OrderDetailsRoute(
                navKey = key,
                // Another feature's screen, pushed on the *current* tab.
                onItemClick = { itemId -> navigator.navigate(DetailsNavKey(itemId)) },
                // The form's only argument; the app opens it full screen.
                onReportProblemClick = { navigator.navigate(SupportRequestNavKey(orderId = key.orderId)) },
                onBackClick = { navigator.goBack() },
            )
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface OrdersNavigationModule {

    @Binds
    @IntoSet
    fun bindOrdersEntryProvider(impl: OrdersEntryProvider): EntryProviderInstaller
}
