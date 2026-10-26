package dev.cyrzan.skeleton.feature.home.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.core.model.Promo

@Composable
internal fun HomeRoute(
    onItemClick: (Item) -> Unit,
    onPromoClick: (Promo) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onItemClick = onItemClick,
        onPromoClick = onPromoClick,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onItemClick: (Item) -> Unit,
    onPromoClick: (Promo) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            HomeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is HomeUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                uiState.promo?.let { promo ->
                    item(key = "promo") {
                        ListItem(
                            headlineContent = { Text(promo.title) },
                            supportingContent = { Text(promo.link) },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            modifier = Modifier.clickable { onPromoClick(promo) },
                        )
                    }
                }
                items(uiState.items, key = { it.id.value }) { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.description) },
                        modifier = Modifier.clickable { onItemClick(item) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    SkeletonTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                listOf(Item(ItemId("1"), "Item #1", "Description"), Item(ItemId("2"), "Item #2", "Description")),
                Promo("Featured: Item #7", "https://app.example.com/items/7"),
            ),
            onItemClick = {},
            onPromoClick = {},
            onSettingsClick = {},
        )
    }
}
