package dev.cyrzan.skeleton.app.legacy

import javax.inject.Inject
import javax.inject.Singleton

/** Fake in-memory session of the legacy app – just enough to demonstrate the authentication guard. */
@Singleton
class SessionManager @Inject constructor() {

    @Volatile
    var isLoggedIn: Boolean = false
        private set

    fun logIn() {
        isLoggedIn = true
    }

    fun logOut() {
        isLoggedIn = false
    }
}
