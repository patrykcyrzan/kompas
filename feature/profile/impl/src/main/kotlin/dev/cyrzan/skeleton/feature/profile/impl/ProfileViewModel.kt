package dev.cyrzan.skeleton.feature.profile.impl

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = session.isSignedIn

    /** The app resets the whole navigation when the session ends – nothing to do here. */
    fun signOut() = session.signOut()
}
