package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.data.auth.AuthState
import com.example.ui.theme.AppColors

@Composable
fun AuthDialog(
    authState: AuthState,
    isSubmitting: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppColors.Panel,
            border = BorderStroke(1.dp, AppColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (authState is AuthState.LoggedIn) "Compte Google" else "Connexion Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (authState is AuthState.LoggedIn) {
                    Text(
                        text = "Connecté avec Google :",
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary
                    )
                    if (!authState.session.displayName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = authState.session.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = authState.session.email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.AccentLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "UID Firebase : ${authState.session.userId}",
                        fontSize = 11.sp,
                        color = AppColors.TextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.TextSecondary
                            ),
                            border = BorderStroke(1.dp, AppColors.Border)
                        ) {
                            Text("Fermer")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSignOut()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.ErrorRed,
                                contentColor = AppColors.TextPrimary
                            ),
                            modifier = Modifier.testTag("auth_logout_button")
                        ) {
                            Text("Se déconnecter")
                        }
                    }
                } else {
                    Text(
                        text = "Utilisez votre compte Google pour accéder à vos données et activer la synchronisation Firebase Firestore.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = AppColors.TextSecondary
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            color = AppColors.ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGoogleSignIn,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.OnAccent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = AppColors.OnAccent,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continuer avec Google",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
