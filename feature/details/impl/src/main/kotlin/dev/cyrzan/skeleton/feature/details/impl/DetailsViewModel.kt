package dev.cyrzan.skeleton.feature.details.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.ItemsRepository
import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The navigation key (with its arguments) is passed straight into the ViewModel
 * via assisted injection – no SavedStateHandle string lookups.
 */
@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
internal class DetailsViewModel @AssistedInject constructor(
    @Assisted navKey: DetailsNavKey,
    itemsRepository: ItemsRepository,
) : ViewModel() {

    val uiState: StateFlow<DetailsUiState> = itemsRepository.observeItem(navKey.itemId)
        .map { item -> if (item == null) DetailsUiState.NotFound else DetailsUiState.Success(item) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailsUiState.Loading,
        )

    @AssistedFactory
    interface Factory {
        fun create(navKey: DetailsNavKey): DetailsViewModel
    }
}

internal sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data object NotFound : DetailsUiState
    data class Success(val item: Item) : DetailsUiState
}
