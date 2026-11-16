package dev.cyrzan.skeleton.feature.invoices.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The user's invoices, each downloadable as a PDF. */
@Serializable
data object InvoicesNavKey : NavKey
