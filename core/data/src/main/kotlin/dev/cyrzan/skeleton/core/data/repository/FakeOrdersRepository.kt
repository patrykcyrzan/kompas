package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** In-memory placeholder until a real data source (network/database) is plugged in. */
internal class FakeOrdersRepository @Inject constructor() : OrdersRepository {

    private val orders = MutableStateFlow(
        listOf(
            Order(id = 1001, title = "Order #1001", status = "Delivered", itemIds = listOf("7", "3")),
            Order(id = 1002, title = "Order #1002", status = "Shipped", itemIds = listOf("12")),
            Order(id = 1003, title = "Order #1003", status = "Processing", itemIds = listOf("1", "7", "20")),
        ),
    )

    override fun observeOrders(): Flow<List<Order>> = orders

    override fun observeOrder(id: Long): Flow<Order?> =
        orders.map { list -> list.firstOrNull { it.id == id } }
}
