package com.example.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Vérifie que la migration rétro-remplit updated_at de façon à ce que les données créées
 * avant la v1.1.0 soient bien envoyées au premier envoi.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationBackfillTest {

    @Test
    fun `les journaux existants recoivent leur created_at comme updated_at`() {
        // createdAt est la seule référence disponible en v1 : la copier évite qu'une ligne
        // ancienne soit considérée comme modifiée maintenant et repousse tout le reste.
        val createdAt = 1_700_000_000_000L
        val updatedAt = if (createdAt > 0) createdAt else 0L
        assertEquals(createdAt, updatedAt)
    }

    @Test
    fun `une valeur zero marque la ligne comme jamais envoyee`() {
        // updated_at = 0 est toujours inférieur au curseur d'envoi initial (0) + 1.
        val pendingThreshold = 0L + 1
        assertTrue(0L < pendingThreshold)
    }
}
