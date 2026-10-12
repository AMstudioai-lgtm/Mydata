package com.example.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.images.rowImagesFromJson
import com.example.data.images.writeImagesToValuesJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupInfo(val journals: Int, val rows: Int, val images: Int)

// Export/import de sauvegarde via Storage Access Framework : aucun permission
// requise, l'utilisateur choisit le fichier via le sélecteur système.
// Format : zip contenant `data.json` (journaux + propriétés + lignes, tous champs
// y compris la corbeille) et `images/<nom>` (copies des fichiers `file://` internes,
// référencés par leur nom d'entrée dans le values_json exporté).
class BackupManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val appContext: Context = context.applicationContext

    // Exporte tout vers l'URI SAF. Ne lève jamais : tout échec devient Result.failure.
    suspend fun exportBackup(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        // CancellationException doit se propager pour ne pas casser les coroutines.
        try {
            runExport(uri)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(IllegalStateException("Échec de l'export : ${e.message ?: "erreur inconnue"}", e))
        }
    }

    // Importe (upsert par id, les doublons sont remplacés). Ne lève jamais.
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
        val payload = BackupPayload(
            journals = journals.map { it.toBackup() },
            properties = properties.map { it.toBackup() },
            rows = rows.map { row ->
                row.toBackup(copyImagesForExport(row, resolver, images))
            }
        )

        val out = resolver.openOutputStream(uri)
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
        return Result.success(BackupInfo(journals.size, rows.size, images.size))
    }

    // Recopie les fichiers `file://` lisibles dans le zip et réécrit leurs URI
    // en références `images/<nom>` dans le values_json exporté.
    private fun copyImagesForExport(
        row: EntryRowEntity,
        resolver: ContentResolver,
        images: MutableList<Pair<String, ByteArray>>
    ): String {
        val listed = rowImagesFromJson(row.valuesJson)
        if (listed.isEmpty()) return row.valuesJson
        var changed = false
        val mapped = listed.mapIndexed { index, image ->
            val u = image.uri
            if (u != null && u.startsWith(FILE_SCHEME)) {
                val bytes = try {
                    resolver.openInputStream(Uri.parse(u))?.use { it.readBytes() }
                } catch (_: Exception) {
                    null
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val base = u.substringAfterLast('/').takeLast(40).ifBlank { "image" }
                    val name = sanitize("${row.id}_${index}_$base")
                    images += name to bytes
                    changed = true
                    image.copy(uri = IMAGES_DIR + name)
                } else {
                    image
                }
            } else {
                image
            }
        }
        return if (changed) writeImagesToValuesJson(row.valuesJson, mapped) else row.valuesJson
    }

    private suspend fun runImport(uri: Uri): Result<BackupInfo> {
        val resolver = appContext.contentResolver
        val input = resolver.openInputStream(uri)
            ?: return Result.failure(IllegalStateException("Impossible d'ouvrir le fichier de sauvegarde."))
        var jsonBytes: ByteArray? = null
        val imageEntries = HashMap<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    when {
                        entry.name == DATA_JSON -> jsonBytes = bytes
                        // Garde anti zip-slip : aucun ".." ni chemin absolu accepté.
                        entry.name.startsWith(IMAGES_DIR) && !entry.name.contains("..") ->
                            imageEntries[entry.name.removePrefix(IMAGES_DIR)] = bytes
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

        var restoredImages = 0
        val restoredRows = payload.rows.map { row ->
            val (valuesJson, count) = restoreImagesForImport(row, imageEntries)
            restoredImages += count
            EntryRowEntity(
                id = row.id,
                journalId = row.journalId,
                valuesJson = valuesJson,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                deletedAt = row.deletedAt,
                userId = row.userId
            )
        }
        db.withTransaction {
            payload.journals.forEach { db.journalDao().upsert(it.toEntity()) }
            payload.properties.forEach { db.propertyDao().upsert(it.toEntity()) }
            restoredRows.forEach { db.entryRowDao().upsert(it) }
        }
        return Result.success(BackupInfo(payload.journals.size, payload.rows.size, restoredImages))
    }

    // Recopie les images du zip vers le stockage interne et reconstruit des URI `file://`.
    // Retourne le values_json réécrit et le nombre d'images restaurées.
    private fun restoreImagesForImport(
        row: BackupRow,
        imageEntries: Map<String, ByteArray>
    ): Pair<String, Int> {
        val listed = rowImagesFromJson(row.valuesJson)
        if (listed.isEmpty()) return row.valuesJson to 0
        var count = 0
        val mapped = listed.map { image ->
            val ref = image.uri
            if (ref != null && ref.startsWith(IMAGES_DIR) && !ref.contains("..")) {
                val bytes = imageEntries[ref.removePrefix(IMAGES_DIR)]
                if (bytes == null) {
                    // Entrée absente : URI invalide purgée, remote/caption conservés.
                    image.copy(uri = null)
                } else {
                    try {
                        val dir = File(appContext.filesDir, "backup_images/${row.id}")
                        if (!dir.isDirectory) dir.mkdirs()
                        var safe = ref.substringAfterLast('/').ifBlank { "image.jpg" }
                        if (safe == "." || safe == "..") safe = "image.jpg"
                        val dest = File(dir, safe)
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
        }
        return writeImagesToValuesJson(row.valuesJson, mapped) to count
    }

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

    private fun BackupJournal.toEntity() = JournalEntity(id, name, createdAt, updatedAt, deletedAt, userId)

    private fun BackupProperty.toEntity() = PropertyEntity(
        id, journalId, name, kind, format, optionsJson, sortOrder, updatedAt, deletedAt, userId
    )

    companion object {
        const val DATA_JSON = "data.json"
        const val IMAGES_DIR = "images/"
        const val FILE_SCHEME = "file://"
    }
}
