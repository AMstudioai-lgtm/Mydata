package com.example.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// JUnit pur : la règle corbeille (deleted_at non nul) ne dépend pas d'Android.
class TrashRulesTest {

    @Test
    fun `deleted_at nul signifie vivant`() {
        assertFalse(TrashRules.isTrashed(null))
    }

    @Test
    fun `deleted_at renseigne signifie en corbeille`() {
        assertTrue(TrashRules.isTrashed(1_700_000_000_000L))
    }

    @Test
    fun `deleted_at a zero reste en corbeille car non nul`() {
        // 0 est l'époque Unix, pas "absent" : seul null vaut vivant, comme en SQL (IS NOT NULL).
        assertTrue(TrashRules.isTrashed(0L))
    }

    @Test
    fun `la classification survit a un aller-retour backup`() {
        val payload = BackupPayload(
            journals = listOf(
                BackupJournal("vivant", "n", 1L, 2L, null, null),
                BackupJournal("jeté", "n", 1L, 2L, 3L, null)
            )
        )
        val back = BackupCodec.decode(BackupCodec.encode(payload))
        val trashed = back.journals.filter { TrashRules.isTrashed(it.deletedAt) }
        assertEquals(listOf("jeté"), trashed.map { it.id })
    }
}
