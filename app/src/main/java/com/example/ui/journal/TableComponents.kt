package com.example.ui.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FirstPage
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
import com.example.domain.logic.formatNumberValue
import com.example.domain.model.CalculationMode
import com.example.domain.model.ColumnNumberStats
import com.example.ui.theme.AppColors
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
internal fun TableHeaderRow(
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
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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
            .padding(horizontal = 8.dp)
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

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = if (isFirst) "PRI" else kindDisplay.uppercase(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (isFirst) AppColors.AccentLight else AppColors.TextMuted
            )

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
                                        imageVector = Icons.AutoMirrored.Filled.LastPage,
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

@Composable
internal fun TableFooterRow(
    properties: List<PropertyEntity>,
    rows: List<EntryRowEntity>,
    calculationMode: CalculationMode,
    numberStats: Map<String, ColumnNumberStats>,
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
                    .clickable(onClick = onToggleCalculation),
                contentAlignment = Alignment.CenterStart
            ) {
                if (prop.kind == "number") {
                    val stats = numberStats[prop.id] ?: ColumnNumberStats()
                    val displayText = if (calculationMode == CalculationMode.SUM) {
                        "Σ " + formatNumberValue(stats.sum, prop.format)
                    } else {
                        "moy " + formatNumberValue(stats.avg, prop.format)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayText,
                            color = AppColors.Accent,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (calculationMode == CalculationMode.SUM) "SUM" else "AVG",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp,
                            color = AppColors.TextMuted
                        )
                    }
                } else if (isFirst) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Background,
                        border = BorderStroke(1.dp, AppColors.Border),
                        modifier = Modifier
                            .clickable(onClick = onToggleCalculation)
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
                                text = if (calculationMode == CalculationMode.SUM) "Mode: Somme" else "Mode: Moyenne",
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

        Box(
            modifier = Modifier
                .width(actionColumnWidth)
                .height(44.dp)
                .border(0.5.dp, AppColors.Border),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${rows.size}",
                color = AppColors.TextSecondary,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
