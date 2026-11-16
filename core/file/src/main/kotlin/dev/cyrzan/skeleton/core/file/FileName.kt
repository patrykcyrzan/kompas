package dev.cyrzan.skeleton.core.file

/** File names that come from the backend or from users. */
object FileName {

    private val forbidden = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    /** A name that stays inside its directory: no separators or control characters, no leading dots. */
    fun sanitize(name: String, fallback: String = "file"): String =
        name.replace(forbidden, "_")
            .trim()
            .trimStart('.')
            .take(MAX_LENGTH)
            .ifBlank { fallback }

    /** Most file systems allow 255 bytes; a margin for multi-byte characters. */
    private const val MAX_LENGTH = 120
}
