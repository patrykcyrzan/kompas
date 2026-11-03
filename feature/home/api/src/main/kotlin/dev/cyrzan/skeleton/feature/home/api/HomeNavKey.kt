package dev.cyrzan.skeleton.feature.home.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import kotlinx.serialization.Serializable

@Serializable
data object HomeNavKey : NavKey, Destination
