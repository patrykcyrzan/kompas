package dev.cyrzan.skeleton.app.lock

import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    private class FakeSession(signedIn: Boolean) : SessionRepository {
        override val isSignedIn = MutableStateFlow(signedIn)
        override fun signIn() { isSignedIn.value = true }
        override fun signOut() { isSignedIn.value = false }
        override fun verifyPin(pin: String) = pin == "1234"
    }

    private var now = 0L
    private val session = FakeSession(signedIn = true)

    private fun appLock(lockAfterMillis: Long = 10_000) =
        AppLock(session, lockAfterMillis, now = { now }, maxAttempts = 3)

    @Test
    fun `cold start with a session is locked, without one it is not`() {
        assertTrue(appLock().isLocked.value)

        session.signOut()
        assertFalse(appLock().isLocked.value)
    }

    @Test
    fun `locks only after the grace period in the background`() {
        val lock = appLock().apply { unlock("1234") }

        lock.onAppBackgrounded()
        now += 9_999
        lock.onAppForegrounded()
        assertFalse(lock.isLocked.value)

        lock.onAppBackgrounded()
        now += 10_000
        lock.onAppForegrounded()
        assertTrue(lock.isLocked.value)
    }

    @Test
    fun `zero grace locks already when going to the background`() {
        val lock = appLock(lockAfterMillis = 0).apply { unlock("1234") }

        lock.onAppBackgrounded()

        assertTrue(lock.isLocked.value)
    }

    @Test
    fun `never locks without a session`() {
        session.signOut()
        val lock = appLock(lockAfterMillis = 0)

        lock.onAppBackgrounded()
        lock.onAppForegrounded()

        assertFalse(lock.isLocked.value)
    }

    @Test
    fun `too many wrong pins sign the user out`() {
        val lock = appLock()

        assertEquals(UnlockResult.WrongPin(attemptsLeft = 2), lock.unlock("0000"))
        assertEquals(UnlockResult.WrongPin(attemptsLeft = 1), lock.unlock("0000"))
        assertEquals(UnlockResult.SignedOut, lock.unlock("0000"))

        assertFalse(session.isSignedIn.value)
        assertFalse(lock.isLocked.value)
    }

    @Test
    fun `correct pin unlocks and resets the attempts`() {
        val lock = appLock()
        lock.unlock("0000")

        assertEquals(UnlockResult.Unlocked, lock.unlock("1234"))
        assertFalse(lock.isLocked.value)
    }
}
