package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.core.model.Order
import dev.cyrzan.skeleton.core.model.OrderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** In-memory placeholder until a real data source (network/database) is plugged in. */
internal class FakeOrdersRepository @Inject constructor() : OrdersRepository {

    private val orders = MutableStateFlow(
        listOf(
            Order(id = OrderId(1001), title = "Order #1001", status = "Delivered", itemIds = listOf(ItemId("7"), ItemId("3"))),
            Order(id = OrderId(1002), title = "Order #1002", status = "Shipped", itemIds = listOf(ItemId("12"))),
            Order(id = OrderId(1003), title = "Order #1003", status = "Processing", itemIds = listOf(ItemId("1"), ItemId("7"), ItemId("20"))),
        ),
    )

    override fun observeOrders(): Flow<List<Order>> = orders

    override fun observeOrder(id: OrderId): Flow<Order?> =
        orders.map { list -> list.firstOrNull { it.id == id } }
}
