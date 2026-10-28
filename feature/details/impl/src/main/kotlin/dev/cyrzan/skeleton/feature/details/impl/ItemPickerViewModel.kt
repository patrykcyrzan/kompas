package dev.cyrzan.skeleton.feature.details.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.ItemsRepository
import dev.cyrzan.skeleton.core.model.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** No key needed: the only argument (current selection) is purely presentational and read by the screen. */
@HiltViewModel
internal class ItemPickerViewModel @Inject constructor(
    itemsRepository: ItemsRepository,
) : ViewModel() {

    val items: StateFlow<List<Item>?> = itemsRepository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)
}
