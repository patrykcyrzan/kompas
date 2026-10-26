package dev.cyrzan.skeleton.feature.orders.impl

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cyrzan.skeleton.core.model.ItemId
import dev.cyrzan.skeleton.core.model.Order
import dev.cyrzan.skeleton.feature.orders.api.OrderDetailsNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrdersRoute(
    onOrderClick: (Order) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Orders") }) }) { padding ->
        val list = orders
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(list, key = { it.id.value }) { order ->
                    ListItem(
                        headlineContent = { Text(order.title) },
                        supportingContent = { Text("${order.status} · ${order.itemIds.size} item(s)") },
                        modifier = Modifier.clickable { onOrderClick(order) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderDetailsRoute(
    navKey: OrderDetailsNavKey,
    onItemClick: (itemId: ItemId) -> Unit,
    onBackClick: () -> Unit,
    viewModel: OrderDetailsViewModel = hiltViewModel<OrderDetailsViewModel, OrderDetailsViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #${navKey.orderId}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            OrderDetailsUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            OrderDetailsUiState.NotFound -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Order not found")
            }
            is OrderDetailsUiState.Success -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item { ListItem(headlineContent = { Text("Status: ${state.order.status}") }) }
                items(state.order.itemIds) { itemId ->
                    ListItem(
                        headlineContent = { Text("Item #$itemId") },
                        supportingContent = { Text("Tap to open the item") },
                        modifier = Modifier.clickable { onItemClick(itemId) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
