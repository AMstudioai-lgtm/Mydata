package com.example.ui.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FORMAT_DOLLAR
import com.example.data.FORMAT_PERCENT
import com.example.data.FORMAT_PLAIN
import com.example.data.db.entity.PropertyEntity
import com.example.ui.theme.AppColors
import org.json.JSONArray

@Composable
fun EditPropertyDialog(
    property: PropertyEntity,
    onDismiss: () -> Unit,
    onUpdateProperty: (propertyId: String, name: String, kind: String, options: List<String>, format: String?) -> Unit
) {
    val initialOptions = remember(property.optionsJson) {
        try {
            val arr = JSONArray(property.optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list.joinToString(", ")
        } catch (_: Exception) {
            ""
        }
    }

    var name by remember { mutableStateOf(property.name) }
    var selectedKind by remember { mutableStateOf(property.kind) }
    var optionsText by remember { mutableStateOf(initialOptions) }
    // Pré-remplissage : une colonne créée avant la migration porte un format nul, donc "plain".
    var selectedFormat by remember { mutableStateOf(property.format ?: FORMAT_PLAIN) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var formatDropdownExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val kindLabels = mapOf(
        "text" to "Texte",
        "number" to "Nombre",
        "date" to "Date",
        "select" to "Sélection"
    )

    val formatLabels = mapOf(
        FORMAT_PLAIN to "Nom libre",
        FORMAT_PERCENT to "Pourcentage",
        FORMAT_DOLLAR to "Dollars"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppColors.Panel,
            border = BorderStroke(1.dp, AppColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_property_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Modifier la colonne",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Nom de la colonne") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.Border,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedContainerColor = AppColors.Background,
                        unfocusedContainerColor = AppColors.Background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_property_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kind selector dropdown
                Text(
                    text = "Type de données",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                            .testTag("edit_property_type_selector"),
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Background,
                        border = BorderStroke(1.dp, AppColors.Border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = kindLabels[selectedKind] ?: "Texte",
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choisir",
                                tint = AppColors.TextSecondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(AppColors.Panel)
                            .testTag("edit_property_type_dropdown")
                    ) {
                        kindLabels.forEach { (kindKey, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = AppColors.TextPrimary) },
                                onClick = {
                                    selectedKind = kindKey
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sous-format des nombres, imbriqué sous le type ; masqué pour les autres types.
                if (selectedKind == "number") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Format",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { formatDropdownExpanded = true }
                                .testTag("edit_property_format_selector"),
                            shape = RoundedCornerShape(4.dp),
                            color = AppColors.Background,
                            border = BorderStroke(1.dp, AppColors.Border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatLabels[selectedFormat] ?: "Nom libre",
                                    color = AppColors.TextPrimary,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Choisir",
                                    tint = AppColors.TextSecondary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = formatDropdownExpanded,
                            onDismissRequest = { formatDropdownExpanded = false },
                            modifier = Modifier
                                .background(AppColors.Panel)
                                .testTag("edit_property_format_dropdown")
                        ) {
                            formatLabels.forEach { (formatKey, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = AppColors.TextPrimary) },
                                    onClick = {
                                        selectedFormat = formatKey
                                        formatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // If select kind, show options input
                if (selectedKind == "select") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = optionsText,
                        onValueChange = { optionsText = it },
                        label = { Text("Options (séparées par des virgules)") },
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Accent,
                            unfocusedBorderColor = AppColors.Border,
                            focusedTextColor = AppColors.TextPrimary,
                            unfocusedTextColor = AppColors.TextPrimary,
                            focusedContainerColor = AppColors.Background,
                            unfocusedContainerColor = AppColors.Background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_property_options_input")
                    )
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = AppColors.ErrorRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, AppColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.TextSecondary
                        )
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Veuillez entrer un nom de colonne"
                                return@Button
                            }
                            val parsedOptions = if (selectedKind == "select") {
                                optionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            } else {
                                emptyList()
                            }
                            if (selectedKind == "select" && parsedOptions.isEmpty()) {
                                errorText = "Veuillez ajouter au moins une option"
                                return@Button
                            }
                            // Le format ne voyage que sur une colonne numérique, sinon il reste nul.
                            val format = if (selectedKind == "number") selectedFormat else null
                            onUpdateProperty(property.id, name, selectedKind, parsedOptions, format)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.TextPrimary
                        ),
                        modifier = Modifier.testTag("edit_property_confirm_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}
