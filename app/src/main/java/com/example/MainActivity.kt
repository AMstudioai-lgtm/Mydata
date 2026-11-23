package com.example

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthState
import com.example.data.update.UpdateInfo
import com.example.di.AppContainer
import com.example.ui.auth.AuthScreen
import com.example.ui.shell.Shell
import com.example.ui.theme.AppTheme
import com.example.ui.update.UpdateDialog
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.JournalViewModel
import com.example.viewmodel.SyncViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )

        val app = application as MyApp
        val container = app.container

        val authViewModel: AuthViewModel by viewModels {
            AuthViewModel.Factory(container.authService)
        }

        setContent {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val isAuthSubmitting by authViewModel.isSubmitting.collectAsStateWithLifecycle()
            val authErrorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

            // Reconnexion silencieuse Google via Credential Manager dès le démarrage si déconnecté.
            LaunchedEffect(authState) {
                if (authState is AuthState.LoggedOut) {
                    authViewModel.attemptSilentSignIn(this@MainActivity)
                }
            }

            // Vérification de mise à jour au premier lancement uniquement.
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                updateInfo = container.updateChecker.check(BuildConfig.VERSION_NAME)
            }

            AppTheme {
                val loggedInState = authState as? AuthState.LoggedIn
                if (loggedInState == null) {
                    AuthScreen(
                        authState = authState,
                        isSubmitting = isAuthSubmitting,
                        errorMessage = authErrorMessage,
                        onGoogleSignIn = { authViewModel.signInWithGoogle(this@MainActivity) }
                    )
                } else {
                    val uid = loggedInState.session.userId
                    key(uid) {
                        AuthenticatedWorkspace(
                            userId = uid,
                            authState = loggedInState,
                            isAuthSubmitting = isAuthSubmitting,
                            authErrorMessage = authErrorMessage,
                            container = container,
                            syncPrefs = remember { getSharedPreferences("myapp_sync_prefs", MODE_PRIVATE) },
                            onGoogleSignIn = { authViewModel.signInWithGoogle(this@MainActivity) },
                            onSignOut = { authViewModel.signOut(this@MainActivity) }
                        )
                    }
                }

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

@Composable
private fun AuthenticatedWorkspace(
    userId: String,
    authState: AuthState.LoggedIn,
    isAuthSubmitting: Boolean,
    authErrorMessage: String?,
    container: AppContainer,
    syncPrefs: android.content.SharedPreferences,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val journalViewModel: JournalViewModel = viewModel(
        key = "journal_$userId",
        factory = JournalViewModel.Factory(container.journalRepository, container.backupManager)
    )
    val syncViewModel: SyncViewModel = viewModel(
        key = "sync_$userId",
        factory = SyncViewModel.Factory(container.syncRepository)
    )

    val journals by journalViewModel.journals.collectAsStateWithLifecycle()
    val selectedJournal by journalViewModel.selectedJournal.collectAsStateWithLifecycle()
    val properties by journalViewModel.properties.collectAsStateWithLifecycle()
    val rows by journalViewModel.rows.collectAsStateWithLifecycle()
    val selectedRowForDetail by journalViewModel.selectedRowForDetail.collectAsStateWithLifecycle()

    val syncStatus by syncViewModel.status.collectAsStateWithLifecycle()
    val syncOutcome by syncViewModel.lastOutcome.collectAsStateWithLifecycle()
    val isSyncAvailable by syncViewModel.isAvailable.collectAsStateWithLifecycle()

    val trashedJournals by journalViewModel.trashedJournals.collectAsStateWithLifecycle()
    val trashedRows by journalViewModel.trashedRows.collectAsStateWithLifecycle()
    val backupFeedback by journalViewModel.backupFeedback.collectAsStateWithLifecycle()

    var isAutoSyncEnabled by remember {
        mutableStateOf(syncPrefs.getBoolean("auto_sync", true))
    }

    // Propage l'UID authentifié au ViewModel local et rattache les éventuelles données orphelines,
    // puis déclenche la synchronisation Firestore initiale.
    LaunchedEffect(userId) {
        journalViewModel.setCurrentUser(userId)
        container.journalRepository.adoptOrphans(userId)
        if (isAutoSyncEnabled) {
            syncViewModel.syncNow()
        }
    }

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
        onRenameJournal = { id, name -> journalViewModel.renameJournal(id, name) },
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
        onGoogleSignIn = onGoogleSignIn,
        onSignOut = onSignOut,
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
}
