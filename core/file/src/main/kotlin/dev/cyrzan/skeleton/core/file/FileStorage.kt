package dev.cyrzan.skeleton.core.file

import java.io.InputStream

/**
 * Generic file operations – knows nothing about what the file is. Features decide the name, the MIME type
 * and where the bytes come from.
 */
interface FileStorage {

    /**
     * Writes [content] to the app's cache and closes it. A file with the same name is replaced.
     * The cache can be cleared by the system at any time – keep the result only as long as you need it.
     */
    suspend fun saveToCache(fileName: String, mimeType: String, content: InputStream): CachedFile

    suspend fun saveToCache(fileName: String, mimeType: String, bytes: ByteArray): CachedFile =
        saveToCache(fileName, mimeType, bytes.inputStream())

    /** Deletes everything [saveToCache] wrote, e.g. on sign-out. */
    suspend fun clearCache()
}
