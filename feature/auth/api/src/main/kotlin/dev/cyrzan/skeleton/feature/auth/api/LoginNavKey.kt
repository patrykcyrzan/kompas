package dev.cyrzan.skeleton.feature.auth.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import kotlinx.serialization.Serializable

/**
 * Sign-in. Opened by the user (Profile) or by the authentication guard instead of a link that
 * needs a session – after signing in, the interrupted link is resumed.
 */
@Serializable
data object LoginNavKey : NavKey, Destination
