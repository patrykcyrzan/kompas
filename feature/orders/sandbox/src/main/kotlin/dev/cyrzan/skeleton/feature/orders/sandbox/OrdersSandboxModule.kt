package dev.cyrzan.skeleton.feature.orders.sandbox

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import dev.cyrzan.skeleton.core.model.OrderId
import dev.cyrzan.skeleton.feature.orders.api.OrderDetailsNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.sandbox.Sandbox
import dev.cyrzan.skeleton.sandbox.Scenario

@Module
@InstallIn(SingletonComponent::class)
internal object OrdersSandboxModule {

    @Provides
    fun provideSandbox(session: SessionRepository) = Sandbox(
        name = "Orders",
        scenarios = listOf(
            Scenario("Orders list", OrdersNavKey),
            Scenario("Delivered order", OrdersNavKey, OrderDetailsNavKey(OrderId(1001))) { session.signIn() },
            Scenario("Order in progress", OrdersNavKey, OrderDetailsNavKey(OrderId(1003))) { session.signIn() },
            Scenario("Unknown order", OrdersNavKey, OrderDetailsNavKey(OrderId(9999))),
        ),
    )
}
