package dev.cyrzan.skeleton.core.deeplink

/**
 * Minimal parsed URI. Implemented by hand instead of `android.net.Uri` / `java.net.URI`
 * to keep this module platform-neutral.
 */
data class DeepLink(
    val scheme: String,
    val host: String?,
    val pathSegments: List<String>,
    val queryParameters: Map<String, String>,
) {
    companion object {
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+\\-.]*$")

        /** Returns `null` when [uri] is not a valid absolute URI. */
        fun parse(uri: String): DeepLink? {
            val schemeEnd = uri.indexOf(':')
            if (schemeEnd <= 0) return null
            val scheme = uri.substring(0, schemeEnd)
            if (!SCHEME_REGEX.matches(scheme)) return null

            val rest = uri.substring(schemeEnd + 1).substringBefore('#')
            val hierarchical = rest.substringBefore('?')
            val rawQuery = rest.substringAfter('?', missingDelimiterValue = "")

            val host: String?
            val rawPath: String
            if (hierarchical.startsWith("//")) {
                val authority = hierarchical.substring(2).substringBefore('/')
                host = authority.substringAfterLast('@').substringBefore(':').ifEmpty { null }
                rawPath = hierarchical.substring(2 + authority.length)
            } else {
                host = null
                rawPath = hierarchical
            }

            val pathSegments = rawPath.split('/')
                .filter { it.isNotEmpty() }
                .map { percentDecode(it, plusAsSpace = false) ?: return null }

            val queryParameters = buildMap {
                rawQuery.split('&').filter { it.isNotEmpty() }.forEach { pair ->
                    val name = percentDecode(pair.substringBefore('='), plusAsSpace = true) ?: return null
                    val value = percentDecode(pair.substringAfter('=', ""), plusAsSpace = true) ?: return null
                    if (name !in this) put(name, value) // first occurrence wins
                }
            }

            return DeepLink(scheme.lowercase(), host, pathSegments, queryParameters)
        }
    }
}

/** Decodes `%XX` sequences as UTF-8. Returns `null` for malformed escapes. */
internal fun percentDecode(value: String, plusAsSpace: Boolean): String? {
    if ('%' !in value && !(plusAsSpace && '+' in value)) return value
    val result = StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        when {
            c == '%' -> {
                val bytes = mutableListOf<Byte>()
                while (i < value.length && value[i] == '%') {
                    if (i + 3 > value.length) return null
                    val byte = value.substring(i + 1, i + 3).toIntOrNull(16) ?: return null
                    bytes += byte.toByte()
                    i += 3
                }
                result.append(bytes.toByteArray().decodeToString())
            }
            c == '+' && plusAsSpace -> {
                result.append(' ')
                i++
            }
            else -> {
                result.append(c)
                i++
            }
        }
    }
    return result.toString()
}
