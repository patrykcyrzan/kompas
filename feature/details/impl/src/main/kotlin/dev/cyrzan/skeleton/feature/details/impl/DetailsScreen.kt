package dev.cyrzan.skeleton.feature.details.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey

@Composable
internal fun DetailsRoute(
    navKey: DetailsNavKey,
    onBackClick: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailsScreen(uiState = uiState, onBackClick = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsScreen(
    uiState: DetailsUiState,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((uiState as? DetailsUiState.Success)?.item?.title ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            when (uiState) {
                DetailsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DetailsUiState.NotFound -> Text("Item not found", Modifier.align(Alignment.Center))
                is DetailsUiState.Success -> Column {
                    Text(uiState.item.title, style = MaterialTheme.typography.headlineMedium)
                    Text(uiState.item.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
