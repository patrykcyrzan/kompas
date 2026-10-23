package dev.cyrzan.skeleton.feature.auth.impl

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import javax.inject.Inject

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    fun signIn() = session.signIn()
}
