package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.auth.AuthState
import com.example.data.sync.SyncOutcome
import com.example.data.sync.SyncStatus
import com.example.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE)

@Composable
fun SyncSection(
    authState: AuthState,
    syncStatus: SyncStatus,
    lastOutcome: SyncOutcome?,
    onSync: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AppColors.Panel,
        border = BorderStroke(1.dp, AppColors.Border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Synchronisation cloud",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isLoggedIn = authState is AuthState.LoggedIn

            if (!isLoggedIn) {
                Text(
                    text = "Connectez-vous pour activer la synchronisation entre vos appareils.",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                return@Column
            }

            StatusLine(syncStatus = syncStatus)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Vos données restent stockées localement. La synchronisation envoie et " +
                    "reçoit uniquement les modifications, et la version la plus récente gagne " +
                    "en cas de conflit.",
                fontSize = 11.sp,
                color = AppColors.TextMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSync,
                enabled = !syncStatus.isSyncing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.TextPrimary
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("sync_button")
            ) {
                if (syncStatus.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(syncStatus.phase.ifBlank { "Synchronisation…" })
                } else {
                    Text("Synchroniser maintenant")
                }
            }

            val feedback = lastOutcome
            if (feedback is SyncOutcome.Success) {
                FeedbackText(
                    text = "Synchronisé : ${feedback.pushed} envoi(s), ${feedback.pulled} réception(s).",
                    positive = true,
                    onDismiss = onDismissFeedback
                )
            } else if (feedback is SyncOutcome.Failure) {
                FeedbackText(
                    text = feedback.message,
                    positive = false,
                    onDismiss = onDismissFeedback
                )
            } else if (feedback is SyncOutcome.Skipped) {
                FeedbackText(
                    text = feedback.reason,
                    positive = false,
                    onDismiss = onDismissFeedback
                )
            }
        }
    }
}

@Composable
private fun StatusLine(syncStatus: SyncStatus) {
    val icon = when {
        syncStatus.isSyncing -> Icons.Default.CloudSync
        syncStatus.error != null -> Icons.Default.CloudOff
        syncStatus.lastSyncAt > 0 -> Icons.Default.CloudDone
        else -> Icons.Default.CloudOff
    }
    val tint = when {
        syncStatus.error != null -> AppColors.ErrorRed
        syncStatus.lastSyncAt > 0 -> AppColors.SuccessGreen
        else -> AppColors.TextMuted
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = when {
                syncStatus.isSyncing -> syncStatus.phase.ifBlank { "Synchronisation…" }
                syncStatus.error != null -> "Dernière synchro en erreur"
                syncStatus.lastSyncAt > 0 -> "Dernière synchro : ${dateFormat.format(Date(syncStatus.lastSyncAt))}"
                else -> "Jamais synchronisé"
            },
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun FeedbackText(text: String, positive: Boolean, onDismiss: () -> Unit) {
    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (positive) AppColors.SuccessGreen else AppColors.ErrorRed,
            modifier = Modifier.weight(1f)
        )
        SmallTextButton(onClick = onDismiss, text = "OK")
    }
}

@Composable
private fun SmallTextButton(onClick: () -> Unit, text: String) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary),
        border = BorderStroke(1.dp, AppColors.Border),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp)
    }
}
