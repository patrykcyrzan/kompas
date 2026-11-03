package dev.cyrzan.skeleton.core.model

data class Order(
    val id: OrderId,
    val title: String,
    val status: String,
    val itemIds: List<ItemId>,
)
