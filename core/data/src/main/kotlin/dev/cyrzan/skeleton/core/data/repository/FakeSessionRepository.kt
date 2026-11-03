package dev.cyrzan.skeleton.core.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Placeholder session: survives process death (like a real token would), PIN is always [DEMO_PIN].
 * A real implementation keeps the token encrypted and never stores the PIN in plain text.
 */
internal class FakeSessionRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SessionRepository {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val signedIn = MutableStateFlow(prefs.getBoolean(KEY_SIGNED_IN, false))

    override val isSignedIn: StateFlow<Boolean> = signedIn.asStateFlow()

    override fun signIn() = update(true)

    override fun signOut() = update(false)

    override fun verifyPin(pin: String): Boolean = pin == DEMO_PIN

    private fun update(value: Boolean) {
        prefs.edit { putBoolean(KEY_SIGNED_IN, value) }
        signedIn.value = value
    }

    private companion object {
        const val KEY_SIGNED_IN = "signed_in"
        const val DEMO_PIN = "1234"
    }
}
