package com.example.domain.logic

import com.example.domain.model.ColumnNumberStats
import com.example.domain.model.IMAGES_KEY
import com.example.domain.model.LEGACY_NOTES_KEY
import com.example.domain.model.NOTES_KEY
import com.example.domain.model.TablePropertyData
import com.example.domain.model.TableRowData

/**
 * Calculs métier purs sur les données de tableau : filtrage, tri,
 * agrégations numériques (somme/moyenne) et lecture des cellules.
 */
object TableQueryCalculator {

    fun parseCellMap(valuesJson: String): Map<String, String?> =
        RowValuesJsonReader.parseObjectMap(valuesJson)

    fun parseCellString(valuesJson: String, propId: String): String? =
        parseCellMap(valuesJson)[propId]

    fun parseSelectOptions(optionsJson: String): List<String> =
        RowValuesJsonReader.parseStringArray(optionsJson)

    fun filterRows(
        rows: List<TableRowData>,
        query: String
    ): List<TableRowData> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return rows
        return rows.filter { row ->
            parseCellMap(row.valuesJson).values.any { value ->
                value?.lowercase()?.contains(q) == true
            }
        }
    }

    fun sortRows(
        rows: List<TableRowData>,
        properties: List<TablePropertyData>,
        sortPropertyId: String?,
        sortAscending: Boolean?
    ): List<TableRowData> {
        if (sortPropertyId == null || sortAscending == null) {
            return rows.sortedBy { it.createdAt }
        }
        val prop = properties.find { it.id == sortPropertyId }
            ?: return rows.sortedBy { it.createdAt }

        if (prop.kind == "number") {
            val comparator = compareBy<TableRowData>(
                { parseNumberInput(parseCellString(it.valuesJson, prop.id)) == null },
                { parseNumberInput(parseCellString(it.valuesJson, prop.id)) ?: Double.MAX_VALUE }
            )
            return if (sortAscending) rows.sortedWith(comparator) else rows.sortedWith(comparator.reversed())
        }

        val comparator = compareBy<TableRowData> {
            parseCellString(it.valuesJson, prop.id)?.lowercase() ?: ""
        }
        return if (sortAscending) rows.sortedWith(comparator) else rows.sortedWith(comparator.reversed())
    }

    fun visibleRows(
        rows: List<TableRowData>,
        properties: List<TablePropertyData>,
        query: String,
        sortPropertyId: String?,
        sortAscending: Boolean?
    ): List<TableRowData> {
        val filtered = filterRows(rows, query)
        return sortRows(filtered, properties, sortPropertyId, sortAscending)
    }

    fun computeNumberStats(
        properties: List<TablePropertyData>,
        rows: List<TableRowData>
    ): Map<String, ColumnNumberStats> {
        val statsMap = LinkedHashMap<String, ColumnNumberStats>()
        for (prop in properties) {
            if (prop.kind != "number") continue
            var sum = 0.0
            var count = 0
            for (row in rows) {
                val raw = parseCellString(row.valuesJson, prop.id)
                val num = parseNumberInput(raw)
                if (num != null) {
                    sum += num
                    count++
                }
            }
            val avg = if (count > 0) sum / count else 0.0
            statsMap[prop.id] = ColumnNumberStats(sum = sum, avg = avg)
        }
        return statsMap
    }

    fun readNotes(valuesJson: String): String {
        val map = parseCellMap(valuesJson)
        return when {
            map.containsKey(NOTES_KEY) -> map[NOTES_KEY].orEmpty()
            else -> map[LEGACY_NOTES_KEY].orEmpty()
        }
    }

    fun rowPreview(valuesJson: String): String {
        val map = parseCellMap(valuesJson)
        return map.entries
            .filter { (key, value) -> key != IMAGES_KEY && !value.isNullOrBlank() }
            .mapNotNull { it.value }
            .joinToString(" • ")
            .take(120)
            .ifBlank { "Ligne sans contenu" }
    }
}
