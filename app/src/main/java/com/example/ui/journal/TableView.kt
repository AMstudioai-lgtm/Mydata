package com.example.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.db.toDomain
import com.example.domain.logic.TableQueryCalculator
import com.example.domain.model.CalculationMode
import com.example.ui.theme.AppColors

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
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val primaryColumnWidth = 160.dp
    val regularColumnWidth = 140.dp
    val actionColumnWidth = 52.dp

    var calculationMode by remember { mutableStateOf(CalculationMode.SUM) }

    val numberStats = remember(properties, rows) {
        TableQueryCalculator.computeNumberStats(
            properties = properties.map { it.toDomain() },
            rows = rows.map { it.toDomain() }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
        color = AppColors.Background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                                TableQueryCalculator.parseCellMap(row.valuesJson)
                            }

                            Row(
                                modifier = Modifier
                                    .height(44.dp)
                                    .background(if (rowIndex % 2 == 0) AppColors.Background else AppColors.Panel)
                                    .border(0.5.dp, AppColors.Border)
                            ) {
                                properties.forEachIndexed { colIndex, prop ->
                                    val isFirst = colIndex == 0
                                    val width = if (isFirst) primaryColumnWidth else regularColumnWidth
                                    val cellValue = rowValues[prop.id].orEmpty()

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
                                                    "text" -> TextCell(
                                                        value = cellValue,
                                                        onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                                    )
                                                    "number" -> NumberCell(
                                                        value = cellValue,
                                                        onValueChange = { onUpdateCell(row.id, prop.id, it) },
                                                        format = prop.format
                                                    )
                                                    "date" -> DateCell(
                                                        value = cellValue,
                                                        onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                                    )
                                                    "select" -> SelectCell(
                                                        value = cellValue,
                                                        optionsJson = prop.optionsJson,
                                                        onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                                    )
                                                }
                                            }

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

            Box(modifier = Modifier.imePadding()) {
                TableFooterRow(
                    properties = properties,
                    rows = rows,
                    calculationMode = calculationMode,
                    numberStats = numberStats,
                    primaryColumnWidth = primaryColumnWidth,
                    regularColumnWidth = regularColumnWidth,
                    actionColumnWidth = actionColumnWidth,
                    onToggleCalculation = { calculationMode = calculationMode.toggle() },
                    modifier = Modifier.horizontalScroll(horizontalScrollState)
                )
            }
        }
    }
}
