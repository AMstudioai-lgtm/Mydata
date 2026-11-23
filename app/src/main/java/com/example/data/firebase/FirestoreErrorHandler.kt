package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

/**
 * Journalise une erreur Firestore structurée en JSON afin de diagnostiquer immédiatement
 * tout refus de règle de sécurité ou anomalie réseau sans jamais faire planter l'UI.
 */
fun logFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
) {
    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfo = JSONArray().apply {
        currentUser?.providerData?.forEach { info ->
            put(
                JSONObject().apply {
                    put("providerId", info.providerId)
                    put("displayName", info.displayName)
                    put("email", info.email)
                    put("photoUrl", info.photoUrl?.toString())
                }
            )
        }
    }

    val authInfo = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("isAnonymous", currentUser?.isAnonymous)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", providerInfo)
    }

    val errorJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path)
        put("authInfo", authInfo)
    }.toString()

    Log.e("FirestoreError", errorJson, exception)
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): Nothing {
    logFirestoreError(exception, operationType, path)
    throw IllegalStateException(
        "Firestore ${operationType.name.lowercase()} failed at ${path ?: "unknown"}: ${exception.message}",
        exception
    )
}
