package dev.cyrzan.skeleton.feature.search.api

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.deeplink.Destination
import kotlinx.serialization.Serializable

/** The search form, prefilled with [initial] (e.g. when going back from results opened by a link). */
@Serializable
data class SearchNavKey(val initial: SearchCriteria = SearchCriteria()) : NavKey, Destination

/** Results for [criteria]. Other features open it directly, e.g. "more in this category". */
@Serializable
data class SearchResultsNavKey(val criteria: SearchCriteria) : NavKey, Destination
