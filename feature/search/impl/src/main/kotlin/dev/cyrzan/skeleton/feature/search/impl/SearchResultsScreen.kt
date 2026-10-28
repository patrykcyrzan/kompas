package dev.cyrzan.skeleton.feature.search.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchResultsNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchSort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchResultsRoute(
    navKey: SearchResultsNavKey,
    onItemClick: (ItemId) -> Unit,
    onBackClick: () -> Unit,
    viewModel: SearchResultsViewModel = hiltViewModel<SearchResultsViewModel, SearchResultsViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val list = results
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "criteria") {
                Text(
                    text = viewModel.criteria.summary(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()
            }
            when {
                list == null -> item(key = "loading") {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                list.isEmpty() -> item(key = "empty") { Text("No results", Modifier.padding(16.dp)) }
                else -> items(list, key = { it.id.value }) { item: Item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text("${item.category} · ${item.price} zł") },
                        modifier = Modifier.clickable { onItemClick(item.id) },
                    )
                }
            }
        }
    }
}

private fun SearchCriteria.summary(): String = listOfNotNull(
    query.takeIf { it.isNotBlank() }?.let { "„$it”" },
    category?.name,
    maxPrice?.let { "≤ $it zł" },
    sort.takeIf { it != SearchSort.Relevance }?.label(),
).ifEmpty { listOf("Everything") }.joinToString(" · ")
