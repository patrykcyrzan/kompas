package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.Order
import dev.cyrzan.skeleton.core.model.OrderId
import kotlinx.coroutines.flow.Flow

interface OrdersRepository {
    fun observeOrders(): Flow<List<Order>>
    fun observeOrder(id: OrderId): Flow<Order?>
}
