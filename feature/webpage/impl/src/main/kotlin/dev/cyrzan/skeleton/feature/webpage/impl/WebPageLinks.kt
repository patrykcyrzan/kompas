package dev.cyrzan.skeleton.feature.webpage.impl

import android.net.Uri
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult
import dev.cyrzan.skeleton.core.webview.HostAllowlist
import dev.cyrzan.skeleton.core.webview.UrlHandler

/** Pages that may stay inside the app. Everything else opens in the browser. */
internal val TRUSTED_HOSTS = HostAllowlist(setOf("example.com"))

/**
 * This feature's rules for links in the page, in order. They replace a feature's own `WebViewClient`:
 * everything else (state, errors, safe defaults) is the same for every WebView in the app.
 */
internal fun webPageUrlHandlers(
    openLink: (String) -> OpenLinkResult,
    openInBrowser: (Uri) -> Unit,
): List<UrlHandler> = listOf(
    // The web team's "open in a new tab": `?target=newTab` – the page itself asks to leave the app.
    UrlHandler { url ->
        // getQueryParameter throws on opaque URIs (mailto:x, tel:1) – only hierarchical ones have a query.
        val newTab = url.isHierarchical && url.getQueryParameter(TARGET_PARAM) == NEW_TAB
        if (newTab) openInBrowser(url)
        newTab
    },
    // App links open app screens, through the same guards as any other link. Blocked stays blocked – not the browser.
    UrlHandler { url -> openLink(url.toString()) != OpenLinkResult.Unsupported },
    // A page that isn't ours never gets the app's UI around it.
    UrlHandler { url ->
        val outside = url.scheme?.lowercase() in WEB_SCHEMES && !TRUSTED_HOSTS.allows(url.host)
        if (outside) openInBrowser(url)
        outside
    },
)

private const val TARGET_PARAM = "target"
private const val NEW_TAB = "newTab"
private val WEB_SCHEMES = setOf("https", "http")
