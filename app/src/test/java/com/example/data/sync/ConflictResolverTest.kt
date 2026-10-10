package com.example.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La règle de conflit est ce qui garantit qu'une modification faite hors-ligne n'est pas
 * silencieusement écrasée par une donnée distante plus ancienne.
 */
class ConflictResolverTest {

    @Test
    fun `ligne distante plus recente gagne`() {
        assertTrue(ConflictResolver.remoteWins(localUpdatedAt = 1_000L, remoteUpdatedAt = 2_000L))
    }

    @Test
    fun `horodatages egaux conservent la ligne locale`() {
        // Cas réel d'une reprise de synchro : l'upsert rejoué ne doit rien écraser.
        assertFalse(ConflictResolver.remoteWins(localUpdatedAt = 5_000L, remoteUpdatedAt = 5_000L))
    }

    @Test
    fun `ligne distante plus ancienne perd`() {
        assertFalse(ConflictResolver.remoteWins(localUpdatedAt = 9_000L, remoteUpdatedAt = 3_000L))
    }

    @Test
    fun `ligne locale jamais envoyee perd face au distant`() {
        // updated_at a 0 signifie "données hors-ligne existantes, jamais poussées".
        assertTrue(ConflictResolver.remoteWins(localUpdatedAt = 0L, remoteUpdatedAt = 1L))
    }
}
