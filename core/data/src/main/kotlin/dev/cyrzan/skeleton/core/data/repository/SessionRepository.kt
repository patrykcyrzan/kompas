package dev.cyrzan.skeleton.core.data.repository

import kotlinx.coroutines.flow.StateFlow

/** The signed-in user's session. */
interface SessionRepository {
    val isSignedIn: StateFlow<Boolean>

    /**
     * Opaque account id from the backend (a UUID), `null` when signed out. Safe to hand to analytics:
     * never an e-mail, phone or login – those are personal data and Firebase forbids them in `setUserId`.
     */
    val userId: StateFlow<String?>

    fun signIn()

    fun signOut()

    /** Checks the app lock PIN of the signed-in user. */
    fun verifyPin(pin: String): Boolean
}
