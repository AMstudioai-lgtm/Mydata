package com.example.ui.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.update.UpdateInfo
import com.example.ui.theme.AppColors

@Composable
fun UpdateDialog(info: UpdateInfo, onDownload: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        title = {
            Text(
                text = "Mise à jour disponible",
                color = AppColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "La version ${info.version} est disponible.\n\n" +
                    "Vos données locales sont conservées si vous mettez à jour " +
                    "sans désinstaller l'application.",
                color = AppColors.TextSecondary,
                fontSize = 13.sp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Plus tard", color = AppColors.TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.TextPrimary
                )
            ) {
                Text("Télécharger")
            }
        }
    )
}
