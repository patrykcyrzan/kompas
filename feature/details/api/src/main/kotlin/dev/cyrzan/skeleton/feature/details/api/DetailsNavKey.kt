package dev.cyrzan.skeleton.feature.details.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DetailsNavKey(val itemId: String) : NavKey
