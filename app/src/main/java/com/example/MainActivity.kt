package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.AuthState
import com.example.data.update.UpdateInfo
import com.example.ui.shell.Shell
import com.example.ui.theme.AppTheme
import com.example.ui.update.UpdateDialog
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.JournalViewModel
import com.example.viewmodel.SyncViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MyApp
        val container = app.container

        val journalViewModel: JournalViewModel by viewModels {
            JournalViewModel.Factory(container.journalRepository)
        }

        val authViewModel: AuthViewModel by viewModels {
            AuthViewModel.Factory(container.authService)
        }

        val syncViewModel: SyncViewModel by viewModels {
            SyncViewModel.Factory(container.syncRepository)
        }

        setContent {
            val journals by journalViewModel.journals.collectAsStateWithLifecycle()
            val selectedJournal by journalViewModel.selectedJournal.collectAsStateWithLifecycle()
            val properties by journalViewModel.properties.collectAsStateWithLifecycle()
            val rows by journalViewModel.rows.collectAsStateWithLifecycle()
            val selectedRowForDetail by journalViewModel.selectedRowForDetail.collectAsStateWithLifecycle()

            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val currentUserId by authViewModel.currentUserId.collectAsStateWithLifecycle()
            val isAuthSubmitting by authViewModel.isSubmitting.collectAsStateWithLifecycle()
            val authErrorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

            val syncStatus by syncViewModel.status.collectAsStateWithLifecycle()
            val syncOutcome by syncViewModel.lastOutcome.collectAsStateWithLifecycle()
            val isSyncAvailable by syncViewModel.isAvailable.collectAsStateWithLifecycle()

            val trashedJournals by journalViewModel.trashedJournals.collectAsStateWithLifecycle()
            val trashedRows by journalViewModel.trashedRows.collectAsStateWithLifecycle()
            val backupFeedback by journalViewModel.backupFeedback.collectAsStateWithLifecycle()

            // Interrupteur « synchro auto », persisté en SharedPreferences.
            val syncPrefs = remember { getSharedPreferences("myapp_sync_prefs", MODE_PRIVATE) }
            var isAutoSyncEnabled by remember {
                mutableStateOf(syncPrefs.getBoolean("auto_sync", false))
            }

            // Synchro auto au premier lancement : une seule fois (garde-fou en
            // mémoire), quand l'option est activée et la session active.
            var autoSyncFired by remember { mutableStateOf(false) }
            LaunchedEffect(authState, isAutoSyncEnabled) {
                if (!autoSyncFired && isAutoSyncEnabled && authState is AuthState.LoggedIn) {
                    autoSyncFired = true
                    syncViewModel.syncNow()
                }
            }

            // Vérification de mise à jour au premier lancement uniquement.
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                updateInfo = container.updateChecker.check(BuildConfig.VERSION_NAME)
            }

            // Propage le compte courant vers le ViewModel des journaux pour que
            // chaque compte ne voie que ses données (+ orphelines) via flatMapLatest.
            LaunchedEffect(currentUserId) {
                journalViewModel.setCurrentUser(currentUserId)
            }

            // Une connexion qui vient de réussir déclenche la synchronisation immédiatement,
            // pour que l'utilisateur retrouve ses données sans action supplémentaire.
            var wasLoggedIn by remember { mutableStateOf(authState is AuthState.LoggedIn) }
            LaunchedEffect(authState) {
                val loggedInState = authState as? AuthState.LoggedIn
                if (loggedInState != null && !wasLoggedIn) {
                    // Les orphelines créées hors-ligne sont adoptées AVANT la synchro
                    // pour être incluses dans le push vers le cloud.
                    container.journalRepository.adoptOrphans(loggedInState.session.userId)
                    syncViewModel.syncNow()
                }
                wasLoggedIn = loggedInState != null
            }

            AppTheme {
                Shell(
                    journals = journals,
                    selectedJournal = selectedJournal,
                    properties = properties,
                    rows = rows,
                    selectedRowForDetail = selectedRowForDetail,
                    authState = authState,
                    isAuthSubmitting = isAuthSubmitting,
                    authErrorMessage = authErrorMessage,
                    onSelectJournal = { id -> journalViewModel.selectJournal(id) },
                    onCreateJournal = { journalViewModel.createJournal() },
                    onDeleteJournal = { id -> journalViewModel.deleteJournal(id) },
                    onAddProperty = { name, kind, options, format ->
                        journalViewModel.addProperty(name, kind, options, format)
                    },
                    onDeleteProperty = { propId -> journalViewModel.deleteProperty(propId) },
                    onUpdateProperty = { propId, name, kind, options, format ->
                        journalViewModel.updateProperty(propId, name, kind, options, format)
                    },
                    onMoveProperty = { propId, delta -> journalViewModel.moveProperty(propId, delta) },
                    onMovePropertyToFront = { propId -> journalViewModel.movePropertyToFront(propId) },
                    onMovePropertyToBack = { propId -> journalViewModel.movePropertyToBack(propId) },
                    onOpenRow = { row -> journalViewModel.openRowDetail(row.id) },
                    onCloseRowDetail = { journalViewModel.closeRowDetail() },
                    onAddRow = { journalViewModel.addRow() },
                    onUpdateCell = { rowId, propId, value ->
                        journalViewModel.updateCell(rowId, propId, value)
                    },
                    onDeleteRow = { rowId -> journalViewModel.deleteRow(rowId) },
                    onSignIn = { email, password -> authViewModel.signIn(email, password) },
                    onSignUp = { email, password -> authViewModel.signUp(email, password) },
                    onSignOut = { authViewModel.signOut() },
                    syncStatus = syncStatus,
                    syncOutcome = syncOutcome,
                    isSyncAvailable = isSyncAvailable,
                    onSync = { syncViewModel.syncNow() },
                    onDismissSyncFeedback = { syncViewModel.clearFeedback() },
                    trashedJournals = trashedJournals,
                    trashedRows = trashedRows,
                    onRestoreJournal = { id -> journalViewModel.restoreJournal(id) },
                    onPurgeJournal = { id -> journalViewModel.purgeJournal(id) },
                    onRestoreRow = { id -> journalViewModel.restoreRow(id) },
                    onPurgeRow = { id -> journalViewModel.purgeRow(id) },
                    backupFeedback = backupFeedback,
                    onExportBackup = { uri -> journalViewModel.exportBackup(uri) },
                    onImportBackup = { uri -> journalViewModel.importBackup(uri) },
                    onClearBackupFeedback = { journalViewModel.clearBackupFeedback() },
                    isAutoSyncEnabled = isAutoSyncEnabled,
                    onAutoSyncChange = { enabled ->
                        syncPrefs.edit().putBoolean("auto_sync", enabled).apply()
                        isAutoSyncEnabled = enabled
                    }
                )

                val pendingUpdate = updateInfo
                if (pendingUpdate != null && !updateDismissed) {
                    UpdateDialog(
                        info = pendingUpdate,
                        onDownload = {
                            updateDismissed = true
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pendingUpdate.url)))
                        },
                        onDismiss = { updateDismissed = true }
                    )
                }
            }
        }
    }
}
