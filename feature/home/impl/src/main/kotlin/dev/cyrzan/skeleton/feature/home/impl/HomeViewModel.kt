package dev.cyrzan.skeleton.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.ItemsRepository
import dev.cyrzan.skeleton.core.model.Item
import dev.cyrzan.skeleton.core.model.Promo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    itemsRepository: ItemsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        itemsRepository.observeItems(),
        itemsRepository.observePromo(),
    ) { items, promo -> HomeUiState.Success(items, promo) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )
}

internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val items: List<Item>, val promo: Promo?) : HomeUiState
}
