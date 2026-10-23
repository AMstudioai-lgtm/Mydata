package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.auth.AuthState
import com.example.data.sync.SyncOutcome
import com.example.data.sync.SyncStatus
import com.example.ui.theme.AppColors

@Composable
fun SettingsPage(
    authState: AuthState,
    onOpenAuthDialog: () -> Unit,
    onSignOut: () -> Unit,
    syncStatus: SyncStatus,
    syncOutcome: SyncOutcome?,
    isSyncAvailable: Boolean,
    onSync: () -> Unit,
    onDismissSyncFeedback: () -> Unit,
    backupFeedback: String?,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onClearBackupFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) onExportBackup(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportBackup(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_page"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Paramètres",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )

        // Profile Section Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AppColors.Panel,
            border = BorderStroke(1.dp, AppColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profil Utilisateur",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (authState) {
                    is AuthState.LoggedIn -> {
                        Text(
                            text = "Email connecté :",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = authState.session.email,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.AccentLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = AppColors.SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Session Supabase active — synchronisation disponible",
                                fontSize = 11.sp,
                                color = AppColors.SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.ErrorRed
                            ),
                            border = BorderStroke(1.dp, AppColors.ErrorRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("settings_logout_button")
                        ) {
                            Text("Se déconnecter")
                        }
                    }
                    else -> {
                        Text(
                            text = "Mode local (sans compte)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vous utilisez l'application en mode 100% hors-ligne. Connectez-vous pour authentifier votre session et activer la synchronisation entre appareils.",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onOpenAuthDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Accent,
                                contentColor = AppColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("settings_login_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connexion / Créer un compte")
                        }
                    }
                }
            }
        }

        // Sync Section (v1.1.0)
        if (isSyncAvailable) {
            SyncSection(
                authState = authState,
                syncStatus = syncStatus,
                lastOutcome = syncOutcome,
                onSync = onSync,
                onDismissFeedback = onDismissSyncFeedback
            )
        }

        // Sauvegarde locale (v1.3.2)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AppColors.Panel,
            border = BorderStroke(1.dp, AppColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sauvegarde locale",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Exportez vos données dans un fichier ZIP pour les conserver " +
                        "hors de l'appareil. La désinstallation de l'application efface " +
                        "toutes les données locales : sans sauvegarde, elles sont " +
                        "définitivement perdues.",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("Mydata-backup.zip") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("backup_export_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/zip")) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.TextPrimary
                        ),
                        border = BorderStroke(1.dp, AppColors.Border),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("backup_import_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importer")
                    }
                }

                if (backupFeedback != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = backupFeedback,
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_feedback")
                        )
                        OutlinedButton(
                            onClick = onClearBackupFeedback,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.TextSecondary
                            ),
                            border = BorderStroke(1.dp, AppColors.Border),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("backup_ok_button")
                        ) {
                            Text("OK", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // About Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AppColors.Panel,
            border = BorderStroke(1.dp, AppColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppColors.AccentLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "À propos de Mydata",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} — Application de bases de données et tableaux personnalisés offline-first, avec synchronisation cloud des données pour les appareils connectés.",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
