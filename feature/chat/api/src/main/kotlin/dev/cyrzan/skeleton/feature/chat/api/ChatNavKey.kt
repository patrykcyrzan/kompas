package dev.cyrzan.skeleton.feature.chat.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The conversation with support. Usually opened through [ChatButton], not navigated to directly. */
@Serializable
data object ChatNavKey : NavKey
