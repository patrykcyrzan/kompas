package dev.cyrzan.skeleton.feature.search.api

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute
import dev.cyrzan.skeleton.core.model.ItemCategory

object SearchDeepLinks {
    /**
     * `kompas://search?q=item&category=books&maxPrice=100&sort=price_asc`
     *
     * Every parameter is optional and validated; an unknown value makes the link invalid rather than
     * silently searching for something else.
     */
    val results: DeepLinkRoute = deepLinkRoute("search") { params ->
        val query = params.query("q").orEmpty()
        val category = params.query("category")?.let { value ->
            ItemCategory.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: return@deepLinkRoute null
        }
        val maxPrice = params.query("maxPrice")?.let { value ->
            value.toIntOrNull()?.takeIf { it in 1..1_000_000 } ?: return@deepLinkRoute null
        }
        val sort = params.query("sort")?.let { value -> SORT_PARAMS[value] ?: return@deepLinkRoute null } ?: SearchSort.Relevance
        if (query.length > SearchCriteria.MAX_QUERY_LENGTH) return@deepLinkRoute null

        SearchResultsNavKey(SearchCriteria(query, category, maxPrice, sort))
    }

    private val SORT_PARAMS = mapOf(
        "relevance" to SearchSort.Relevance,
        "price_asc" to SearchSort.PriceAscending,
        "price_desc" to SearchSort.PriceDescending,
    )
}
