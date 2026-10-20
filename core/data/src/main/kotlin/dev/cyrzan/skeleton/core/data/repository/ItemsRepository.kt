package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.Item
import kotlinx.coroutines.flow.Flow

interface ItemsRepository {
    fun observeItems(): Flow<List<Item>>
    fun observeItem(id: String): Flow<Item?>
}
