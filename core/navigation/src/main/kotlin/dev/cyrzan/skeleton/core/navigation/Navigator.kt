package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey

/**
 * The single entry point features use to move around the app.
 *
 * Features never touch the back stack directly – it keeps navigation rules
 * (deep links, guards, synthetic back stacks) in one place.
 */
@Stable
interface Navigator {

    /** Pushes [key] on top of the back stack. */
    fun navigate(key: NavKey)

    /** Pops the top entry. Returns `false` when already at the root. */
    fun goBack(): Boolean

    /** Replaces the whole back stack. */
    fun resetTo(keys: List<NavKey>)

    /**
     * Opens an in-app link, e.g. one received from the backend (`https://<host>/items/42`).
     *
     * The link goes through the same validation and guards as links from outside the app.
     * On success the target is pushed on top of the current screen.
     */
    fun openLink(uri: String): OpenLinkResult

    /**
     * Resumes the link that was interrupted by a guard redirect (e.g. to login).
     * Call it once the reason for the redirect is gone. The link is validated again.
     *
     * @return `true` if something was opened.
     */
    fun resumePendingLink(): Boolean
}

enum class OpenLinkResult {
    /** The target (or a redirect such as login) was opened. */
    Opened,

    /** A guard rejected it. Do NOT fall back to opening it elsewhere. */
    Blocked,

    /** Not an app link (or malformed). The caller may decide what to do, e.g. open `https` in a browser. */
    Unsupported,
}
