package com.example.ui.journal

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.ui.theme.AppColors

@Composable
fun JournalPage(
    selectedJournal: JournalEntity?,
    properties: List<PropertyEntity>,
    rows: List<EntryRowEntity>,
    hasAnyJournals: Boolean,
    onCreateJournal: () -> Unit,
    onDeleteJournal: (String) -> Unit,
    onAddProperty: (name: String, kind: String, options: List<String>, format: String?) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onUpdateProperty: (propertyId: String, name: String, kind: String, options: List<String>, format: String?) -> Unit,
    onAddRow: () -> Unit,
    onUpdateCell: (rowId: String, propId: String, value: Any?) -> Unit,
    onDeleteRow: (rowId: String) -> Unit,
    onMoveProperty: (propId: String, delta: Int) -> Unit,
    onMovePropertyToFront: (propId: String) -> Unit,
    onMovePropertyToBack: (propId: String) -> Unit,
    onOpenRow: (EntryRowEntity) -> Unit,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    sortPropertyId: String? = null,
    sortAscending: Boolean? = null,
    onToggleSort: (String) -> Unit = {},
    onRenameJournal: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showAddPropertyDialog by remember { mutableStateOf(false) }
    var propertyToEdit by remember { mutableStateOf<PropertyEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // État local de repli : Shell ne câble pas encore le ViewModel, donc la page
    // reste pilotable seule tout en notifiant le parent pour le câblage futur.
    var localQuery by remember(query) { mutableStateOf(query) }
    var localSortId by remember(sortPropertyId) { mutableStateOf(sortPropertyId) }
    var localSortAsc by remember(sortAscending) { mutableStateOf(sortAscending) }
    val effectiveQuery = localQuery
    val visibleRows = remember(rows, properties, effectiveQuery, localSortId, localSortAsc) {
        visibleTableRows(rows, properties, effectiveQuery, localSortId, localSortAsc)
    }

    if (!hasAnyJournals || selectedJournal == null) {
        // Empty state when no journals exist
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.Panel,
                    border = BorderStroke(1.dp, AppColors.Border),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aucune base de données",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Crée ta première base de données dynamique pour gérer et analyser toutes tes données.",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCreateJournal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Accent,
                        contentColor = AppColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_database_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Créer une database",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        // Database display with table and command bar
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            // Database Header Banner
            Surface(
                color = AppColors.Panel,
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AppColors.SelectedBg,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "DB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedJournal.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.size(32.dp).testTag("rename_journal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Renommer la database",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${visibleRows.size} lignes)",
                            fontSize = 12.sp,
                            color = AppColors.TextMuted
                        )
                    }

                    // Delete Database button
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(32.dp).testTag("delete_journal_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer la database",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Recherche plein-texte au-dessus du tableau.
            if (properties.isNotEmpty()) {
                OutlinedTextField(
                    value = effectiveQuery,
                    onValueChange = {
                        localQuery = it
                        onQueryChange(it)
                    },
                    placeholder = { Text("Rechercher…", color = AppColors.TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.Border,
                        cursorColor = AppColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("table_search_field")
                )
            }

            // Body: Empty properties OR TableView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (properties.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                            .background(AppColors.Panel.copy(alpha = 0.5f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewColumn,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Table vide",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ajoute ta première propriété pour configurer les colonnes de ton journal.",
                            fontSize = 13.sp,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddPropertyDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Accent,
                                    contentColor = AppColors.TextPrimary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("add_first_property_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ajouter propriété")
                            }
                        }
                    }
                } else {
                    TableView(
                        properties = properties,
                        rows = visibleRows,
                        onUpdateCell = onUpdateCell,
                        onDeleteRow = onDeleteRow,
                        onDeleteProperty = onDeleteProperty,
                        onEditProperty = { propertyToEdit = it },
                        onMoveProperty = onMoveProperty,
                        onMovePropertyToFront = onMovePropertyToFront,
                        onMovePropertyToBack = onMovePropertyToBack,
                        onOpenRow = onOpenRow,
                        sortPropertyId = localSortId,
                        sortAscending = localSortAsc,
                        onToggleSort = { propId ->
                            if (localSortId != propId) {
                                localSortId = propId
                                localSortAsc = true
                            } else when (localSortAsc) {
                                true -> localSortAsc = false
                                else -> {
                                    localSortId = null
                                    localSortAsc = null
                                }
                            }
                            onToggleSort(propId)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Command Bar
            Surface(
                color = AppColors.Panel,
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [Propriété] Outlined button
                    OutlinedButton(
                        onClick = { showAddPropertyDialog = true },
                        border = BorderStroke(1.dp, AppColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AppColors.Background,
                            contentColor = AppColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("add_property_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewColumn,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Propriété",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // [+ Ligne] Filled accent button
                    Button(
                        onClick = onAddRow,
                        enabled = properties.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.TextPrimary,
                            disabledContainerColor = AppColors.Panel,
                            disabledContentColor = AppColors.TextMuted
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("add_row_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ligne",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Add Property Dialog
    if (showAddPropertyDialog) {
        AddPropertyDialog(
            onDismiss = { showAddPropertyDialog = false },
            onAddProperty = onAddProperty
        )
    }

    // Edit Property Dialog
    if (propertyToEdit != null) {
        EditPropertyDialog(
            property = propertyToEdit!!,
            onDismiss = { propertyToEdit = null },
            onUpdateProperty = onUpdateProperty
        )
    }

    // Delete Journal Confirmation Dialog
    if (showDeleteConfirmDialog && selectedJournal != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = AppColors.Panel,
            title = {
                Text(
                    text = "Supprimer ${selectedJournal.name} ?",
                    color = AppColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Suppression définitive, avec toutes ses lignes et propriétés.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Annuler", color = AppColors.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteJournal(selectedJournal.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ErrorRed,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Text("Supprimer")
                }
            }
        )
    }

    // Rename Journal Dialog : nom vide refusé (bouton désactivé).
    if (showRenameDialog && selectedJournal != null) {
        var renameText by remember(selectedJournal.id) { mutableStateOf(selectedJournal.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = AppColors.Panel,
            title = {
                Text(
                    text = "Renommer la database",
                    color = AppColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.Border,
                        cursorColor = AppColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_journal_field")
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    Text("Annuler", color = AppColors.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameJournal(selectedJournal.id, renameText.trim())
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Accent,
                        contentColor = AppColors.TextPrimary,
                        disabledContainerColor = AppColors.Panel,
                        disabledContentColor = AppColors.TextMuted
                    ),
                    modifier = Modifier.testTag("rename_journal_confirm")
                ) {
                    Text("Renommer")
                }
            }
        )
    }
}
