package com.example.ui.trash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.ui.theme.AppColors
import org.json.JSONObject

@Composable fun TrashPage(
  trashedJournals: List<JournalEntity>,
  allJournals: List<JournalEntity>,
  trashedRows: List<EntryRowEntity>,
  onRestoreJournal: (String) -> Unit,
  onPurgeJournal: (String) -> Unit,
  onRestoreRow: (String) -> Unit,
  onPurgeRow: (String) -> Unit,
  modifier: Modifier = Modifier
) {
    var journalToPurge by remember { mutableStateOf<JournalEntity?>(null) }
    var rowToPurge by remember { mutableStateOf<EntryRowEntity?>(null) }

    if (trashedJournals.isEmpty() && trashedRows.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background)
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
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Corbeille vide",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Les tables et lignes supprimées apparaîtront ici.",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section : tables supprimées.
        Text(
            text = "Tables supprimées (${trashedJournals.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        if (trashedJournals.isEmpty()) {
            Text(
                text = "Aucune table supprimée.",
                fontSize = 13.sp,
                color = AppColors.TextMuted
            )
        } else {
            trashedJournals.forEachIndexed { index, journal ->
                Surface(
                    color = AppColors.Panel,
                    border = BorderStroke(1.dp, AppColors.Border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trashed_journal_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = journal.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onRestoreJournal(journal.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Accent,
                                contentColor = AppColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("restore_journal_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restaurer", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { journalToPurge = journal },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.ErrorRed,
                                contentColor = AppColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("purge_journal_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer définitivement", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section : lignes supprimées.
        Text(
            text = "Lignes supprimées (${trashedRows.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        if (trashedRows.isEmpty()) {
            Text(
                text = "Aucune ligne supprimée.",
                fontSize = 13.sp,
                color = AppColors.TextMuted
            )
        } else {
            trashedRows.forEachIndexed { index, row ->
                val tableName = allJournals.find { it.id == row.journalId }?.name ?: "Table inconnue"
                Surface(
                    color = AppColors.Panel,
                    border = BorderStroke(1.dp, AppColors.Border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trashed_row_$index")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = tableName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.AccentLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rowPreview(row.valuesJson),
                            fontSize = 13.sp,
                            color = AppColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = { onRestoreRow(row.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Accent,
                                    contentColor = AppColors.TextPrimary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("restore_row_button_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestoreFromTrash,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restaurer", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { rowToPurge = row },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.ErrorRed,
                                    contentColor = AppColors.TextPrimary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("purge_row_button_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Supprimer définitivement", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (journalToPurge != null) {
        AlertDialog(
            onDismissRequest = { journalToPurge = null },
            containerColor = AppColors.Panel,
            title = {
                Text(
                    text = "Supprimer définitivement « ${journalToPurge!!.name} » ?",
                    color = AppColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Cette action est irréversible, avec toutes ses lignes et propriétés.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { journalToPurge = null }) {
                    Text("Annuler", color = AppColors.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPurgeJournal(journalToPurge!!.id)
                        journalToPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ErrorRed,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Text("Supprimer définitivement")
                }
            }
        )
    }

    if (rowToPurge != null) {
        AlertDialog(
            onDismissRequest = { rowToPurge = null },
            containerColor = AppColors.Panel,
            title = {
                Text(
                    text = "Supprimer définitivement cette ligne ?",
                    color = AppColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Cette action est irréversible.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { rowToPurge = null }) {
                    Text("Annuler", color = AppColors.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPurgeRow(rowToPurge!!.id)
                        rowToPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ErrorRed,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Text("Supprimer définitivement")
                }
            }
        )
    }
}

private fun rowPreview(valuesJson: String): String {
    return try {
        val obj = JSONObject(valuesJson)
        obj.keys().asSequence()
            .map { obj.optString(it, "") }
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .take(120)
            .ifBlank { "Ligne sans contenu" }
    } catch (_: Exception) {
        "Ligne sans contenu"
    }
}
