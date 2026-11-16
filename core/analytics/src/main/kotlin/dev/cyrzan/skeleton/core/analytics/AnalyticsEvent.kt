package dev.cyrzan.skeleton.core.analytics

data class AnalyticsEvent(
    val name: String,
    val params: List<Param> = emptyList(),
) {
    /** What may leave the device without consent: only parameters that cannot identify anyone. */
    fun anonymized(): AnalyticsEvent = copy(params = params.filter { it.dataClass == DataClass.ANONYMOUS })
}

/** [value] is a String, Number or Boolean – the common denominator of analytics SDKs. */
data class Param(val key: String, val value: Any, val dataClass: DataClass)

enum class DataClass {
    /** Coarse, cannot be linked to a person: screen name, button, app version. */
    ANONYMOUS,

    /** Not a person on its own, but linkable: item ids, timestamps, device model + locale. */
    PSEUDONYMOUS,

    /** Typed by or describing the user: search queries, e-mail, location. */
    PERSONAL,
}

fun anonymous(key: String, value: Any) = Param(key, value, DataClass.ANONYMOUS)

fun pseudonymous(key: String, value: Any) = Param(key, value, DataClass.PSEUDONYMOUS)

fun personal(key: String, value: Any) = Param(key, value, DataClass.PERSONAL)
