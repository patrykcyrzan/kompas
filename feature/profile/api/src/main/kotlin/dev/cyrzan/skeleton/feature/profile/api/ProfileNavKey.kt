package dev.cyrzan.skeleton.feature.profile.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import kotlinx.serialization.Serializable

/** Root of the Profile tab. */
@Serializable
data object ProfileNavKey : NavKey, Destination

@Serializable
data object AboutNavKey : NavKey, Destination
