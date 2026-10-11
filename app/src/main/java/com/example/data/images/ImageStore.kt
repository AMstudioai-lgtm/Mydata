package com.example.data.images

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Téléversement et cache local des images d'une ligne.
 *
 * Ne lève jamais : toute image illisible ou tout échec réseau est renvoyé inchangé,
 * la synchronisation JSON restant utilisable en mode dégradé (images locales seules).
 */
class ImageStore(
    private val context: Context,
    private val storage: SupabaseStorageClient?,
    private val bucket: String = IMAGES_BUCKET
) {
    private val appContext: Context = context.applicationContext

    /**
     * Téléverse les images locales pas encore distantes (`remote == null`) vers
     * `<userId>/<rowId>/<uuid>.<ext>` et retourne la liste avec `remote` renseigné.
     * Sans effet (images inchangées) si [storage] est null ou si une image est illisible.
     */
    suspend fun uploadPending(userId: String, rowId: String, images: List<RowImage>): List<RowImage> =
        withContext(Dispatchers.IO) {
            val client = storage ?: return@withContext images
            val result = ArrayList<RowImage>(images.size)
            for (image in images) {
                result += try {
                    if (image.remote != null || image.uri.isNullOrEmpty()) image
                    else uploadOne(client, userId, rowId, image)
                } catch (_: Exception) {
                    image
                }
            }
            result
        }

    private suspend fun uploadOne(
        client: SupabaseStorageClient,
        userId: String,
        rowId: String,
        image: RowImage
    ): RowImage {
        val uri = Uri.parse(image.uri)
        val resolver = appContext.contentResolver
        val contentType = mimeToContentType(resolver.getType(uri))
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return image
        if (bytes.isEmpty()) return image
        val remote = "$userId/$rowId/${UUID.randomUUID()}.${extensionOf(contentType)}"
        client.upload(bucket, remote, bytes, contentType)
        return image.copy(remote = remote)
    }

    /**
     * Garantit une URI locale affichable : si [image.uri] est absente ou illisible mais
     * [RowImage.remote] présent, télécharge l'objet dans [cacheFile] et retourne l'image
     * avec `uri` pointant vers ce fichier cache. Sinon retourne l'image inchangée.
     *
     * Choix documenté : URI `file://` (via [Uri.fromFile]) plutôt que FileProvider,
     * pour ne déclarer aucun provider dans le manifeste.
     */
    suspend fun ensureLocal(userId: String, rowId: String, image: RowImage): RowImage =
        withContext(Dispatchers.IO) {
            try {
                val remote = image.remote
                if (remote.isNullOrEmpty()) return@withContext image
                if (isReadable(image.uri)) return@withContext image
                val client = storage ?: return@withContext image
                val file = cacheFile(userId, rowId, remote.substringAfterLast('/'))
                file.writeBytes(client.download(bucket, remote))
                image.copy(uri = Uri.fromFile(file).toString())
            } catch (_: Exception) {
                image
            }
        }

    /** Fichier cache sous `files/row_images/<userId>/<rowId>/`, dossiers créés si besoin. */
    fun cacheFile(userId: String, rowId: String, name: String): File {
        var safeName = name.substringAfterLast('/').substringAfterLast(File.separatorChar)
        if (safeName.isEmpty() || safeName == "." || safeName == "..") {
            safeName = "${UUID.randomUUID()}.jpg"
        }
        val dir = File(appContext.filesDir, "row_images/$userId/$rowId")
        if (!dir.isDirectory) dir.mkdirs()
        return File(dir, safeName)
    }

    private fun isReadable(uriString: String?): Boolean {
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

    // Seuls jpeg/png/webp sont émis ; tout autre type mime retombe sur JPEG par défaut.
    private fun mimeToContentType(mime: String?): String =
        when (mime?.lowercase()?.substringBefore(';')?.trim()) {
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            else -> "image/jpeg"
        }

    private fun extensionOf(contentType: String): String =
        when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
}
