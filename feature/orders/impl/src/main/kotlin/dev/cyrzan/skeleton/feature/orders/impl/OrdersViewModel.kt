package dev.cyrzan.skeleton.feature.orders.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.OrdersRepository
import dev.cyrzan.skeleton.core.model.Order
import dev.cyrzan.skeleton.feature.orders.api.OrderDetailsNavKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class OrdersViewModel @Inject constructor(
    ordersRepository: OrdersRepository,
) : ViewModel() {

    val orders: StateFlow<List<Order>?> = ordersRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)
}

@HiltViewModel(assistedFactory = OrderDetailsViewModel.Factory::class)
internal class OrderDetailsViewModel @AssistedInject constructor(
    @Assisted navKey: OrderDetailsNavKey,
    ordersRepository: OrdersRepository,
) : ViewModel() {

    val uiState: StateFlow<OrderDetailsUiState> = ordersRepository.observeOrder(navKey.orderId)
        .map { order -> if (order == null) OrderDetailsUiState.NotFound else OrderDetailsUiState.Success(order) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = OrderDetailsUiState.Loading)

    @AssistedFactory
    interface Factory {
        fun create(navKey: OrderDetailsNavKey): OrderDetailsViewModel
    }
}

internal sealed interface OrderDetailsUiState {
    data object Loading : OrderDetailsUiState
    data object NotFound : OrderDetailsUiState
    data class Success(val order: Order) : OrderDetailsUiState
}
