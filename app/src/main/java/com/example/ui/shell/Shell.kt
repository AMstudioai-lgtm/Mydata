package com.example.ui.shell

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.ui.auth.AuthButton
import com.example.ui.auth.AuthDialog
import com.example.ui.journal.JournalPage
import com.example.ui.journal.RowDetailPage
import com.example.data.sync.SyncOutcome
import com.example.data.sync.SyncStatus
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
    var currentPageIndex by remember { mutableIntStateOf(0) } // 0 = Journal, 1 = Corbeille, 2 = Paramètres
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showAuthDialog by remember { mutableStateOf(false) }
    var journalSectionExpanded by remember { mutableStateOf(true) }
    var journalToDelete by remember { mutableStateOf<JournalEntity?>(null) }

    // Handle back button: dismiss row detail, close drawer, or switch to home tab
    BackHandler(enabled = selectedRowForDetail != null || drawerState.isOpen || currentPageIndex != 0) {
        if (selectedRowForDetail != null) {
            onCloseRowDetail()
        } else if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentPageIndex != 0) {
            currentPageIndex = 0
        }
    }

    // If row detail is open, display it directly
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
            ModalDrawerSheet(
                drawerContainerColor = AppColors.Panel,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .testTag("navigation_drawer")
            ) {
                // Header: App Logo (Large blue M on black background) + "Mydata"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppColors.Border),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_mydata_logo),
                            contentDescription = "Logo Mydata",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mydata",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Bases de données & Tableaux offline",
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = AppColors.Border)

                Spacer(modifier = Modifier.height(8.dp))

                // Section "Journaux" (Expandable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { journalSectionExpanded = !journalSectionExpanded }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Databases",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (journalSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Button [+] to create database
                    IconButton(
                        onClick = {
                            onCreateJournal()
                            currentPageIndex = 0
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.size(28.dp).testTag("drawer_create_journal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Créer une database",
                            tint = AppColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Database items list: shifted to the right with vertical indentation tree guide line
                AnimatedVisibility(visible = journalSectionExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        if (journals.isEmpty()) {
                            Text(
                                text = "Aucune database existante",
                                fontSize = 12.sp,
                                color = AppColors.TextMuted,
                                modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp)
                            )
                        } else {
                            journals.forEach { journal ->
                                val isSelected = selectedJournal?.id == journal.id && currentPageIndex == 0

                                // Shifted right with an indentation guide line on the left
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Vertical indentation guide line
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(36.dp)
                                            .background(if (isSelected) AppColors.Accent else AppColors.Border)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectJournal(journal.id)
                                                currentPageIndex = 0
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .testTag("journal_item_${journal.id}"),
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) AppColors.SelectedBg else androidx.compose.ui.graphics.Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = journal.name,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Trash icon to delete
                                            IconButton(
                                                onClick = { journalToDelete = journal },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Supprimer",
                                                    tint = AppColors.TextMuted,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AppColors.Border)
                Spacer(modifier = Modifier.height(8.dp))

                // Section "Corbeille"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable {
                            currentPageIndex = 1
                            coroutineScope.launch { drawerState.close() }
                        }
                        .testTag("drawer_trash_button"),
                    shape = RoundedCornerShape(6.dp),
                    color = if (currentPageIndex == 1) AppColors.SelectedBg else androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (currentPageIndex == 1) AppColors.TextPrimary else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Corbeille",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (currentPageIndex == 1) AppColors.TextPrimary else AppColors.TextSecondary
                        )
                    }
                }

                // Section "Paramètres"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable {
                            currentPageIndex = 2
                            coroutineScope.launch { drawerState.close() }
                        }
                        .testTag("drawer_settings_button"),
                    shape = RoundedCornerShape(6.dp),
                    color = if (currentPageIndex == 2) AppColors.SelectedBg else androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (currentPageIndex == 2) AppColors.TextPrimary else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Paramètres",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (currentPageIndex == 2) AppColors.TextPrimary else AppColors.TextSecondary
                        )
                    }
                }
            }
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
                            text = if (currentPageIndex == 0) {
                                if (selectedJournal != null) "Database — ${selectedJournal.name}"
                                else "Database"
                            } else if (currentPageIndex == 1) "Corbeille" else "Paramètres",
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
                if (currentPageIndex == 0) {
                    JournalPage(
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
                } else if (currentPageIndex == 1) {
                    TrashPage(
                        trashedJournals = trashedJournals,
                        allJournals = journals,
                        trashedRows = trashedRows,
                        onRestoreJournal = onRestoreJournal,
                        onPurgeJournal = onPurgeJournal,
                        onRestoreRow = onRestoreRow,
                        onPurgeRow = onPurgeRow,
                        modifier = Modifier
                    )
                } else {
                    SettingsPage(
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

    // Auth Dialog
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

    // Delete confirmation dialog from drawer
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
