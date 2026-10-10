package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.shell.Shell
import com.example.ui.theme.AppTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.JournalViewModel

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

        setContent {
            val journals by journalViewModel.journals.collectAsStateWithLifecycle()
            val selectedJournal by journalViewModel.selectedJournal.collectAsStateWithLifecycle()
            val properties by journalViewModel.properties.collectAsStateWithLifecycle()
            val rows by journalViewModel.rows.collectAsStateWithLifecycle()
            val selectedRowForDetail by journalViewModel.selectedRowForDetail.collectAsStateWithLifecycle()

            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val isAuthSubmitting by authViewModel.isSubmitting.collectAsStateWithLifecycle()
            val authErrorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

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
                    onAddProperty = { name, kind, options ->
                        journalViewModel.addProperty(name, kind, options)
                    },
                    onDeleteProperty = { propId -> journalViewModel.deleteProperty(propId) },
                    onUpdateProperty = { propId, name, kind, options ->
                        journalViewModel.updateProperty(propId, name, kind, options)
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
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
