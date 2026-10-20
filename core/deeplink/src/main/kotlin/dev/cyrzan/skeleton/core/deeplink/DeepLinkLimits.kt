package dev.cyrzan.skeleton.core.deeplink

/**
 * Hard limits applied to every link before any route is matched. Anything outside them is
 * rejected as [DeepLinkResult.Invalid] – legitimate links are short and boring.
 */
data class DeepLinkLimits(
    val maxUriLength: Int = 2048,
    val maxPathSegments: Int = 16,
    val maxQueryParameters: Int = 32,
    /** Max length of a single decoded path segment, query name or query value. */
    val maxValueLength: Int = 256,
)

internal fun DeepLinkLimits.validateRaw(uri: String): String? = when {
    uri.length > maxUriLength -> "URI too long"
    uri.any { it.isUnsafe() || it == ' ' } -> "Control or whitespace characters in URI"
    else -> null
}

internal fun DeepLinkLimits.validateParsed(link: DeepLink): String? {
    if (link.pathSegments.size > maxPathSegments) return "Too many path segments"
    if (link.queryParameters.size > maxQueryParameters) return "Too many query parameters"
    link.pathSegments.forEach { segment ->
        // Decoded "%2F" / "%5C" / "." / ".." could escape the path once used e.g. in an API call.
        if (segment == "." || segment == ".." || '/' in segment || '\\' in segment) return "Path traversal attempt"
        validateValue(segment)?.let { return it }
    }
    link.queryParameters.forEach { (name, value) ->
        validateValue(name)?.let { return it }
        validateValue(value)?.let { return it }
    }
    return null
}

private fun DeepLinkLimits.validateValue(value: String): String? = when {
    value.length > maxValueLength -> "Value too long"
    value.any { it.isUnsafe() } -> "Control characters in decoded value"
    else -> null
}

/** C0/C1 control characters, DEL, and Unicode bidi overrides used to spoof how text is displayed. */
private fun Char.isUnsafe(): Boolean =
    code < 0x20 || code in 0x7F..0x9F || code in 0x202A..0x202E || code in 0x2066..0x2069
