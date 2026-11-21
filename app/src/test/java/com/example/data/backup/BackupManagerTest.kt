package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.images.rowImagesFromJson
import com.example.data.images.writeImagesToValuesJson
import com.example.data.session.SessionProvider
import com.example.domain.model.RowImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private var activeUserId: String? = "user-A"

    private val sessionProvider = object : SessionProvider {
        override fun currentUserId(): String? = activeUserId
        override fun currentEmail(): String? = activeUserId?.let { "$it@example.com" }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `export puis reimport restaure tables proprietes lignes et photos dans la session active`() = runBlocking {
        val sourceImage = File(context.cacheDir, "photo_test_export.jpg")
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        sourceImage.writeBytes(imageBytes)

        val journal = JournalEntity(
            id = "j-1",
            name = "Portefeuille",
            createdAt = 1_000L,
            updatedAt = 2_000L,
            deletedAt = null,
            userId = "user-A"
        )
        val prop = PropertyEntity(
            id = "p-1",
            journalId = "j-1",
            name = "Titre",
            kind = "text",
            format = null,
            optionsJson = "[]",
            sortOrder = 0,
            updatedAt = 2_000L,
            deletedAt = null,
            userId = "user-A"
        )
        val valuesWithImage = writeImagesToValuesJson(
            """{"p-1":"Entrée avec photo"}""",
            listOf(RowImage(uri = Uri.fromFile(sourceImage).toString(), caption = "Capture 1"))
        )
        val row = EntryRowEntity(
            id = "r-1",
            journalId = "j-1",
            valuesJson = valuesWithImage,
            createdAt = 1_500L,
            updatedAt = 2_000L,
            deletedAt = null,
            userId = "user-A"
        )

        db.journalDao().upsert(journal)
        db.propertyDao().upsert(prop)
        db.entryRowDao().upsert(row)

        val manager = BackupManager(context, db, sessionProvider)
        val zipFile = File(context.cacheDir, "backup_roundtrip.zip")
        val exportResult = manager.exportBackup(Uri.fromFile(zipFile))
        assertTrue(exportResult.isSuccess)
        val exportInfo = exportResult.getOrThrow()
        assertEquals(1, exportInfo.journals)
        assertEquals(1, exportInfo.rows)
        assertEquals(1, exportInfo.images)

        // Simule une réinstallation + changement vers session locale ou un autre utilisateur.
        db.clearAllTables()
        sourceImage.delete()
        activeUserId = "user-B"

        val importResult = manager.importBackup(Uri.fromFile(zipFile))
        assertTrue(importResult.isSuccess)
        val importInfo = importResult.getOrThrow()
        assertEquals(1, importInfo.journals)
        assertEquals(1, importInfo.rows)
        assertEquals(1, importInfo.images)
        assertEquals("j-1", importInfo.firstActiveJournalId)

        // Vérifie que les tables, propriétés et lignes sont immédiatement visibles pour user-B.
        val visibleJournals = db.journalDao().watchAllForUser("user-B").first()
        assertEquals(1, visibleJournals.size)
        assertEquals("Portefeuille", visibleJournals.single().name)
        assertEquals("user-B", visibleJournals.single().userId)

        val visibleProps = db.propertyDao().watchByJournalForUser("j-1", "user-B").first()
        assertEquals(1, visibleProps.size)
        assertEquals("Titre", visibleProps.single().name)

        val visibleRows = db.entryRowDao().watchByJournalForUser("j-1", "user-B").first()
        assertEquals(1, visibleRows.size)

        val restoredImages = rowImagesFromJson(visibleRows.single().valuesJson)
        assertEquals(1, restoredImages.size)
        assertEquals("Capture 1", restoredImages.single().caption)
        val restoredUri = Uri.parse(restoredImages.single().uri)
        val restoredFile = File(restoredUri.path!!)
        assertNotNull(restoredFile)
        assertTrue(restoredFile.exists())
        assertArrayEquals(imageBytes, restoredFile.readBytes())
    }

    @Test
    fun `reimport hors-ligne rattache les donnees au mode local null sans ecraser la table existante`() = runBlocking {
        val existingJournal = JournalEntity(
            id = "j-existing",
            name = "Table Locale",
            createdAt = 500L,
            updatedAt = 500L,
            deletedAt = null,
            userId = null
        )
        val existingProp = PropertyEntity(
            id = "p-existing",
            journalId = "j-existing",
            name = "Note",
            kind = "text",
            sortOrder = 0,
            updatedAt = 500L,
            userId = null
        )
        db.journalDao().upsert(existingJournal)
        db.propertyDao().upsert(existingProp)

        val exportedJournal = JournalEntity(
            id = "j-imported",
            name = "Table Archive",
            createdAt = 600L,
            updatedAt = 600L,
            deletedAt = null,
            userId = "former-account"
        )
        db.journalDao().upsert(exportedJournal)
        val manager = BackupManager(context, db, sessionProvider)
        val zipFile = File(context.cacheDir, "backup_offline_merge.zip")
        assertTrue(manager.exportBackup(Uri.fromFile(zipFile)).isSuccess)

        db.journalDao().hardDeleteById("j-imported")
        activeUserId = null

        val importResult = manager.importBackup(Uri.fromFile(zipFile))
        assertTrue(importResult.isSuccess)

        val offlineJournals = db.journalDao().watchAllForUser(null).first()
        assertEquals(setOf("j-existing", "j-imported"), offlineJournals.map { it.id }.toSet())
        val existingPropsAfter = db.propertyDao().watchByJournalForUser("j-existing", null).first()
        assertEquals(1, existingPropsAfter.size)
    }
}
