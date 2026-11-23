package com.example.data.images

import android.content.Context
import android.net.Uri
import com.example.domain.model.RowImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Gestion locale des fichiers d'images associés aux lignes (cache et sauvegarde ZIP).
 *
 * Les photos restent stockées localement sur l'appareil et sont préservées dans les
 * archives ZIP d'export/import.
 */
class ImageStore(
    private val context: Context
) {
    private val appContext: Context = context.applicationContext

    suspend fun ensureLocal(userId: String, rowId: String, image: RowImage): RowImage =
        withContext(Dispatchers.IO) {
            image
        }

    fun cacheFile(userId: String, rowId: String, name: String): File {
        var safeName = name.substringAfterLast('/').substringAfterLast(File.separatorChar)
        if (safeName.isEmpty() || safeName == "." || safeName == "..") {
            safeName = "${UUID.randomUUID()}.jpg"
        }
        val dir = File(appContext.filesDir, "row_images/$userId/$rowId")
        if (!dir.isDirectory) dir.mkdirs()
        return File(dir, safeName)
    }

    fun isReadable(uriString: String?): Boolean {
        if (uriString.isNullOrEmpty()) return false
        return try {
            val stream = appContext.contentResolver.openInputStream(Uri.parse(uriString))
                ?: return false
            stream.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}
