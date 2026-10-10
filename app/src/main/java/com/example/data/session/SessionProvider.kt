package com.example.data.session

/**
 * Accès en lecture seule à la session courante.
 * Sert à taguer les données locales avec leur propriétaire cloud, afin qu'un changement de
 * compte ne mélange jamais les données de deux utilisateurs.
 */
interface SessionProvider {
    fun currentUserId(): String?
    fun currentEmail(): String?
}
