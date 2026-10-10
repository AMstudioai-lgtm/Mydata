package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthState
import com.example.ui.theme.AppColors

@Composable
fun AuthButton(
    authState: AuthState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, icon) = when (authState) {
        is AuthState.LoggedIn -> {
            val email = authState.session.email
            val display = if (email.length > 18) email.take(15) + "…" else email
            display to Icons.Default.AccountCircle
        }
        else -> "Connexion" to Icons.Default.Login
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.testTag("auth_button"),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, AppColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.Panel,
            contentColor = AppColors.TextPrimary
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
