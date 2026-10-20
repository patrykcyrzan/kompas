package dev.cyrzan.skeleton.core.data.repository

import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.core.model.Promo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** In-memory placeholder until a real data source (network/database) is plugged in. */
internal class FakeItemsRepository @Inject constructor() : ItemsRepository {

    private val items = MutableStateFlow(
        List(20) { index ->
            Item(
                id = "${index + 1}",
                title = "Item #${index + 1}",
                description = "Description of item #${index + 1}.",
            )
        },
    )

    override fun observeItems(): Flow<List<Item>> = items

    override fun observeItem(id: String): Flow<Item?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override fun observePromo(): Flow<Promo?> = flowOf(
        Promo(title = "Featured: Item #7", link = "https://kompas.example.com/items/7"),
    )
}
