package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.core.model.Promo
import kotlinx.coroutines.flow.Flow

interface ItemsRepository {
    fun observeItems(): Flow<List<Item>>
    fun observeItem(id: String): Flow<Item?>
    fun observePromo(): Flow<Promo?>
}
