package dev.cyrzan.skeleton.app.network

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Whether [MockInterceptor] answers from assets. Survives restarts; toggled in [DebugMenuActivity]. */
@Singleton
internal class MockSettings @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("debug_network", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    private companion object {
        const val KEY_ENABLED = "mocks_enabled"
    }
}
