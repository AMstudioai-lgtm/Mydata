package com.example.data.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

/**
 * Orchestre Google Sign-In via Jetpack Credential Manager.
 * Chaque GetCredentialRequest porte une seule option pour garantir la fiabilité sur API 36 :
 * - GetGoogleIdOption pour la reconnexion silencieuse au démarrage ;
 * - GetSignInWithGoogleOption pour le bouton interactif « Continuer avec Google ».
 */
object GoogleSignInHelper {
    private const val TAG = "Auth"

    suspend fun attemptAutoSignIn(activityContext: Context): String? {
        val serverClientId = resolveWebClientId(activityContext) ?: return null
        val activity = activityContext.findActivity() ?: return null
        if (activity.isFinishing || activity.isDestroyed) return null

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )
            extractIdToken(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            null
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google auto sign-in cancelled: ${e.message}", e)
            null
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Silent Google auto sign-in unavailable: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google auto sign-in failed: ${e.message}", e)
            null
        } catch (e: LinkageError) {
            Log.w(TAG, "Silent Google auto sign-in unavailable (Play Services): ${e.message}", e)
            null
        }
    }

    suspend fun requestInteractiveSignIn(activityContext: Context): GoogleSignInResult {
        val serverClientId = resolveWebClientId(activityContext)
            ?: return GoogleSignInResult.Error(
                "Configuration Google Sign-In manquante (default_web_client_id absent)."
            )
        val activity = activityContext.findActivity()
            ?: return GoogleSignInResult.Error(
                "Impossible d'ouvrir Google Sign-In hors d'une activité Android."
            )
        if (activity.isFinishing || activity.isDestroyed) {
            return GoogleSignInResult.Error("Fenêtre de connexion indisponible, réessayez.")
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )
            val token = extractIdToken(result.credential)
            if (token != null) {
                GoogleSignInResult.Success(token)
            } else {
                GoogleSignInResult.Error("Jeton Google invalide ou non reconnu.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials available on device: ${e.message}", e)
            GoogleSignInResult.Error("Aucun compte Google disponible sur cet appareil.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google Sign-In failed: ${e.message}", e)
            GoogleSignInResult.Error(e.errorMessage?.toString() ?: e.message ?: "Échec de connexion Google.")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected Google Sign-In failure: ${e.message}", e)
            GoogleSignInResult.Error(e.message ?: "Erreur inattendue lors de la connexion Google.")
        } catch (e: LinkageError) {
            Log.w(TAG, "Google Play Services / Credential Manager unavailable: ${e.message}", e)
            GoogleSignInResult.Error("Services Google Play indisponibles sur cet appareil.")
        }
    }

    suspend fun clearCredentialState(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to clear Credential Manager state: ${e.message}", e)
            } catch (e: LinkageError) {
                Log.w(TAG, "Unable to clear Credential Manager state: ${e.message}", e)
            }
        }
    }

    /**
     * Résout `default_web_client_id` dynamiquement pour éviter une erreur de compilation
     * lorsque `app/google-services.json` est absent (mode passthrough en CI).
     */
    @SuppressLint("DiscouragedApi")
    internal fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return try {
            context.getString(resId).trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun extractIdToken(credential: androidx.credentials.Credential): String? {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (e: GoogleIdTokenParsingException) {
                Log.w(TAG, "Failed to parse GoogleIdTokenCredential: ${e.message}", e)
                null
            }
        }
        return null
    }
}
