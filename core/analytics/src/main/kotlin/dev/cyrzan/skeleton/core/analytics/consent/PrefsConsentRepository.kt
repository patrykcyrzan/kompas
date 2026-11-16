package dev.cyrzan.skeleton.core.analytics.consent

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Keeps the decision with its policy version and time – that is the record you show
 * when asked to prove consent. A decision made for an older policy counts as no decision.
 */
internal class PrefsConsentRepository @Inject constructor(
    @ApplicationContext context: Context,
) : ConsentRepository {

    private val prefs = context.getSharedPreferences("consent", Context.MODE_PRIVATE)
    private val current = MutableStateFlow(load())

    override val state: StateFlow<ConsentState> = current.asStateFlow()

    override val needsDecision: Boolean
        get() = current.value.decidedAtMillis == null

    override fun decide(granted: Set<ConsentCategory>) {
        val state = ConsentState(granted, CURRENT_POLICY_VERSION, System.currentTimeMillis())
        prefs.edit {
            putStringSet(KEY_GRANTED, granted.map { it.name }.toSet())
            putInt(KEY_POLICY_VERSION, state.policyVersion)
            putLong(KEY_DECIDED_AT, requireNotNull(state.decidedAtMillis))
        }
        current.value = state
    }

    private fun load(): ConsentState {
        if (prefs.getInt(KEY_POLICY_VERSION, 0) != CURRENT_POLICY_VERSION) return ConsentState.Undecided
        val granted = prefs.getStringSet(KEY_GRANTED, emptySet()).orEmpty()
            .mapNotNull { name -> ConsentCategory.entries.find { it.name == name } }
            .toSet()
        return ConsentState(granted, CURRENT_POLICY_VERSION, prefs.getLong(KEY_DECIDED_AT, 0))
    }

    private companion object {
        /** Bump when the privacy policy changes in a way that needs a new decision. */
        const val CURRENT_POLICY_VERSION = 1

        const val KEY_GRANTED = "granted"
        const val KEY_POLICY_VERSION = "policy_version"
        const val KEY_DECIDED_AT = "decided_at"
    }
}
