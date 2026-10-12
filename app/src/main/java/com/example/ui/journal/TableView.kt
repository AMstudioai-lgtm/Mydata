package com.example.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.formatNumberValue
import com.example.data.parseNumberInput
import com.example.ui.theme.AppColors
import org.json.JSONObject

@Composable
fun TableView(
    properties: List<PropertyEntity>,
    rows: List<EntryRowEntity>,
    onUpdateCell: (rowId: String, propId: String, value: Any?) -> Unit,
    onDeleteRow: (rowId: String) -> Unit,
    onDeleteProperty: (propId: String) -> Unit,
    onEditProperty: (PropertyEntity) -> Unit,
    onMoveProperty: (propId: String, delta: Int) -> Unit,
    onMovePropertyToFront: (propId: String) -> Unit,
    onMovePropertyToBack: (propId: String) -> Unit,
    onOpenRow: (EntryRowEntity) -> Unit,
    sortPropertyId: String? = null,
    sortAscending: Boolean? = null,
    onToggleSort: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Un seul état de scroll horizontal, partagé par l'en-tête, le corps et le pied :
    // les trois bandes restent alignées sur les mêmes colonnes pendant le défilement.
    val horizontalScrollState = rememberScrollState()

    // Scroll vertical du corps. Les listes de lignes sont longues : il doit être fluide.
    val verticalScrollState = rememberScrollState()

    val primaryColumnWidth = 160.dp
    val regularColumnWidth = 140.dp
    val actionColumnWidth = 52.dp

    // Choice between sum or average ("sum" vs "avg") - NEVER display both at the same time
    var calculationMode by remember { mutableStateOf("sum") }

    // Precalculate footer stats for number columns
    val numberStats = remember(properties, rows) {
        val statsMap = mutableMapOf<String, Pair<Double, Double>>() // propId -> (sum, avg)
        for (prop in properties) {
            if (prop.kind == "number") {
                var sum = 0.0
                var count = 0
                for (row in rows) {
                    val value = parseCellString(row.valuesJson, prop.id)
                    val num = parseNumberInput(value)
                    if (num != null) {
                        sum += num
                        count++
                    }
                }
                val avg = if (count > 0) sum / count else 0.0
                statsMap[prop.id] = Pair(sum, avg)
            }
        }
        statsMap
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
        color = AppColors.Background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sticky Header Row : ne défile jamais, reste visible en haut.
            TableHeaderRow(
                properties = properties,
                primaryColumnWidth = primaryColumnWidth,
                regularColumnWidth = regularColumnWidth,
                actionColumnWidth = actionColumnWidth,
                sortPropertyId = sortPropertyId,
                sortAscending = sortAscending,
                onToggleSort = onToggleSort,
                onDeleteProperty = onDeleteProperty,
                onEditProperty = onEditProperty,
                onMoveProperty = onMoveProperty,
                onMovePropertyToFront = onMovePropertyToFront,
                onMovePropertyToBack = onMovePropertyToBack,
                modifier = Modifier.horizontalScroll(horizontalScrollState)
            )

            // Body : occupe tout l'espace restant et défile verticalement.
            // Le scroll horizontal est partagé avec l'en-tête et le pied.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (rows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune ligne. Clique sur '+ Ligne' pour commencer.",
                            color = AppColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        rows.forEachIndexed { rowIndex, row ->
                            val rowValues = remember(row.valuesJson) {
                                try {
                                    JSONObject(row.valuesJson)
                                } catch (_: Exception) {
                                    JSONObject()
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .height(44.dp)
                                    .background(if (rowIndex % 2 == 0) AppColors.Background else AppColors.Panel.copy(alpha = 0.4f))
                                    .border(0.5.dp, AppColors.Border)
                            ) {
                                properties.forEachIndexed { colIndex, prop ->
                                    val isFirst = colIndex == 0
                                    val width = if (isFirst) primaryColumnWidth else regularColumnWidth
                                    val cellValue = if (rowValues.has(prop.id)) {
                                        rowValues.optString(prop.id, "")
                                    } else ""

                                    Box(
                                        modifier = Modifier
                                            .width(width)
                                            .height(44.dp)
                                            .border(0.5.dp, AppColors.Border)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                when (prop.kind) {
                                                    "text" -> {
                                                        TextCell(
                                                            value = cellValue,
                                                            onValueChange = { newValue ->
                                                                onUpdateCell(row.id, prop.id, newValue)
                                                            }
                                                        )
                                                    }
                                                    "number" -> {
                                                        NumberCell(
                                                            value = cellValue,
                                                            onValueChange = { newDouble ->
                                                                onUpdateCell(row.id, prop.id, newDouble)
                                                            },
                                                            format = prop.format
                                                        )
                                                    }
                                                    "date" -> {
                                                        DateCell(
                                                            value = cellValue,
                                                            onValueChange = { newDate ->
                                                                onUpdateCell(row.id, prop.id, newDate)
                                                            }
                                                        )
                                                    }
                                                    "select" -> {
                                                        SelectCell(
                                                            value = cellValue,
                                                            optionsJson = prop.optionsJson,
                                                            onValueChange = { newSelected ->
                                                                onUpdateCell(row.id, prop.id, newSelected)
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // Primary Column Mini Pencil Button to open row page
                                            if (isFirst) {
                                                IconButton(
                                                    onClick = { onOpenRow(row) },
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .padding(end = 4.dp)
                                                        .testTag("open_row_pencil_button_${rowIndex}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Ouvrir la page de l'élément",
                                                        tint = AppColors.Accent,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Action cell with delete row button
                                Box(
                                    modifier = Modifier
                                        .width(actionColumnWidth)
                                        .height(44.dp)
                                        .border(0.5.dp, AppColors.Border),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { onDeleteRow(row.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_row_button_${rowIndex}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer ligne",
                                            tint = AppColors.TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Footer Row (Summary for number columns) : visible en permanence en bas.
            // imePadding sur le seul conteneur du pied : le clavier ne masque jamais les totaux
            // sans pousser le reste de la page.
            Box(modifier = Modifier.imePadding()) {
                TableFooterRow(
                    properties = properties,
                    rows = rows,
                    calculationMode = calculationMode,
                    numberStats = numberStats,
                    primaryColumnWidth = primaryColumnWidth,
                    regularColumnWidth = regularColumnWidth,
                    actionColumnWidth = actionColumnWidth,
                    onToggleCalculation = {
                        calculationMode = if (calculationMode == "sum") "avg" else "sum"
                    },
                    modifier = Modifier.horizontalScroll(horizontalScrollState)
                )
            }
        }
    }
}

/** Colonne d'en-tête : nom, badge de type et menu de la colonne. Appui = tri. */
@Composable
private fun ColumnHeaderCell(
    prop: PropertyEntity,
    index: Int,
    isLast: Boolean,
    width: Dp,
    sortPropertyId: String?,
    sortAscending: Boolean?,
    onToggleSort: (String) -> Unit,
    onEditProperty: (PropertyEntity) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onMoveProperty: (String, Int) -> Unit,
    onMovePropertyToFront: (String) -> Unit,
    onMovePropertyToBack: (String) -> Unit
) {
    val isFirst = index == 0
    var menuExpanded by remember { mutableStateOf(false) }

    val kindDisplay = when (prop.kind) {
        "text" -> "txt"
        "number" -> "num"
        "date" -> "date"
        "select" -> "sel"
        else -> prop.kind
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(48.dp)
            .border(0.5.dp, AppColors.Border)
            .clickable { onToggleSort(prop.id) }
            .padding(horizontal = 6.dp)
            .testTag("column_header_$index"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFirst) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Colonne principale",
                    tint = AppColors.Accent,
                    modifier = Modifier
                        .size(13.dp)
                        .padding(end = 2.dp)
                )
            }

            Text(
                text = prop.name + when {
                    prop.id == sortPropertyId && sortAscending == true -> " ▲"
                    prop.id == sortPropertyId && sortAscending == false -> " ▼"
                    else -> ""
                },
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isFirst) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Kind indicator badge
            Surface(
                color = if (isFirst) AppColors.Accent.copy(alpha = 0.25f) else AppColors.SelectedBg.copy(alpha = 0.4f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    text = if (isFirst) "Principal" else kindDisplay,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.AccentLight,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Column action menu (reordering and deletion)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("column_menu_button_${index}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options de colonne",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(AppColors.Panel)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = AppColors.Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Modifier", color = AppColors.TextPrimary, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            onEditProperty(prop)
                            menuExpanded = false
                        }
                    )

                    HorizontalDivider(color = AppColors.Border)

                    if (!isFirst) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FirstPage,
                                        contentDescription = null,
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Passer tout devant", color = AppColors.TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                onMovePropertyToFront(prop.id)
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Déplacer à gauche", color = AppColors.TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                onMoveProperty(prop.id, -1)
                                menuExpanded = false
                            }
                        )
                    }

                    if (!isLast) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Déplacer à droite", color = AppColors.TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                onMoveProperty(prop.id, 1)
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LastPage,
                                        contentDescription = null,
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Passer tout derrière", color = AppColors.TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                onMovePropertyToBack(prop.id)
                                menuExpanded = false
                            }
                        )
                    }

                    HorizontalDivider(color = AppColors.Border)

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = AppColors.ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Supprimer la colonne", color = AppColors.ErrorRed, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            onDeleteProperty(prop.id)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

/** En-tête de colonnes. */
@Composable
private fun TableHeaderRow(
    properties: List<PropertyEntity>,
    primaryColumnWidth: Dp,
    regularColumnWidth: Dp,
    actionColumnWidth: Dp,
    sortPropertyId: String?,
    sortAscending: Boolean?,
    onToggleSort: (String) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onEditProperty: (PropertyEntity) -> Unit,
    onMoveProperty: (String, Int) -> Unit,
    onMovePropertyToFront: (String) -> Unit,
    onMovePropertyToBack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(AppColors.Panel)
            .height(48.dp)
            .border(1.dp, AppColors.Border)
    ) {
        properties.forEachIndexed { index, prop ->
            val isFirst = index == 0
            val width = if (isFirst) primaryColumnWidth else regularColumnWidth

            ColumnHeaderCell(
                prop = prop,
                index = index,
                isLast = index == properties.lastIndex,
                width = width,
                sortPropertyId = sortPropertyId,
                sortAscending = sortAscending,
                onToggleSort = onToggleSort,
                onEditProperty = onEditProperty,
                onDeleteProperty = onDeleteProperty,
                onMoveProperty = onMoveProperty,
                onMovePropertyToFront = onMovePropertyToFront,
                onMovePropertyToBack = onMovePropertyToBack
            )
        }

        // Header cell for action column
        Box(
            modifier = Modifier
                .width(actionColumnWidth)
                .height(48.dp)
                .border(0.5.dp, AppColors.Border),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#",
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Pied de tableau : somme ou moyenne des colonnes numériques + nombre de lignes. */
@Composable
private fun TableFooterRow(
    properties: List<PropertyEntity>,
    rows: List<EntryRowEntity>,
    calculationMode: String,
    numberStats: Map<String, Pair<Double, Double>>,
    primaryColumnWidth: Dp,
    regularColumnWidth: Dp,
    actionColumnWidth: Dp,
    onToggleCalculation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(AppColors.Panel)
            .height(44.dp)
            .border(1.dp, AppColors.Border),
        verticalAlignment = Alignment.CenterVertically
    ) {
        properties.forEachIndexed { colIndex, prop ->
            val isFirst = colIndex == 0
            val width = if (isFirst) primaryColumnWidth else regularColumnWidth

            Box(
                modifier = Modifier
                    .width(width)
                    .height(44.dp)
                    .border(0.5.dp, AppColors.Border)
                    .padding(horizontal = 8.dp)
                    .clickable {
                        // Toggle between sum and average by tapping the footer cell
                        onToggleCalculation()
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (prop.kind == "number") {
                    val stats = numberStats[prop.id] ?: Pair(0.0, 0.0)
                    // Le pied de tableau reprend le sous-format de la colonne.
                    val displayText = if (calculationMode == "sum") {
                        "Σ " + formatNumberValue(stats.first, prop.format)
                    } else {
                        "moy " + formatNumberValue(stats.second, prop.format)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayText,
                            color = AppColors.Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AppColors.SelectedBg.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (calculationMode == "sum") "somme" else "moy",
                                fontSize = 9.sp,
                                color = AppColors.AccentLight,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                } else if (isFirst) {
                    // First column footer indicator: shows toggle button between Somme and Moyenne
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
                        modifier = Modifier
                            .clickable { onToggleCalculation() }
                            .testTag("footer_calc_mode_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = AppColors.Accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (calculationMode == "sum") "Mode: Somme" else "Mode: Moyenne",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "—",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Footer cell for action column: Total row count
        Box(
            modifier = Modifier
                .width(actionColumnWidth)
                .height(44.dp)
                .border(0.5.dp, AppColors.Border),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${rows.size}",
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun parseCellString(valuesJson: String, propId: String): String? {
    return try {
        val obj = JSONObject(valuesJson)
        if (obj.has(propId)) obj.optString(propId) else null
    } catch (_: Exception) {
        null
    }
}

/** Filtre les lignes sur toutes les cellules en texte, insensible à la casse. */
fun filterTableRows(
    rows: List<EntryRowEntity>,
    query: String
): List<EntryRowEntity> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return rows
    return rows.filter { row ->
        try {
            val obj = JSONObject(row.valuesJson)
            obj.keys().asSequence().any { key ->
                obj.optString(key, "").lowercase().contains(q)
            }
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Trie les lignes : ordre de création si hors tri, numérique pour les colonnes
 * `number`, alphabétique (insensible à la casse) sinon.
 */
fun sortTableRows(
    rows: List<EntryRowEntity>,
    properties: List<PropertyEntity>,
    sortPropertyId: String?,
    sortAscending: Boolean?
): List<EntryRowEntity> {
    if (sortPropertyId == null || sortAscending == null) {
        return rows.sortedBy { it.createdAt }
    }
    val prop = properties.find { it.id == sortPropertyId }
        ?: return rows.sortedBy { it.createdAt }
    if (prop.kind == "number") {
        val comparator = compareBy<EntryRowEntity>(
            { parseNumberInput(parseCellString(it.valuesJson, prop.id)) == null },
            { parseNumberInput(parseCellString(it.valuesJson, prop.id)) ?: Double.MAX_VALUE }
        )
        return if (sortAscending) rows.sortedWith(comparator) else rows.sortedWith(comparator.reversed())
    }
    val comparator = compareBy<EntryRowEntity> { parseCellString(it.valuesJson, prop.id)?.lowercase() ?: "" }
    return if (sortAscending) rows.sortedWith(comparator) else rows.sortedWith(comparator.reversed())
}

/** Recherche puis tri : la liste réellement affichée par le tableau. */
fun visibleTableRows(
    rows: List<EntryRowEntity>,
    properties: List<PropertyEntity>,
    query: String,
    sortPropertyId: String?,
    sortAscending: Boolean?
): List<EntryRowEntity> {
    return sortTableRows(filterTableRows(rows, query), properties, sortPropertyId, sortAscending)
}
