package com.example.data.auth

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
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
    private val context: Context? = null,
    private val authProvider: () -> FirebaseAuth = { Firebase.auth },
    private val configuredCheck: () -> Boolean = { isDefaultFirebaseConfigured(context) }
) {
    val isConfigured: Boolean
        get() = configuredCheck()

    fun currentUserId(): String? = currentUserSafe()?.uid?.takeIf { it.isNotBlank() }

    fun currentEmail(): String? = currentUserSafe()?.email

    fun getCachedSession(): AuthSession? = currentUserSafe()?.toAuthSession()

    fun observeAuthSession(): Flow<AuthSession?> = callbackFlow {
        val firebaseAuth = authOrNull()
        if (firebaseAuth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
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
        val firebaseAuth = authOrNull()
            ?: return Result.failure(
                IllegalStateException("Firebase n'est pas configuré dans ce build (google-services.json absent).")
            )
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
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
        try {
            authOrNull()?.signOut()
        } catch (_: Exception) {
        }
    }

    private fun authOrNull(): FirebaseAuth? = try {
        authProvider()
    } catch (_: Exception) {
        null
    }

    private fun currentUserSafe(): FirebaseUser? = try {
        authOrNull()?.currentUser
    } catch (_: Exception) {
        null
    }

    private fun FirebaseUser.toAuthSession(): AuthSession = AuthSession(
        userId = uid,
        email = email.orEmpty().ifBlank { displayName.orEmpty().ifBlank { uid } },
        displayName = displayName?.takeIf { it.isNotBlank() },
        photoUrl = photoUrl?.toString()
    )

    private companion object {
        fun isDefaultFirebaseConfigured(context: Context?): Boolean {
            val hasFirebaseApp = try {
                if (context != null) {
                    FirebaseApp.getApps(context).isNotEmpty()
                } else {
                    FirebaseApp.getInstance()
                    true
                }
            } catch (_: Exception) {
                false
            }
            if (!hasFirebaseApp) return false
            return context == null || GoogleSignInHelper.resolveWebClientId(context) != null
        }
    }
}
