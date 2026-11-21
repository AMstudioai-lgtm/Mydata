package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette monochrome Noir / Gris / Blanc + Accent Bleu pour les actions spéciales.
 *
 * 4 rôles chromatiques :
 * 1. Primaire : Noir pur (#000000) pour le fond principal.
 * 2. Secondaire : Blanc (#FAFAFA) et gris neutres (#A1A1AA, #6B6B73) pour la hiérarchie typographique.
 * 3. Tertiaire : Surfaces plates en gris anthracite (#121214, #1A1A1D) et bordures fines (#26262A).
 * 4. Quaternaire : Accent Bleu Cobalt (#2563EB) pour les points d'action clés et états système.
 */
object AppColors {
    val Background = Color(0xFF000000)       // Noir pur (fond principal)
    val Panel = Color(0xFF121214)            // Gris anthracite plat (AppBar, Drawer, panneaux, en-têtes)
    val SurfaceElevated = Color(0xFF1A1A1D)  // Gris moyen sombre (lignes alternées, champs imbriqués)
    val Border = Color(0xFF26262A)           // Gris bordure fine (1dp, sans ombre)
    val SelectedBg = Color(0xFF0F1D36)       // Surface active subtilement teintée de bleu
    val Accent = Color(0xFF2563EB)           // Accent principal (Bleu cobalt) pour actions clés
    val AccentLight = Color(0xFF60A5FA)      // Accent bleu adouci pour liens et repères secondaires
    val OnAccent = Color(0xFFFAFAFA)         // Contraste blanc net sur l'accent bleu
    val TextPrimary = Color(0xFFFAFAFA)      // Blanc principal
    val TextSecondary = Color(0xFFA1A1AA)    // Gris clair lisible
    val TextMuted = Color(0xFF6B6B73)        // Gris discret pour placeholders et vides
    val ErrorRed = Color(0xFFEF4444)         // Erreurs et actions destructives
    val SuccessGreen = Color(0xFF22C55E)     // Confirmations et états positifs
    val WarningAmber = Color(0xFFF59E0B)     // Avertissements
}
