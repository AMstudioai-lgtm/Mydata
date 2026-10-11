package com.example.ui.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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

    val imageUris = remember(row.valuesJson) { readImageUris(row.valuesJson) }
    val notesValue = remember(row.valuesJson) { readNotes(row.valuesJson) }

    // Titre : buffer local comme dans les cellules du tableau. Le champ est réécrit en base
    // à chaque frappe (le tableau doit suivre en direct), donc la value reviendrait à
    // TextRange(0) et la première lettre se collerait à droite du curseur sans ce buffer.
    var titleBuffer by remember {
        mutableStateOf(TextFieldValue(primaryValue, TextRange(primaryValue.length)))
    }
    var isTitleFocused by remember { mutableStateOf(false) }
    if (!isTitleFocused && titleBuffer.text != primaryValue) {
        titleBuffer = TextFieldValue(primaryValue, TextRange(primaryValue.length))
    }

    // Migration de l'ancien bloc de notes : dès que la zone libre est modifiée, la note
    // héritée est recopiée sous la clé réservée et l'ancienne clé est retirée.
    var legacyNotesCleared by remember(row.valuesJson) {
        mutableStateOf(!rowValues.has(LEGACY_NOTES_KEY))
    }

    var notesBuffer by remember {
        mutableStateOf(TextFieldValue(notesValue, TextRange(notesValue.length)))
    }
    var isNotesFocused by remember { mutableStateOf(false) }
    if (!isNotesFocused && notesBuffer.text != notesValue) {
        notesBuffer = TextFieldValue(notesValue, TextRange(notesValue.length))
    }

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
            // Big Page Title (linked to primary property)
            if (primaryProperty != null) {
                Text(
                    text = "${primaryProperty.name} (Colonne principale)",
                    fontSize = 11.sp,
                    color = AppColors.AccentLight,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = titleBuffer,
                    onValueChange = { newValue ->
                        titleBuffer = newValue
                        onUpdateCell(row.id, primaryProperty.id, newValue.text)
                    },
                    textStyle = TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(AppColors.Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isTitleFocused = it.isFocused }
                        .testTag("row_detail_title_input"),
                    decorationBox = { innerTextField ->
                        if (titleBuffer.text.isEmpty()) {
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

            // Properties List (Key-Value attributes)
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
                                            onValueChange = { onUpdateCell(row.id, prop.id, it) },
                                            format = prop.format
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

            // Images : juste après les propriétés, même logique d'enchaînement que Notion.
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = AppColors.Border)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Images",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (imageUris.isEmpty()) {
                        "aucune image"
                    } else {
                        "${imageUris.size}" + (if (imageUris.size > 1) " images" else " image")
                    },
                    fontSize = 11.sp,
                    color = AppColors.TextMuted
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            RowImagesSection(
                images = imageUris,
                onImagesChanged = { updated -> onUpdateCell(row.id, IMAGES_KEY, imageUrisJson(updated)) },
                modifier = Modifier.testTag("row_detail_images_section")
            )

            // Zone de texte libre : remplace l'ancien bloc de notes dédié. C'est l'endroit
            // vide situé juste après les propriétés, dans le prolongement des images.
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = AppColors.Border)
            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    tint = AppColors.AccentLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Espace libre",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))

            BasicTextField(
                value = notesBuffer,
                onValueChange = { newValue ->
                    notesBuffer = newValue
                    if (!legacyNotesCleared) {
                        legacyNotesCleared = true
                        onUpdateCell(row.id, LEGACY_NOTES_KEY, null)
                    }
                    onUpdateCell(row.id, NOTES_KEY, newValue.text)
                },
                textStyle = TextStyle(
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                ),
                cursorBrush = SolidColor(AppColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .onFocusChanged { isNotesFocused = it.isFocused }
                    .testTag("row_notes_input"),
                decorationBox = { innerTextField ->
                    if (notesBuffer.text.isEmpty()) {
                        Text(
                            text = "Clique ici pour écrire librement : contexte, objectifs, " +
                                "détails, remarques…",
                            color = AppColors.TextMuted,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
