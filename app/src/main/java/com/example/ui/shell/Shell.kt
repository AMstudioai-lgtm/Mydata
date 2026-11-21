package com.example.ui.shell

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthState
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.sync.SyncOutcome
import com.example.data.sync.SyncStatus
import com.example.ui.auth.AuthButton
import com.example.ui.auth.AuthDialog
import com.example.ui.journal.JournalPage
import com.example.ui.journal.RowDetailPage
import com.example.ui.settings.SettingsPage
import com.example.ui.theme.AppColors
import com.example.ui.trash.TrashPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Shell(
    journals: List<JournalEntity>,
    selectedJournal: JournalEntity?,
    properties: List<PropertyEntity>,
    rows: List<EntryRowEntity>,
    selectedRowForDetail: EntryRowEntity?,
    authState: AuthState,
    isAuthSubmitting: Boolean,
    authErrorMessage: String?,
    onSelectJournal: (String) -> Unit,
    onCreateJournal: () -> Unit,
    onDeleteJournal: (String) -> Unit,
    onRenameJournal: (String, String) -> Unit = { _, _ -> },
    onAddProperty: (name: String, kind: String, options: List<String>, format: String?) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onUpdateProperty: (propertyId: String, name: String, kind: String, options: List<String>, format: String?) -> Unit,
    onMoveProperty: (propId: String, delta: Int) -> Unit,
    onMovePropertyToFront: (propId: String) -> Unit,
    onMovePropertyToBack: (propId: String) -> Unit,
    onOpenRow: (EntryRowEntity) -> Unit,
    onCloseRowDetail: () -> Unit,
    onAddRow: () -> Unit,
    onUpdateCell: (rowId: String, propId: String, value: Any?) -> Unit,
    onDeleteRow: (rowId: String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    syncStatus: SyncStatus,
    syncOutcome: SyncOutcome?,
    isSyncAvailable: Boolean,
    onSync: () -> Unit,
    onDismissSyncFeedback: () -> Unit,
    trashedJournals: List<JournalEntity>,
    trashedRows: List<EntryRowEntity>,
    onRestoreJournal: (String) -> Unit,
    onPurgeJournal: (String) -> Unit,
    onRestoreRow: (String) -> Unit,
    onPurgeRow: (String) -> Unit,
    backupFeedback: String?,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onClearBackupFeedback: () -> Unit,
    isAutoSyncEnabled: Boolean,
    onAutoSyncChange: (Boolean) -> Unit
) {
    var currentPageIndex by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showAuthDialog by remember { mutableStateOf(false) }
    var journalToDelete by remember { mutableStateOf<JournalEntity?>(null) }

    BackHandler(enabled = selectedRowForDetail != null || drawerState.isOpen || currentPageIndex != 0) {
        if (selectedRowForDetail != null) {
            onCloseRowDetail()
        } else if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentPageIndex != 0) {
            currentPageIndex = 0
        }
    }

    if (selectedRowForDetail != null) {
        RowDetailPage(
            row = selectedRowForDetail,
            properties = properties,
            onUpdateCell = onUpdateCell,
            onDeleteRow = onDeleteRow,
            onClose = onCloseRowDetail
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ShellDrawerContent(
                journals = journals,
                selectedJournal = selectedJournal,
                currentPageIndex = currentPageIndex,
                onSelectJournal = { id ->
                    onSelectJournal(id)
                    currentPageIndex = 0
                    coroutineScope.launch { drawerState.close() }
                },
                onCreateJournal = {
                    onCreateJournal()
                    currentPageIndex = 0
                    coroutineScope.launch { drawerState.close() }
                },
                onRequestDeleteJournal = { journal -> journalToDelete = journal },
                onNavigateToPage = { page ->
                    currentPageIndex = page
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
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
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("drawer_burger_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = when (currentPageIndex) {
                                0 -> if (selectedJournal != null) "Database — ${selectedJournal.name}" else "Database"
                                1 -> "Corbeille"
                                else -> "Paramètres"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onAutoSyncChange(!isAutoSyncEnabled) },
                                modifier = Modifier.testTag("autosync_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = if (isAutoSyncEnabled) "Synchro auto activée" else "Synchro auto désactivée",
                                    tint = if (isAutoSyncEnabled) AppColors.Accent else AppColors.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            AuthButton(
                                authState = authState,
                                onClick = { showAuthDialog = true },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentPageIndex,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    },
                    label = "shell_page_transition"
                ) { page ->
                    when (page) {
                        0 -> JournalPage(
                            selectedJournal = selectedJournal,
                            properties = properties,
                            rows = rows,
                            hasAnyJournals = journals.isNotEmpty(),
                            onCreateJournal = onCreateJournal,
                            onDeleteJournal = onDeleteJournal,
                            onAddProperty = onAddProperty,
                            onDeleteProperty = onDeleteProperty,
                            onUpdateProperty = onUpdateProperty,
                            onMoveProperty = onMoveProperty,
                            onMovePropertyToFront = onMovePropertyToFront,
                            onMovePropertyToBack = onMovePropertyToBack,
                            onOpenRow = onOpenRow,
                            onAddRow = onAddRow,
                            onUpdateCell = onUpdateCell,
                            onDeleteRow = onDeleteRow,
                            onRenameJournal = onRenameJournal
                        )
                        1 -> TrashPage(
                            trashedJournals = trashedJournals,
                            allJournals = journals,
                            trashedRows = trashedRows,
                            onRestoreJournal = onRestoreJournal,
                            onPurgeJournal = onPurgeJournal,
                            onRestoreRow = onRestoreRow,
                            onPurgeRow = onPurgeRow
                        )
                        else -> SettingsPage(
                            authState = authState,
                            onOpenAuthDialog = { showAuthDialog = true },
                            onSignOut = onSignOut,
                            syncStatus = syncStatus,
                            syncOutcome = syncOutcome,
                            isSyncAvailable = isSyncAvailable,
                            onSync = onSync,
                            onDismissSyncFeedback = onDismissSyncFeedback,
                            backupFeedback = backupFeedback,
                            onExportBackup = onExportBackup,
                            onImportBackup = onImportBackup,
                            onClearBackupFeedback = onClearBackupFeedback
                        )
                    }
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            authState = authState,
            isSubmitting = isAuthSubmitting,
            errorMessage = authErrorMessage,
            onSignIn = onSignIn,
            onSignUp = onSignUp,
            onSignOut = onSignOut,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (journalToDelete != null) {
        val journal = journalToDelete!!
        AlertDialog(
            onDismissRequest = { journalToDelete = null },
            containerColor = AppColors.Panel,
            title = {
                Text(
                    text = "Supprimer ${journal.name} ?",
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
                TextButton(onClick = { journalToDelete = null }) {
                    Text("Annuler", color = AppColors.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteJournal(journal.id)
                        journalToDelete = null
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
}
