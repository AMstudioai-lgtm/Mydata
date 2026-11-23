package com.example.data.sync

import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.firebase.OperationType
import com.example.data.firebase.logFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class FirestorePullResult<T>(
    val items: List<T>,
    val maxClientUpdatedAt: Long
)

/**
 * Client Firestore pour la synchronisation des collections privées `/users/{userId}/...`.
 *
 * Reçoit toujours l'instance nommée (`FirebaseFirestore.getInstance(R.string.firestore_database_id)`),
 * jamais la base `(default)`.
 */
class FirestoreSyncClient(
    private val firestore: FirebaseFirestore,
    private val authProvider: () -> FirebaseAuth = { Firebase.auth }
) {
    private fun requireUserId(expectedUserId: String): String {
        val uid = authProvider().currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Authentification Google requise pour synchroniser.")
        if (uid != expectedUserId) {
            throw SecurityException("L'utilisateur connecté ne correspond pas à la session demandée.")
        }
        return uid
    }

    private fun userCollection(userId: String, name: String) =
        firestore.collection("users").document(userId).collection(name)

    suspend fun pullJournals(userId: String, since: Long): Result<FirestorePullResult<FirestoreJournalDoc>> {
        val path = "users/$userId/journals"
        return try {
            val uid = requireUserId(userId)
            val snapshot = userCollection(uid, "journals")
                .whereGreaterThan("clientUpdatedAt", since)
                .orderBy("clientUpdatedAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val docs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    FirestoreJournalDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            val maxUpdated = docs.maxOfOrNull { it.clientUpdatedAt } ?: since
            Result.success(FirestorePullResult(docs, maxUpdated))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pullProperties(userId: String, since: Long): Result<FirestorePullResult<FirestorePropertyDoc>> {
        val path = "users/$userId/properties"
        return try {
            val uid = requireUserId(userId)
            val snapshot = userCollection(uid, "properties")
                .whereGreaterThan("clientUpdatedAt", since)
                .orderBy("clientUpdatedAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val docs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    FirestorePropertyDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            val maxUpdated = docs.maxOfOrNull { it.clientUpdatedAt } ?: since
            Result.success(FirestorePullResult(docs, maxUpdated))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pullEntryRows(userId: String, since: Long): Result<FirestorePullResult<FirestoreEntryRowDoc>> {
        val path = "users/$userId/entry_rows"
        return try {
            val uid = requireUserId(userId)
            val snapshot = userCollection(uid, "entry_rows")
                .whereGreaterThan("clientUpdatedAt", since)
                .orderBy("clientUpdatedAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val docs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    FirestoreEntryRowDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            val maxUpdated = docs.maxOfOrNull { it.clientUpdatedAt } ?: since
            Result.success(FirestorePullResult(docs, maxUpdated))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun pushJournals(userId: String, items: List<JournalEntity>): Result<Long> {
        if (items.isEmpty()) return Result.success(0L)
        return try {
            val uid = requireUserId(userId)
            val col = userCollection(uid, "journals")
            var maxPushed = 0L
            for (item in items) {
                val docPath = "users/$uid/journals/${item.id}"
                try {
                    val docRef = col.document(item.id)
                    val existing = docRef.get().await()
                    if (existing.exists()) {
                        docRef.update(FirestoreSyncMapper.journalToUpdateMap(item)).await()
                    } else {
                        docRef.set(FirestoreSyncMapper.journalToCreateMap(item, uid)).await()
                    }
                    if (item.updatedAt > maxPushed) maxPushed = item.updatedAt
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logFirestoreError(e, OperationType.WRITE, docPath)
                    return Result.failure(e)
                }
            }
            Result.success(maxPushed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.WRITE, "users/$userId/journals")
            Result.failure(e)
        }
    }

    suspend fun pushProperties(userId: String, items: List<PropertyEntity>): Result<Long> {
        if (items.isEmpty()) return Result.success(0L)
        return try {
            val uid = requireUserId(userId)
            val col = userCollection(uid, "properties")
            var maxPushed = 0L
            for (item in items) {
                val docPath = "users/$uid/properties/${item.id}"
                try {
                    val docRef = col.document(item.id)
                    val existing = docRef.get().await()
                    if (existing.exists()) {
                        docRef.update(FirestoreSyncMapper.propertyToUpdateMap(item)).await()
                    } else {
                        docRef.set(FirestoreSyncMapper.propertyToCreateMap(item, uid)).await()
                    }
                    if (item.updatedAt > maxPushed) maxPushed = item.updatedAt
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logFirestoreError(e, OperationType.WRITE, docPath)
                    return Result.failure(e)
                }
            }
            Result.success(maxPushed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.WRITE, "users/$userId/properties")
            Result.failure(e)
        }
    }

    suspend fun pushEntryRows(userId: String, items: List<EntryRowEntity>): Result<Long> {
        if (items.isEmpty()) return Result.success(0L)
        return try {
            val uid = requireUserId(userId)
            val col = userCollection(uid, "entry_rows")
            var maxPushed = 0L
            for (item in items) {
                val docPath = "users/$uid/entry_rows/${item.id}"
                try {
                    val docRef = col.document(item.id)
                    val existing = docRef.get().await()
                    if (existing.exists()) {
                        docRef.update(FirestoreSyncMapper.entryRowToUpdateMap(item)).await()
                    } else {
                        docRef.set(FirestoreSyncMapper.entryRowToCreateMap(item, uid)).await()
                    }
                    if (item.updatedAt > maxPushed) maxPushed = item.updatedAt
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logFirestoreError(e, OperationType.WRITE, docPath)
                    return Result.failure(e)
                }
            }
            Result.success(maxPushed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.WRITE, "users/$userId/entry_rows")
            Result.failure(e)
        }
    }
}
