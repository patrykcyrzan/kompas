package dev.cyrzan.skeleton.core.deeplink

/**
 * A path a feature can be opened with, e.g. `items/{itemId}`, plus an explicit mapping to a [Destination].
 *
 * The mapping is written by hand on purpose: the public URL format stays independent from the
 * shape (field names) of the navigation key. Renaming a key's field won't silently break links.
 *
 * Pattern rules: segments separated by `/`, a placeholder `{name}` must be a whole segment,
 * literals are case-sensitive. Query parameters are not part of the pattern – read them
 * via [DeepLinkParams.query].
 */
class DeepLinkRoute internal constructor(
    val pattern: String,
    internal val segments: List<Segment>,
    private val factory: (DeepLinkParams) -> Destination?,
) {
    internal sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Placeholder(val name: String) : Segment
    }

    /** Same literals in the same places = the two routes would match exactly the same links. */
    internal val shape: List<String?> = segments.map { (it as? Segment.Literal)?.value }

    internal fun resolve(pathSegments: List<String>, query: Map<String, String>): Destination? {
        if (pathSegments.size != segments.size) return null
        val pathParams = HashMap<String, String>()
        segments.forEachIndexed { index, segment ->
            val actual = pathSegments[index]
            when (segment) {
                is Segment.Literal -> if (segment.value != actual) return null
                is Segment.Placeholder -> pathParams[segment.name] = actual
            }
        }
        return factory(DeepLinkParams(pathParams, query))
    }

    override fun toString(): String = "DeepLinkRoute($pattern)"

    internal companion object {
        /** Literal beats placeholder at the first differing position (left to right). */
        val SPECIFICITY = Comparator<DeepLinkRoute> { a, b ->
            a.segments.zip(b.segments).forEach { (sa, sb) ->
                val la = sa is Segment.Literal
                val lb = sb is Segment.Literal
                if (la != lb) return@Comparator if (la) -1 else 1
            }
            a.segments.size - b.segments.size
        }
    }
}

class DeepLinkParams internal constructor(
    private val pathParams: Map<String, String>,
    private val queryParams: Map<String, String>,
) {
    /** Value of a `{name}` placeholder declared in the pattern. */
    fun path(name: String): String =
        requireNotNull(pathParams[name]) { "No placeholder {$name} in pattern" }

    fun query(name: String): String? = queryParams[name]

    /**
     * Placeholder value only if it looks like an identifier: `[A-Za-z0-9_-]`, 1–64 chars.
     * Prefer this over [path] for anything that ends up in an API call or a database query.
     */
    fun pathId(name: String): String? = path(name).takeIf(ID_REGEX::matches)

    /** Placeholder value as a positive number, without signs, leading zeros or other surprises. */
    fun pathLong(name: String): Long? = path(name).takeIf(POSITIVE_LONG_REGEX::matches)?.toLongOrNull()

    private companion object {
        val ID_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")
        val POSITIVE_LONG_REGEX = Regex("^[1-9][0-9]{0,18}$")
    }
}

private val PLACEHOLDER = Regex("^\\{([A-Za-z_][A-Za-z0-9_]*)\\}$")

/**
 * Declares a deep link route. [factory] may return `null` to reject arguments it can't handle
 * (e.g. a non-numeric id) – the link is then treated as not matched by this route.
 */
fun deepLinkRoute(pattern: String, factory: (DeepLinkParams) -> Destination?): DeepLinkRoute {
    val segments = pattern.trim('/').split('/').filter { it.isNotEmpty() }.map { raw ->
        val name = PLACEHOLDER.matchEntire(raw)?.groupValues?.get(1)
        when {
            name != null -> DeepLinkRoute.Segment.Placeholder(name)
            '{' in raw || '}' in raw ->
                throw IllegalArgumentException("Placeholder must be a whole segment: '$raw' in '$pattern'")
            else -> DeepLinkRoute.Segment.Literal(raw)
        }
    }
    val names = segments.filterIsInstance<DeepLinkRoute.Segment.Placeholder>().map { it.name }
    require(names.size == names.toSet().size) { "Duplicate placeholder in '$pattern'" }
    return DeepLinkRoute(pattern, segments, factory)
}
