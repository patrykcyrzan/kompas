package dev.cyrzan.skeleton.core.deeplink

/** Where a link came from. Guards may treat sources differently; none of them is trusted. */
enum class DeepLinkSource {
    /**
     * From outside the app: browser, another app, the OS. Fully untrusted – any installed app can
     * send any URI to an exported Activity, even an `https` one of a verified domain.
     */
    External,

    /**
     * Opened by the app's own code, e.g. a link received from the backend. Still validated:
     * the content may be user-generated (chat messages, profiles...) or come from a compromised API.
     */
    InApp,
}
