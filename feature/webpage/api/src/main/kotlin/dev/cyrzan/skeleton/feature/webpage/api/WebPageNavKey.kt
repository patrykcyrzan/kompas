package dev.cyrzan.skeleton.feature.webpage.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A web page shown inside the app (help, terms, a web flow), with the app's rules for its links.
 *
 * Not a [dev.cyrzan.skeleton.core.deeplink.Destination]: a link must not make the app show whatever page
 * it carries inside the app's own UI – that's a ready-made phishing page.
 *
 * @param title for the toolbar; `null` – the page's `<title>`.
 */
@Serializable
data class WebPageNavKey(val url: String, val title: String? = null) : NavKey
