package dev.cyrzan.skeleton.app.analytics

import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsIdentityTest {

    private class FakeSession(userId: String?) : SessionRepository {
        override val isSignedIn = MutableStateFlow(userId != null)
        override val userId = MutableStateFlow(userId)
        override fun signIn() { userId.value = "user-2"; isSignedIn.value = true }
        override fun signOut() { userId.value = null; isSignedIn.value = false }
        override fun verifyPin(pin: String) = true
    }

    private class RecordingAnalytics : Analytics {
        val identified = mutableListOf<String?>()
        override fun track(event: AnalyticsEvent) = Unit
        override fun identify(userId: String?) { identified += userId }
    }

    private val analytics = RecordingAnalytics()

    @Test
    fun `a session restored on cold start is identified right away`() = runTest(UnconfinedTestDispatcher()) {
        AnalyticsIdentity(FakeSession(userId = "user-1"), analytics).start(backgroundScope)

        assertEquals(listOf<String?>("user-1"), analytics.identified)
    }

    @Test
    fun `sign-out clears the id and the next sign-in sets the new one`() = runTest(UnconfinedTestDispatcher()) {
        val session = FakeSession(userId = "user-1")
        AnalyticsIdentity(session, analytics).start(backgroundScope)

        session.signOut()
        session.signIn()

        assertEquals(listOf("user-1", null, "user-2"), analytics.identified)
    }
}
