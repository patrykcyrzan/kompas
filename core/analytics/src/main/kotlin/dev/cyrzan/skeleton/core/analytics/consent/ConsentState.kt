package dev.cyrzan.skeleton.core.analytics.consent

data class ConsentState(
    val granted: Set<ConsentCategory>,
    /** Version of the privacy policy the decision was made for. */
    val policyVersion: Int,
    /** `null` – the user has not decided yet, which counts as "no" for everything. */
    val decidedAtMillis: Long?,
) {
    fun isGranted(category: ConsentCategory) = category in granted

    companion object {
        val Undecided = ConsentState(granted = emptySet(), policyVersion = 0, decidedAtMillis = null)
    }
}
