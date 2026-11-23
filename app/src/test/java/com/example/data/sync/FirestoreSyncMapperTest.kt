package com.example.data.sync

import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirestoreSyncMapperTest {

    @Test
    fun `journalToCreateMap excludes id and null deletedAt and sets server timestamps`() {
        val entity = JournalEntity(
            id = "j-1",
            name = "  Mon Journal  ",
            createdAt = 1000L,
            updatedAt = 2000L,
            deletedAt = null,
            userId = "uid-1"
        )

        val map = FirestoreSyncMapper.journalToCreateMap(entity, "uid-1")

        assertFalse("Must never write @DocumentId id into body", map.containsKey("id"))
        assertFalse("Must omit null deletedAt on create", map.containsKey("deletedAt"))
        assertEquals("uid-1", map["userId"])
        assertEquals("Mon Journal", map["name"])
        assertEquals(1000L, map["clientCreatedAt"])
        assertEquals(2000L, map["clientUpdatedAt"])
        assertEquals(FieldValue.serverTimestamp(), map["createdAt"])
        assertEquals(FieldValue.serverTimestamp(), map["updatedAt"])
    }

    @Test
    fun `journalToUpdateMap uses FieldValue delete when restored and never touches immutable keys`() {
        val restored = JournalEntity(
            id = "j-1",
            name = "Renommé",
            createdAt = 1000L,
            updatedAt = 3000L,
            deletedAt = null,
            userId = "uid-1"
        )

        val map = FirestoreSyncMapper.journalToUpdateMap(restored)

        assertFalse(map.containsKey("userId"))
        assertFalse(map.containsKey("createdAt"))
        assertFalse(map.containsKey("clientCreatedAt"))
        assertEquals("Renommé", map["name"])
        assertEquals(3000L, map["clientUpdatedAt"])
        assertEquals(FieldValue.delete(), map["deletedAt"])
        assertEquals(FieldValue.serverTimestamp(), map["updatedAt"])
    }

    @Test
    fun `propertyToCreateMap uses native options list and omits null format`() {
        val prop = PropertyEntity(
            id = "p-1",
            journalId = "j-1",
            name = "Statut",
            kind = "select",
            format = null,
            optionsJson = """["Ouvert","Fermé"]""",
            sortOrder = 2,
            updatedAt = 5000L,
            deletedAt = null,
            userId = "uid-1"
        )

        val map = FirestoreSyncMapper.propertyToCreateMap(prop, "uid-1")

        assertFalse(map.containsKey("id"))
        assertFalse(map.containsKey("format"))
        assertEquals(listOf("Ouvert", "Fermé"), map["options"])
        assertEquals("select", map["kind"])
        assertEquals(2, map["sortOrder"])
    }

    @Test
    fun `entryRowFromDoc preserves existing local photo URIs and encodes reserved Firestore keys`() {
        val local = EntryRowEntity(
            id = "r-1",
            journalId = "j-1",
            valuesJson = """{"p-1":"Avant","__notes__":"Note perso","__images__":[{"uri":"file:///data/user/0/photo.jpg","caption":"Ticket"}]}""",
            createdAt = 1000L,
            updatedAt = 1000L,
            userId = "uid-1"
        )

        val pushMap = FirestoreSyncMapper.entryRowToCreateMap(local, "uid-1")
        @Suppress("UNCHECKED_CAST")
        val pushedValues = pushMap["values"] as Map<String, Any>
        assertFalse("Must never push reserved __images__ key to Firestore", pushedValues.containsKey("__images__"))
        assertFalse("Must never push reserved __notes__ key to Firestore", pushedValues.containsKey("__notes__"))
        assertEquals("Note perso", pushedValues["mydata_reserved_notes"])
        assertTrue(pushedValues.containsKey("mydata_reserved_images"))

        val remoteDoc = FirestoreEntryRowDoc(
            id = "r-1",
            userId = "uid-1",
            journalId = "j-1",
            values = mapOf(
                "p-1" to "Après",
                "mydata_reserved_notes" to "Note mise à jour",
                "mydata_reserved_images" to listOf(mapOf("caption" to "Ticket"))
            ),
            clientCreatedAt = 1000L,
            clientUpdatedAt = 5000L
        )

        val merged = FirestoreSyncMapper.entryRowFromDoc(remoteDoc, existingLocal = local)

        assertTrue(merged.valuesJson.contains("Après"))
        assertTrue(merged.valuesJson.contains("Note mise à jour"))
        val restoredImages = com.example.data.images.rowImagesFromJson(merged.valuesJson)
        assertEquals(1, restoredImages.size)
        assertEquals("file:///data/user/0/photo.jpg", restoredImages.first().uri)
        assertEquals("Ticket", restoredImages.first().caption)
        assertNull(merged.deletedAt)
    }
}
