package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Regles du sous-format des colonnes numeriques.
 *
 * Test en JUnit pur : les regles de mise en forme et de validation ne dependent pas
 * d'Android, inutile de payer le cout de Robolectric.
 */
class PropertyFormatTest {

    // --- Mise en forme -------------------------------------------------------

    @Test
    fun `plain affiche le nombre nu`() {
        assertEquals("12.5", formatNumberValue(12.5, FORMAT_PLAIN))
        assertEquals("12", formatNumberValue(12.0, FORMAT_PLAIN))
    }

    @Test
    fun `percent ajoute le symbole apres la valeur`() {
        assertEquals("12.5 %", formatNumberValue(12.5, FORMAT_PERCENT))
    }

    @Test
    fun `dollar ajoute le symbole avant la valeur`() {
        assertEquals("$12.5", formatNumberValue(12.5, FORMAT_DOLLAR))
    }

    @Test
    fun `un format absent ou inconnu reste nu`() {
        // Les lignes synchronisees depuis une ancienne version portent format = null.
        assertEquals("7", formatNumberValue(7.0, null))
        assertEquals("7", formatNumberValue(7.0, "inconnu"))
    }

    @Test
    fun `les zeros inutiles sont retires`() {
        assertEquals("$3.25", formatNumberValue(3.25, FORMAT_DOLLAR))
        assertEquals("0 %", formatNumberValue(0.0, FORMAT_PERCENT))
    }

    // --- Lecture de la saisie ------------------------------------------------

    @Test
    fun `la saisie est acceptee avec ou sans symbole`() {
        assertEquals(12.5, parseNumberInput("12.5")!!.toDouble(), 0.0001)
        assertEquals(12.5, parseNumberInput("$12.5")!!.toDouble(), 0.0001)
        assertEquals(12.5, parseNumberInput("12.5 %")!!.toDouble(), 0.0001)
        assertEquals(12.5, parseNumberInput(" 12,5 ")!!.toDouble(), 0.0001)
    }

    @Test
    fun `une saisie non numérique ne produit pas de valeur`() {
        assertNull(parseNumberInput(""))
        assertNull(parseNumberInput("abc"))
        assertNull(parseNumberInput("$"))
        // Garde-fou : évite qu'un NaN ne pollue la somme du pied de tableau.
        assertNull(parseNumberInput("Infinity"))
    }

    // --- Validation de la cohérence kind / format ----------------------------

    @Test
    fun `un nombre sans format recupere plain`() {
        assertEquals(FORMAT_PLAIN, resolvePropertyFormat("number", null))
    }

    @Test
    fun `un nombre accepte les trois formats`() {
        assertEquals(FORMAT_PLAIN, resolvePropertyFormat("number", FORMAT_PLAIN))
        assertEquals(FORMAT_PERCENT, resolvePropertyFormat("number", FORMAT_PERCENT))
        assertEquals(FORMAT_DOLLAR, resolvePropertyFormat("number", FORMAT_DOLLAR))
    }

    @Test
    fun `un format sur un autre kind est refuse`() {
        listOf("text", "date", "select").forEach { kind ->
            assertThrows(IllegalArgumentException::class.java) {
                resolvePropertyFormat(kind, FORMAT_PERCENT)
            }
        }
    }

    @Test
    fun `un autre kind impose un format nul`() {
        assertNull(resolvePropertyFormat("select", null))
    }

    @Test
    fun `un format inconnu sur un nombre est refuse`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolvePropertyFormat("number", "euro")
        }
    }
}
