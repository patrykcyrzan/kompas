package dev.cyrzan.skeleton.core.file

import android.content.Context
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

internal class AndroidFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileStorage {

    private val root: File get() = File(context.cacheDir, ROOT_DIR)

    override suspend fun saveToCache(fileName: String, mimeType: String, content: InputStream): CachedFile =
        withContext(Dispatchers.IO) {
            val name = FileName.sanitize(fileName)
            val dir = root.apply { mkdirs() }
            val target = File(dir, name)
            // Written next to the target and renamed at the end: an error or cancellation never leaves half a file under the real name.
            val temp = File.createTempFile("download", ".tmp", dir)
            try {
                content.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                        }
                    }
                }
                if (!temp.renameTo(target)) throw IOException("Cannot move the download to $target")
            } finally {
                temp.delete()
            }
            CachedFile(
                uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, target),
                name = name,
                mimeType = mimeType,
            )
        }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) { root.deleteRecursively() }
    }

    private companion object {
        /** Keep in sync with `res/xml/core_file_paths.xml`. */
        const val ROOT_DIR = "files"

        /** Keep in sync with the provider in this module's manifest. */
        const val AUTHORITY_SUFFIX = ".core.file.fileprovider"
    }
}

internal class CoreFileProvider : FileProvider(R.xml.core_file_paths)
