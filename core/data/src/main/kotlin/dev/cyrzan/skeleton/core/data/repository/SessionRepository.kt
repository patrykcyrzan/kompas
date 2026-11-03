package dev.cyrzan.skeleton.core.data.repository

import kotlinx.coroutines.flow.StateFlow

/** The signed-in user's session. */
interface SessionRepository {
    val isSignedIn: StateFlow<Boolean>

    fun signIn()

    fun signOut()

    /** Checks the app lock PIN of the signed-in user. */
    fun verifyPin(pin: String): Boolean
}
