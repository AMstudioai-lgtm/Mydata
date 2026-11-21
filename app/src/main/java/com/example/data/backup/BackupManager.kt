package com.example.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.images.ImageStore
import com.example.data.images.rowImagesFromJson
import com.example.data.images.writeImagesToValuesJson
import com.example.data.session.SessionProvider
import com.example.domain.logic.BackupCodec
import com.example.domain.model.BackupFormatException
import com.example.domain.model.BackupInfo
import com.example.domain.model.BackupJournal
import com.example.domain.model.BackupPayload
import com.example.domain.model.BackupProperty
import com.example.domain.model.BackupRow
import com.example.domain.model.RowImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Export et restauration de sauvegarde locale au format ZIP via Storage Access Framework.
 *
 * Contenu de l'archive :
 * - `data.json` : journaux, propriétés et lignes (y compris la corbeille) ;
 * - `images/<nom>` : copies binaires des photos associées aux lignes.
 *
 * À l'import : fusionne les données avec la base locale via `@Upsert` et rattache les
 * éléments restaurés à la session active (ou au mode local `null` hors-ligne) afin que
 * toutes les tables et photos réapparaissent immédiatement.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val sessionProvider: SessionProvider? = null,
    private val imageStore: ImageStore? = null
) {
    private val appContext: Context = context.applicationContext

    suspend fun exportBackup(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            runExport(uri)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(IllegalStateException("Échec de l'export : ${e.message ?: "erreur inconnue"}", e))
        }
    }

    suspend fun importBackup(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            runImport(uri)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(IllegalStateException("Échec de l'import : ${e.message ?: "erreur inconnue"}", e))
        }
    }

    private suspend fun runExport(uri: Uri): Result<BackupInfo> {
        val resolver = appContext.contentResolver
        val journals = db.journalDao().getAllJournals()
        val properties = db.propertyDao().getAllProperties()
        val rows = db.entryRowDao().getAllRows()

        val images = ArrayList<Pair<String, ByteArray>>()
        val usedEntryNames = HashSet<String>()

        val backupRows = rows.map { row ->
            row.toBackup(copyImagesForExport(row, resolver, images, usedEntryNames))
        }

        val payload = BackupPayload(
            journals = journals.map { it.toBackup() },
            properties = properties.map { it.toBackup() },
            rows = backupRows
        )

        val out = openOutputStream(uri, resolver)
            ?: return Result.failure(IllegalStateException("Impossible d'ouvrir la destination de sauvegarde."))
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_JSON))
            zip.write(BackupCodec.encode(payload).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for ((name, bytes) in images) {
                zip.putNextEntry(ZipEntry(IMAGES_DIR + name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val firstActiveId = journals.firstOrNull { it.deletedAt == null }?.id
        return Result.success(BackupInfo(journals.size, rows.size, images.size, firstActiveId))
    }

    private suspend fun copyImagesForExport(
        row: EntryRowEntity,
        resolver: ContentResolver,
        images: MutableList<Pair<String, ByteArray>>,
        usedEntryNames: MutableSet<String>
    ): String {
        val listed = rowImagesFromJson(row.valuesJson)
        if (listed.isEmpty()) return row.valuesJson
        var changed = false
        val mapped = listed.mapIndexed { index, image ->
            val bytes = readExportableImageBytes(row, image, resolver)
            if (bytes != null && bytes.isNotEmpty()) {
                val base = image.uri
                    ?.substringAfterLast('/')
                    ?.substringBefore('?')
                    ?.takeLast(40)
                    ?.ifBlank { "image.jpg" }
                    ?: "image.jpg"
                var candidate = sanitize("${row.id}_${index}_$base")
                while (!usedEntryNames.add(candidate)) {
                    candidate = sanitize("${row.id}_${index}_${UUID.randomUUID().toString().take(8)}_$base")
                }
                images += candidate to bytes
                changed = true
                image.copy(uri = IMAGES_DIR + candidate)
            } else {
                image
            }
        }
        return if (changed) writeImagesToValuesJson(row.valuesJson, mapped) else row.valuesJson
    }

    private suspend fun readExportableImageBytes(
        row: EntryRowEntity,
        image: RowImage,
        resolver: ContentResolver
    ): ByteArray? {
        readLocalUriBytes(image.uri, resolver)?.let { if (it.isNotEmpty()) return it }

        val remote = image.remote
        val store = imageStore
        if (!remote.isNullOrBlank() && store != null) {
            val ownerId = row.userId.orEmpty()
            val downloaded = store.ensureLocal(ownerId, row.id, image)
            readLocalUriBytes(downloaded.uri, resolver)?.let { if (it.isNotEmpty()) return it }
        }
        return null
    }

    private fun readLocalUriBytes(uriString: String?, resolver: ContentResolver): ByteArray? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val parsed = Uri.parse(uriString)
            when {
                parsed.scheme == "file" -> {
                    val path = parsed.path
                    if (!path.isNullOrBlank()) {
                        val file = File(path)
                        if (file.isFile && file.canRead()) return file.readBytes()
                    }
                    resolver.openInputStream(parsed)?.use { it.readBytes() }
                }
                parsed.scheme == null && uriString.startsWith("/") -> {
                    val file = File(uriString)
                    if (file.isFile && file.canRead()) file.readBytes() else null
                }
                else -> resolver.openInputStream(parsed)?.use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun runImport(uri: Uri): Result<BackupInfo> {
        val resolver = appContext.contentResolver
        val input = openInputStream(uri, resolver)
            ?: return Result.failure(IllegalStateException("Impossible d'ouvrir le fichier de sauvegarde."))
        var jsonBytes: ByteArray? = null
        val imageEntries = HashMap<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val normalizedName = normalizeZipPath(entry.name)
                    val bytes = zip.readBytes()
                    when {
                        normalizedName == DATA_JSON -> jsonBytes = bytes
                        normalizedName.startsWith(IMAGES_DIR) && !normalizedName.contains("..") -> {
                            val key = normalizedName.removePrefix(IMAGES_DIR)
                            if (key.isNotBlank()) {
                                imageEntries[key] = bytes
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val json = jsonBytes
            ?: return Result.failure(IllegalStateException("Archive invalide : $DATA_JSON introuvable."))
        val payload = try {
            BackupCodec.decode(json.toString(Charsets.UTF_8))
        } catch (e: BackupFormatException) {
            return Result.failure(e)
        }

        // Fusion et rattachement à la session active (ou locale hors-ligne si null)
        // pour éviter que les tables exportées sous un autre compte restent masquées.
        val activeUserId = sessionProvider?.currentUserId()
        val importNow = System.currentTimeMillis()

        var restoredImages = 0
        val restoredRows = payload.rows.map { row ->
            val (valuesJson, count) = restoreImagesForImport(row, imageEntries)
            restoredImages += count
            EntryRowEntity(
                id = row.id,
                journalId = row.journalId,
                valuesJson = valuesJson,
                createdAt = row.createdAt,
                updatedAt = maxOf(row.updatedAt, importNow),
                deletedAt = row.deletedAt,
                userId = activeUserId
            )
        }

        val restoredJournals = payload.journals.map { journal ->
            JournalEntity(
                id = journal.id,
                name = journal.name,
                createdAt = journal.createdAt,
                updatedAt = maxOf(journal.updatedAt, importNow),
                deletedAt = journal.deletedAt,
                userId = activeUserId
            )
        }

        val restoredProperties = payload.properties.map { prop ->
            PropertyEntity(
                id = prop.id,
                journalId = prop.journalId,
                name = prop.name,
                kind = prop.kind,
                format = prop.format,
                optionsJson = prop.optionsJson,
                sortOrder = prop.sortOrder,
                updatedAt = maxOf(prop.updatedAt, importNow),
                deletedAt = prop.deletedAt,
                userId = activeUserId
            )
        }

        db.withTransaction {
            restoredJournals.forEach { db.journalDao().upsert(it) }
            restoredProperties.forEach { db.propertyDao().upsert(it) }
            restoredRows.forEach { db.entryRowDao().upsert(it) }
        }

        val firstActiveId = restoredJournals.firstOrNull { it.deletedAt == null }?.id
        return Result.success(BackupInfo(restoredJournals.size, restoredRows.size, restoredImages, firstActiveId))
    }

    private fun restoreImagesForImport(
        row: BackupRow,
        imageEntries: Map<String, ByteArray>
    ): Pair<String, Int> {
        val listed = rowImagesFromJson(row.valuesJson)
        if (listed.isEmpty()) return row.valuesJson to 0
        var count = 0
        val mapped = listed.mapIndexed { index, image ->
            val rawRef = image.uri
            if (rawRef != null) {
                val ref = normalizeZipPath(rawRef)
                if (ref.startsWith(IMAGES_DIR) && !ref.contains("..")) {
                    val key = ref.removePrefix(IMAGES_DIR)
                    val bytes = imageEntries[key]
                    if (bytes == null || bytes.isEmpty()) {
                        image.copy(uri = null)
                    } else {
                        try {
                            val dir = File(appContext.filesDir, "row_images/restored_${sanitize(row.id)}")
                            if (!dir.isDirectory) dir.mkdirs()
                            var safe = key.substringAfterLast('/').ifBlank { "image.jpg" }
                            if (safe == "." || safe == "..") safe = "image.jpg"
                            val dest = File(dir, sanitize("${index}_$safe"))
                            dest.writeBytes(bytes)
                            count++
                            image.copy(uri = Uri.fromFile(dest).toString())
                        } catch (_: Exception) {
                            image
                        }
                    }
                } else {
                    image
                }
            } else {
                image
            }
        }
        return writeImagesToValuesJson(row.valuesJson, mapped) to count
    }

    private fun openInputStream(uri: Uri, resolver: ContentResolver) =
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            File(uri.path!!).takeIf { it.isFile && it.canRead() }?.inputStream()
        } else {
            resolver.openInputStream(uri)
        }

    private fun openOutputStream(uri: Uri, resolver: ContentResolver) =
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            File(uri.path!!).also { it.parentFile?.mkdirs() }.outputStream()
        } else {
            resolver.openOutputStream(uri)
        }

    private fun normalizeZipPath(path: String): String =
        path.replace('\\', '/').removePrefix("./").trimStart('/')

    private fun sanitize(name: String): String {
        val clean = name.map { c ->
            if (c.isLetterOrDigit() || c == '.' || c == '_' || c == '-') c else '_'
        }.joinToString("").take(120)
        return clean.ifBlank { "image" }
    }

    private fun JournalEntity.toBackup() = BackupJournal(id, name, createdAt, updatedAt, deletedAt, userId)

    private fun PropertyEntity.toBackup() = BackupProperty(
        id, journalId, name, kind, format, optionsJson, sortOrder, updatedAt, deletedAt, userId
    )

    private fun EntryRowEntity.toBackup(valuesJson: String) = BackupRow(
        id, journalId, valuesJson, createdAt, updatedAt, deletedAt, userId
    )

    companion object {
        const val DATA_JSON = "data.json"
        const val IMAGES_DIR = "images/"
    }
}
