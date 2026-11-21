package com.example.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Gestionnaire de stockage local des images d'une ligne (copie interne, capture photo,
 * rééchantillonnage et cache LRU des vignettes). Isolé des composants Compose UI.
 */
object RowImageFileStore {

    private const val DIRECTORY = "row_images"
    private const val IMPORT_MAX_PX = 1600
    private const val THUMBNAIL_MAX_PX = 640

    private val thumbnails = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    suspend fun importImage(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        var target: File? = null
        try {
            val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            target = File(dir, "${UUID.randomUUID()}.jpg")
            val stream = context.contentResolver.openInputStream(source) ?: return@withContext null
            stream.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
            downscale(target, IMPORT_MAX_PX)
            Uri.fromFile(target).toString()
        } catch (_: Exception) {
            target?.delete()
            null
        }
    }

    fun newCaptureFile(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun captureUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun loadThumbnail(context: Context, uriString: String): Bitmap? {
        thumbnails.get(uriString)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bytes = readBytes(context, uriString) ?: return@withContext null
            val bitmap = decodeSampled(bytes, THUMBNAIL_MAX_PX) ?: return@withContext null
            thumbnails.put(uriString, bitmap)
            bitmap
        }
    }

    fun readBytes(context: Context, uriString: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            when {
                uri.scheme == "file" -> uri.path?.let { File(it).takeIf(File::canRead)?.readBytes() }
                uri.scheme == null && uriString.startsWith("/") ->
                    File(uriString).takeIf(File::canRead)?.readBytes()
                else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun downscale(file: File, maxPx: Int) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= maxPx || longest <= 0) return

        val options = BitmapFactory.Options().apply {
            var sample = 1
            while (longest / (sample * 2) >= maxPx) sample *= 2
            inSampleSize = sample
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return

        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            val compressed = FileOutputStream(temp).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (compressed && file.delete() && temp.renameTo(file)) {
                return
            }
            temp.delete()
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampled(bytes: ByteArray, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        val options = BitmapFactory.Options().apply {
            var sample = 1
            while (longest / (sample * 2) >= maxPx) sample *= 2
            inSampleSize = sample
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
