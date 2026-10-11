package com.example

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
import com.example.ui.shell.Shell
import com.example.ui.theme.AppTheme
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
                    onDismissSyncFeedback = { syncViewModel.clearFeedback() }
                )
            }
        }
    }
}
