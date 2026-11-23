package com.example.data.auth

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Service d'authentification adossé à Firebase Auth avec fournisseur Google Sign-In uniquement.
 */
class FirebaseAuthService(
    private val authProvider: () -> FirebaseAuth = { Firebase.auth }
) {
    private val auth: FirebaseAuth
        get() = authProvider()

    val isConfigured: Boolean = true

    fun currentUserId(): String? = currentUserSafe()?.uid?.takeIf { it.isNotBlank() }

    fun currentEmail(): String? = currentUserSafe()?.email

    fun getCachedSession(): AuthSession? = currentUserSafe()?.toAuthSession()

    fun observeAuthSession(): Flow<AuthSession?> = callbackFlow {
        val firebaseAuth = auth
        val listener = FirebaseAuth.AuthStateListener { state ->
            trySend(state.currentUser?.toAuthSession())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toAuthSession())
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthSession> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Jeton Google manquant"))
        }
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Utilisateur Firebase introuvable après connexion"))
            Result.success(user.toAuthSession())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun currentUserSafe(): FirebaseUser? = try {
        auth.currentUser
    } catch (_: Exception) {
        null
    }

    private fun FirebaseUser.toAuthSession(): AuthSession = AuthSession(
        userId = uid,
        email = email.orEmpty().ifBlank { displayName.orEmpty().ifBlank { uid } },
        displayName = displayName?.takeIf { it.isNotBlank() },
        photoUrl = photoUrl?.toString()
    )
}
