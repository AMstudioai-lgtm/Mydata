package com.example.data.session

import com.example.data.auth.FirebaseAuthService

/**
 * Branche FirebaseAuthService sur les producteurs de données Room.
 * Garde la dépendance dans un seul sens : les repositories connaissent la session,
 * jamais l'inverse.
 */
fun FirebaseAuthService.asSessionProvider(): SessionProvider = object : SessionProvider {
    override fun currentUserId(): String? = this@asSessionProvider.currentUserId()
    override fun currentEmail(): String? = this@asSessionProvider.currentEmail()
}
