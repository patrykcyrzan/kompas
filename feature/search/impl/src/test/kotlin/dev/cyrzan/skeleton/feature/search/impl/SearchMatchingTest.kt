package dev.cyrzan.skeleton.feature.search.impl

import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.core.model.ItemCategory
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchSort
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchMatchingTest {

    private fun item(id: String, category: ItemCategory, price: Int) =
        Item(ItemId(id), "Item #$id", "Description", category, price)

    private val items = listOf(
        item("1", ItemCategory.Books, 120),
        item("2", ItemCategory.Tools, 40),
        item("3", ItemCategory.Books, 30),
    )

    @Test
    fun `filters by category and price, then sorts`() {
        val criteria = SearchCriteria(category = ItemCategory.Books, maxPrice = 150, sort = SearchSort.PriceAscending)

        assertEquals(listOf("3", "1"), items.matching(criteria).map { it.id.value })
    }

    @Test
    fun `query matches the title, case-insensitively`() {
        assertEquals(listOf("2"), items.matching(SearchCriteria(query = "item #2")).map { it.id.value })
    }
}
