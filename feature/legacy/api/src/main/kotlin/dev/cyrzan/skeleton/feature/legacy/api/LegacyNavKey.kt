package dev.cyrzan.skeleton.feature.legacy.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A screen that still lives in `:app` and is built from an XML layout – stands in for the old
 * monolith's Fragments. Features open it by this key, like any other screen; `:app` provides the entry.
 */
@Serializable
data object LegacyNavKey : NavKey
