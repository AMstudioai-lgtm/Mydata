package com.example.domain.logic

import com.example.domain.model.FORMAT_DOLLAR
import com.example.domain.model.IMAGES_KEY
import com.example.domain.model.LEGACY_NOTES_KEY
import com.example.domain.model.NOTES_KEY
import com.example.domain.model.TablePropertyData
import com.example.domain.model.TableRowData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableQueryCalculatorTest {

    private val titleProp = TablePropertyData("p_title", "Nom", "text", null, "[]")
    private val amountProp = TablePropertyData("p_amount", "Montant", "number", FORMAT_DOLLAR, "[]")
    private val statusProp = TablePropertyData("p_status", "Statut", "select", null, "[\"Gain\",\"Perte\"]")
    private val props = listOf(titleProp, amountProp, statusProp)

    private val rows = listOf(
        TableRowData("r1", """{"p_title":"Alpha","p_amount":"$10.5","p_status":"Gain"}""", 100L),
        TableRowData("r2", """{"p_title":"Beta","p_amount":"-2,5","p_status":"Perte"}""", 200L),
        TableRowData("r3", """{"p_title":"Gamma","p_amount":"","p_status":"Gain"}""", 300L)
    )

    @Test
    fun `filterRows filtre sans tenir compte de la casse`() {
        val filtered = TableQueryCalculator.filterRows(rows, "beta")
        assertEquals(listOf("r2"), filtered.map { it.id })
    }

    @Test
    fun `sortRows trie numeriquement en placant les vides en dernier`() {
        val asc = TableQueryCalculator.sortRows(rows, props, "p_amount", sortAscending = true)
        assertEquals(listOf("r2", "r1", "r3"), asc.map { it.id })

        val desc = TableQueryCalculator.sortRows(rows, props, "p_amount", sortAscending = false)
        assertEquals(listOf("r3", "r1", "r2"), desc.map { it.id })
    }

    @Test
    fun `sortRows trie alphabetiquement sur une colonne texte`() {
        val desc = TableQueryCalculator.sortRows(rows, props, "p_title", sortAscending = false)
        assertEquals(listOf("r3", "r2", "r1"), desc.map { it.id })
    }

    @Test
    fun `computeNumberStats calcule somme et moyenne en ignorant les vides`() {
        val stats = TableQueryCalculator.computeNumberStats(props, rows)
        val amountStats = stats.getValue("p_amount")
        assertEquals(8.0, amountStats.sum, 0.0001)
        assertEquals(4.0, amountStats.avg, 0.0001)
    }

    @Test
    fun `parseSelectOptions extrait les options du tableau json`() {
        assertEquals(listOf("Gain", "Perte"), TableQueryCalculator.parseSelectOptions(statusProp.optionsJson))
        assertEquals(emptyList<String>(), TableQueryCalculator.parseSelectOptions("invalid"))
    }

    @Test
    fun `readNotes privilégie la cle actuelle puis retombe sur lancienne cle`() {
        assertEquals(
            "Nouvelle note",
            TableQueryCalculator.readNotes("""{"$NOTES_KEY":"Nouvelle note","$LEGACY_NOTES_KEY":"Ancienne"}""")
        )
        assertEquals(
            "Ancienne note",
            TableQueryCalculator.readNotes("""{"$LEGACY_NOTES_KEY":"Ancienne note"}""")
        )
    }

    @Test
    fun `rowPreview exclut la cle images et resume le contenu`() {
        val preview = TableQueryCalculator.rowPreview(
            """{"p_title":"Trade 1","$IMAGES_KEY":[{"uri":"file://x.jpg"}],"p_amount":"42"}"""
        )
        assertTrue(preview.contains("Trade 1"))
        assertTrue(preview.contains("42"))
        assertFalse(preview.contains("x.jpg"))
    }
}
