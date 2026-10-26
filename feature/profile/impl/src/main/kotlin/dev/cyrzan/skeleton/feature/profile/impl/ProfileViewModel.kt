package dev.cyrzan.skeleton.feature.profile.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import dev.cyrzan.skeleton.core.model.ItemId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val session: SessionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = session.isSignedIn

    /**
     * A result from another screen (the item picker), kept by the receiver: SavedStateHandle survives
     * process death. A choice that must outlive this screen would go to a repository instead.
     */
    val favourite: StateFlow<ItemId?> = savedStateHandle.getStateFlow<String?>(KEY_FAVOURITE, null)
        .map { it?.let(::ItemId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, savedStateHandle.get<String>(KEY_FAVOURITE)?.let(::ItemId))

    fun onFavouritePicked(itemId: ItemId) {
        savedStateHandle[KEY_FAVOURITE] = itemId.value
    }

    /** The app resets the whole navigation when the session ends – nothing to do here. */
    fun signOut() = session.signOut()

    private companion object {
        const val KEY_FAVOURITE = "favourite"
    }
}
