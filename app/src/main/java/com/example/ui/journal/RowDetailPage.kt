package com.example.ui.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.PropertyEntity
import com.example.ui.theme.AppColors
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowDetailPage(
    row: EntryRowEntity,
    properties: List<PropertyEntity>,
    onUpdateCell: (rowId: String, propId: String, value: Any?) -> Unit,
    onDeleteRow: (rowId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val scrollState = rememberScrollState()

    val rowValues = remember(row.valuesJson) {
        try {
            JSONObject(row.valuesJson)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    val primaryProperty = properties.firstOrNull()
    val primaryValue = if (primaryProperty != null && rowValues.has(primaryProperty.id)) {
        rowValues.optString(primaryProperty.id, "")
    } else ""

    val notesValue = if (rowValues.has("_page_notes")) {
        rowValues.optString("_page_notes", "")
    } else ""

    val formattedCreatedDate = remember(row.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(row.createdAt))
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Panel,
                    titleContentColor = AppColors.TextPrimary,
                    navigationIconContentColor = AppColors.TextPrimary
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("row_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour au tableau"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AppColors.SelectedBg,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AppColors.AccentLight,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (primaryValue.isNotBlank()) primaryValue else "Élément sans titre",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onDeleteRow(row.id)
                            onClose()
                        },
                        modifier = Modifier.testTag("row_detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer la ligne",
                            tint = AppColors.ErrorRed
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Notion-like Big Page Title (linked to primary property)
            if (primaryProperty != null) {
                Text(
                    text = "${primaryProperty.name} (Colonne principale)",
                    fontSize = 11.sp,
                    color = AppColors.AccentLight,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = primaryValue,
                    onValueChange = { newValue ->
                        onUpdateCell(row.id, primaryProperty.id, newValue)
                    },
                    textStyle = TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(AppColors.Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("row_detail_title_input"),
                    decorationBox = { innerTextField ->
                        if (primaryValue.isEmpty()) {
                            Text(
                                text = "Titre de l'élément…",
                                color = AppColors.TextMuted,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Créé le $formattedCreatedDate",
                fontSize = 11.sp,
                color = AppColors.TextMuted
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AppColors.Border)
            Spacer(modifier = Modifier.height(16.dp))

            // Notion Properties List (Key-Value attributes)
            Text(
                text = "PROPRIÉTÉS DE L'ÉLÉMENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppColors.Panel,
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    properties.forEachIndexed { index, prop ->
                        val currentValue = if (rowValues.has(prop.id)) {
                            rowValues.optString(prop.id, "")
                        } else ""

                        val icon = when (prop.kind) {
                            "text" -> Icons.Default.TextFields
                            "number" -> Icons.Default.Numbers
                            "date" -> Icons.Default.CalendarToday
                            "select" -> Icons.Default.FormatListBulleted
                            else -> Icons.Default.Description
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left label with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(130.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = prop.name,
                                    fontSize = 13.sp,
                                    color = AppColors.TextSecondary,
                                    maxLines = 1
                                )
                            }

                            // Right editable value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(AppColors.Background, RoundedCornerShape(4.dp))
                                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                    .height(38.dp)
                            ) {
                                when (prop.kind) {
                                    "text" -> {
                                        TextCell(
                                            value = currentValue,
                                            onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                        )
                                    }
                                    "number" -> {
                                        NumberCell(
                                            value = currentValue,
                                            onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                        )
                                    }
                                    "date" -> {
                                        DateCell(
                                            value = currentValue,
                                            onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                        )
                                    }
                                    "select" -> {
                                        SelectCell(
                                            value = currentValue,
                                            optionsJson = prop.optionsJson,
                                            onValueChange = { onUpdateCell(row.id, prop.id, it) }
                                        )
                                    }
                                }
                            }
                        }

                        if (index < properties.lastIndex) {
                            HorizontalDivider(
                                color = AppColors.Border,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = AppColors.Border)
            Spacer(modifier = Modifier.height(16.dp))

            // Notion Page Body: Extended Notes & Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Page & Notes détaillées",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Détaillez ici les informations complémentaires, spécifications et notes libres.",
                fontSize = 12.sp,
                color = AppColors.TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notesValue,
                onValueChange = { newNotes ->
                    onUpdateCell(row.id, "_page_notes", newNotes)
                },
                placeholder = {
                    Text(
                        "Tapez vos notes libres ici…\n- Contexte et objectifs\n- Détails et avancement\n- Remarques et liens",
                        color = AppColors.TextMuted,
                        fontSize = 13.sp
                    )
                },
                minLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = AppColors.Border,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedContainerColor = AppColors.Panel,
                    unfocusedContainerColor = AppColors.Panel
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("row_detail_notes_input")
            )
        }
    }
}
