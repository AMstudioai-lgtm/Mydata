package com.example.data.session

import com.example.data.auth.SupabaseAuthService

/**
 * Branche le service d'authentification sur les producteurs de données.
 * Garde la dépendance dans un seul sens : les repositories connaissent la session,
 * jamais l'inverse.
 */
fun SupabaseAuthService.asSessionProvider(): SessionProvider = object : SessionProvider {
    override fun currentUserId(): String? = this@asSessionProvider.currentUserId()
    override fun currentEmail(): String? = this@asSessionProvider.currentEmail()
}
