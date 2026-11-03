package dev.cyrzan.skeleton.core.deeplink

/**
 * Safe-to-log form of a link: scheme and host are kept, every path segment is masked,
 * query and fragment are dropped. Links may carry tokens (password reset, magic login) or personal data.
 *
 * Example: `https://app.example.com/reset/abc123?email=x` is logged as `https://app.example.com` + two masked segments + `?…`.
 */
fun redactDeepLink(uri: String): String {
    val link = DeepLink.parse(uri) ?: return "<malformed>"
    return buildString {
        append(link.scheme).append("://")
        link.host?.let(::append)
        link.pathSegments.forEach { _ -> append("/*") }
        if (link.queryParameters.isNotEmpty()) append("?…")
    }
}
