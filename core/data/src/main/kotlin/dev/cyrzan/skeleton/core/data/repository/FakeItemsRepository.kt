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
        (1..ITEM_COUNT).map { number ->
            Item(
                id = "$number",
                title = "Item #$number",
                description = "Description of item #$number.",
                // Neighbours: lets the same details screen be opened again with other (or the same) arguments.
                relatedIds = listOf(number - 1, number + 1).filter { it in 1..ITEM_COUNT }.map(Int::toString),
            )
        },
    )

    override fun observeItems(): Flow<List<Item>> = items

    override fun observeItem(id: String): Flow<Item?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override fun observePromo(): Flow<Promo?> = flowOf(
        Promo(title = "Featured: Item #7", link = "https://kompas.example.com/items/7"),
    )

    private companion object {
        const val ITEM_COUNT = 20
    }
}
