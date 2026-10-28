package dev.cyrzan.skeleton.feature.search.api

import dev.cyrzan.skeleton.core.model.ItemCategory
import kotlinx.serialization.Serializable

/**
 * What to search for – a small VALUE object passed as a navigation argument.
 *
 * Fine in a key because it has no identity and no "fresher version" anywhere: it IS the request,
 * it is bounded in size, and it fits in a URL (see [SearchDeepLinks]). Results are not part of it –
 * the results screen loads them for these criteria.
 *
 * Belongs to this feature's contract (api), not to the domain: other features build it to open search.
 */
@Serializable
data class SearchCriteria(
    val query: String = "",
    val category: ItemCategory? = null,
    val maxPrice: Int? = null,
    val sort: SearchSort = SearchSort.Relevance,
) {
    init {
        require(query.length <= MAX_QUERY_LENGTH) { "Query too long" }
        require(maxPrice == null || maxPrice > 0) { "Max price must be positive" }
    }

    companion object {
        const val MAX_QUERY_LENGTH = 100
    }
}

@Serializable
enum class SearchSort { Relevance, PriceAscending, PriceDescending }
