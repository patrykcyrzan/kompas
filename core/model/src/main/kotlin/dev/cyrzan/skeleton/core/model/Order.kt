package dev.cyrzan.skeleton.core.model

data class Order(
    val id: Long,
    val title: String,
    val status: String,
    val itemIds: List<String>,
)
