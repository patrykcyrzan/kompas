package dev.cyrzan.skeleton.core.model

data class Item(
    val id: ItemId,
    val title: String,
    val description: String,
    val relatedIds: List<ItemId> = emptyList(),
)
