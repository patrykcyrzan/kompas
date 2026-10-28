package dev.cyrzan.skeleton.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.ItemsRepository
import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchResultsNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchSort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The criteria come with the key; the results are loaded for them (never passed in). */
@HiltViewModel(assistedFactory = SearchResultsViewModel.Factory::class)
internal class SearchResultsViewModel @AssistedInject constructor(
    @Assisted navKey: SearchResultsNavKey,
    itemsRepository: ItemsRepository,
) : ViewModel() {

    val criteria: SearchCriteria = navKey.criteria

    // A real app passes the criteria to the backend; the fake repository is filtered here.
    val results: StateFlow<List<Item>?> = itemsRepository.observeItems()
        .map { items -> items.matching(criteria) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    @AssistedFactory
    interface Factory {
        fun create(navKey: SearchResultsNavKey): SearchResultsViewModel
    }
}

internal fun List<Item>.matching(criteria: SearchCriteria): List<Item> {
    val query = criteria.query.trim()
    val maxPrice = criteria.maxPrice
    val found = filter { item ->
        (query.isEmpty() || item.title.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true)) &&
            (criteria.category == null || item.category == criteria.category) &&
            (maxPrice == null || item.price <= maxPrice)
    }
    return when (criteria.sort) {
        SearchSort.Relevance -> found
        SearchSort.PriceAscending -> found.sortedBy(Item::price)
        SearchSort.PriceDescending -> found.sortedByDescending(Item::price)
    }
}
