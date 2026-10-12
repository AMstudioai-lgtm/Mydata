package com.example.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// JUnit pur : BackupCodec ne dépend ni d'Android ni de Room.
class BackupCodecTest {

    @Test
    fun `payload vide fait un aller-retour intact`() {
        val payload = BackupPayload()
        assertEquals(payload, BackupCodec.decode(BackupCodec.encode(payload)))
    }

    @Test
    fun `aller-retour complet preserve tous les champs`() {
        val payload = BackupPayload(
            journals = listOf(
                BackupJournal("j1", "Journal", 1_700_000_000_000L, 1_700_000_000_100L, null, "user-1"),
                BackupJournal("j2", "Corbeille", 100L, 200L, 300L, null)
            ),
            properties = listOf(
                BackupProperty("p1", "j1", "Montant", "number", "dollar", "[\"a\"]", 3, 400L, null, "user-1"),
                BackupProperty("p2", "j1", "Sans format", "text", null, "[]", 0, 500L, 600L, null)
            ),
            rows = listOf(
                BackupRow("r1", "j1", "{\"p1\":12.5}", 700L, 800L, null, "user-1"),
                BackupRow("r2", "j2", "{}", 0L, 0L, 900L, null)
            )
        )
        assertEquals(payload, BackupCodec.decode(BackupCodec.encode(payload)))
    }

    @Test
    fun `les caracteres speciaux survivent a laller-retour`() {
        // Guillemets, anti-slash, retours ligne et unicode : cas réel des notes utilisateur.
        val tricky = "Nom \"compliqué\" \\ avec\nretour\tet emoji \uD83D\uDE00"
        val nested = "{\"__images__\":[{\"uri\":\"images/a_b.jpg\",\"caption\":\"légende \\\"x\\\"\"}]}"
        val payload = BackupPayload(
            journals = listOf(BackupJournal("j", tricky, 1L, 2L, null, null)),
            rows = listOf(BackupRow("r", "j", nested, 1L, 2L, null, null))
        )
        val back = BackupCodec.decode(BackupCodec.encode(payload))
        assertEquals(tricky, back.journals.single().name)
        assertEquals(nested, back.rows.single().valuesJson)
    }

    @Test
    fun `les nuls restent nuls apres aller-retour`() {
        val payload = BackupPayload(
            journals = listOf(BackupJournal("j", "n", 1L, 2L, null, null)),
            properties = listOf(BackupProperty("p", "j", "n", "text", null, "[]", 0, 1L, null, null)),
            rows = listOf(BackupRow("r", "j", "{}", 1L, 2L, null, null))
        )
        val back = BackupCodec.decode(BackupCodec.encode(payload))
        assertNull(back.journals.single().deletedAt)
        assertNull(back.journals.single().userId)
        assertNull(back.properties.single().format)
        assertNull(back.rows.single().deletedAt)
    }

    @Test
    fun `un document vide ou tronque est refuse`() {
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("{\"version\":1,\"journals\":[") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("pas du json") }
    }

    @Test
    fun `une version inconnue est refusee`() {
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode("{\"version\":99,\"journals\":[],\"properties\":[],\"rows\":[]}")
        }
    }

    @Test
    fun `un champ manquant ou mal type est refuse`() {
        // Champ "name" absent sur le journal.
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(
                "{\"version\":1,\"journals\":[{\"id\":\"j\"}],\"properties\":[],\"rows\":[]}"
            )
        }
        // updated_at en chaîne au lieu d'entier.
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(
                "{\"version\":1,\"journals\":[{\"id\":\"j\",\"name\":\"n\",\"created_at\":1,\"updated_at\":\"x\"," +
                    "\"deleted_at\":null,\"user_id\":null}],\"properties\":[],\"rows\":[]}"
            )
        }
    }

    @Test
    fun `le json produit reste lisible comme texte`() {
        val json = BackupCodec.encode(BackupPayload(journals = listOf(BackupJournal("j", "n", 1L, 2L, null, null))))
        assertTrue(json.startsWith("{\"version\":1,"))
        assertTrue(json.contains("\"journals\":["))
    }
}
