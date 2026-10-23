package dev.cyrzan.skeleton.app.lock

import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Is it still you?" – asks a signed-in user for the PIN after the app spent [lockAfterMillis]
 * in the background, and on every cold start.
 *
 * Deliberately NOT part of navigation: it is an overlay above everything (tabs, flows, dialogs),
 * the back stacks underneath stay untouched, and after unlocking the user sees exactly what they
 * left. Sign-in (who are you?) is a navigation flow instead – see `LoginNavKey`.
 *
 * Plain Kotlin: fed with app lifecycle events (see `SkeletonApplication`) and a monotonic clock.
 */
internal class AppLock(
    private val session: SessionRepository,
    private val lockAfterMillis: Long,
    private val now: () -> Long,
    private val maxAttempts: Int = 5,
) {
    private val locked = MutableStateFlow(session.isSignedIn.value)
    val isLocked: StateFlow<Boolean> = locked.asStateFlow()

    private var backgroundedAt: Long? = null

    // A real app persists this, otherwise killing the app resets the counter.
    private var failedAttempts = 0

    fun onAppBackgrounded() {
        backgroundedAt = now()
        // Locking right away (not on return) means the first frame after returning is already the lock.
        if (lockAfterMillis <= 0) lockIfSignedIn()
    }

    fun onAppForegrounded() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (now() - since >= lockAfterMillis) lockIfSignedIn()
    }

    fun unlock(pin: String): UnlockResult {
        if (!locked.value) return UnlockResult.Unlocked
        if (session.verifyPin(pin)) {
            failedAttempts = 0
            locked.value = false
            return UnlockResult.Unlocked
        }
        failedAttempts++
        if (failedAttempts >= maxAttempts) {
            signOut()
            return UnlockResult.SignedOut
        }
        return UnlockResult.WrongPin(attemptsLeft = maxAttempts - failedAttempts)
    }

    /** "Not you?" – ends the session; navigation resets itself when the session ends. */
    fun signOut() {
        failedAttempts = 0
        session.signOut()
        locked.value = false
    }

    private fun lockIfSignedIn() {
        if (session.isSignedIn.value) locked.value = true
    }
}

internal sealed interface UnlockResult {
    data object Unlocked : UnlockResult
    data class WrongPin(val attemptsLeft: Int) : UnlockResult

    /** Too many wrong attempts: the session is gone, the user has to sign in again. */
    data object SignedOut : UnlockResult
}
