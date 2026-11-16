package dev.cyrzan.skeleton.core.analytics.consent

/** What the user agrees to – one switch each on the consent screen. */
enum class ConsentCategory {
    /** Product analytics with a persistent identifier (Firebase Analytics, PostHog). */
    ANALYTICS,

    /** Crash and performance reports (Crashlytics). */
    DIAGNOSTICS,

    /** Ads, attribution, remarketing. */
    MARKETING,
}
