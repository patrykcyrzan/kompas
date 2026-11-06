package dev.cyrzan.skeleton.core.webview

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

/**
 * One rule for links the user follows inside the page (`shouldOverrideUrlLoading`, main frame only).
 * Rules are asked in order; the first that returns `true` takes the link and the WebView doesn't load it.
 *
 * A feature passes its own rules instead of writing its own `WebViewClient`:
 * ```
 * UrlHandler { url -> (url.getQueryParameter("target") == "newTab").also { if (it) openInBrowser(url) } }
 * ```
 */
fun interface UrlHandler {
    fun handle(url: Uri): Boolean
}

/**
 * Hosts a WebView may load by itself, e.g. `example.com` (also `www.example.com`, `help.example.com`).
 * Matching on the host, not `startsWith` on the URL: `https://example.com.evil.net` and
 * `https://example.com@evil.net` both start with `https://example.com`.
 */
class HostAllowlist(hosts: Set<String>) {

    private val hosts = hosts.map { it.lowercase() }

    fun allows(host: String?): Boolean {
        val candidate = host?.lowercase()?.trimEnd('.') ?: return false
        return hosts.any { candidate == it || candidate.endsWith(".$it") }
    }
}

/**
 * Opens [uri] in another app: browser, dialer, mail. Only schemes that are safe to hand out –
 * never `intent:` (it can target any component, also unexported ones of this app), `file:`, `content:` or `javascript:`.
 *
 * @return `false` when the scheme isn't allowed or no app can open it.
 */
fun Context.openExternally(uri: Uri): Boolean {
    if (uri.scheme?.lowercase() !in EXTERNAL_SCHEMES) return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        // From a screen: the browser opens on top of the app and Back returns here. NEW_TASK only without an Activity.
        if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val EXTERNAL_SCHEMES = setOf("https", "http", "tel", "mailto", "sms", "geo")
