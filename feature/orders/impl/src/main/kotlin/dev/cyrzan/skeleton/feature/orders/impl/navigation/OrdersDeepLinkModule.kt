package dev.cyrzan.skeleton.feature.orders.impl.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.feature.orders.api.OrdersDeepLinks

@Module
@InstallIn(SingletonComponent::class)
internal object OrdersDeepLinkModule {

    @Provides
    @ElementsIntoSet
    fun provideOrdersDeepLinks(): Set<DeepLinkRoute> = setOf(OrdersDeepLinks.orders, OrdersDeepLinks.orderDetails)
}
