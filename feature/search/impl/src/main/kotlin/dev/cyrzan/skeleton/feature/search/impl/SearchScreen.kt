package dev.cyrzan.skeleton.feature.search.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.cyrzan.skeleton.core.model.ItemCategory
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchSort

private val MAX_PRICES = listOf(null, 50, 100, 150)

/** The form's own input is UI state (saveable); only the finished criteria travel as an argument. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    initial: SearchCriteria,
    onSearch: (SearchCriteria) -> Unit,
    onBackClick: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf(initial.query) }
    var category by rememberSaveable { mutableStateOf(initial.category) }
    var maxPrice by rememberSaveable { mutableStateOf(initial.maxPrice) }
    var sort by rememberSaveable { mutableStateOf(initial.sort) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(SearchCriteria.MAX_QUERY_LENGTH) },
                label = { Text("Query") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ChipRow("Category", listOf(null) + ItemCategory.entries, category, { it?.name ?: "Any" }) { category = it }
            ChipRow("Max price", MAX_PRICES, maxPrice, { it?.let { price -> "≤ $price" } ?: "Any" }) { maxPrice = it }
            ChipRow("Sort", SearchSort.entries, sort, SearchSort::label) { sort = it }
            Button(
                onClick = { onSearch(SearchCriteria(query.trim(), category, maxPrice, sort)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Search")
            }
        }
    }
}

@Composable
private fun <T> ChipRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

internal fun SearchSort.label(): String = when (this) {
    SearchSort.Relevance -> "Relevance"
    SearchSort.PriceAscending -> "Price ↑"
    SearchSort.PriceDescending -> "Price ↓"
}
