package dev.cyrzan.skeleton.feature.settings.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import kotlinx.serialization.Serializable

@Serializable
data object SettingsNavKey : NavKey, Destination
