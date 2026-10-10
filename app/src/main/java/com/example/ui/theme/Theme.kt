package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.TextPrimary,
    primaryContainer = AppColors.SelectedBg,
    onPrimaryContainer = AppColors.TextPrimary,
    secondary = AppColors.AccentLight,
    onSecondary = AppColors.TextPrimary,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Panel,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.Panel,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Border,
    error = AppColors.ErrorRed,
    onError = AppColors.TextPrimary
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
