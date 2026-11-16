package dev.cyrzan.skeleton.core.file

import android.content.Intent
import android.net.Uri

/**
 * A file written by [FileStorage].
 *
 * @param uri `content://` URI – safe to hand to other apps (with [Intent.FLAG_GRANT_READ_URI_PERMISSION]).
 * @param name the file's name on disk, after [FileName.sanitize].
 */
data class CachedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
)

/** Opens the file in another app; [Intent.createChooser] it or catch `ActivityNotFoundException` – there may be none. */
fun CachedFile.viewIntent(): Intent =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

/** Hands the file to the system share sheet. */
fun CachedFile.shareIntent(): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    name,
)
