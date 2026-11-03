package dev.cyrzan.skeleton.core.model

import kotlinx.serialization.Serializable

data class Item(
    val id: ItemId,
    val title: String,
    val description: String,
    val category: ItemCategory = ItemCategory.Tools,
    /** Whole units of the currency – enough for the skeleton. */
    val price: Int = 0,
    val relatedIds: List<ItemId> = emptyList(),
)

/** Serializable: used in search criteria, which are navigation arguments. */
@Serializable
enum class ItemCategory { Tools, Books, Garden }
