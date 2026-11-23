package com.example.data.sync

import android.content.Context

/**
 * Curseurs de synchronisation isolés par utilisateur (`userId`), persistés en SharedPreferences.
 *
 * L'ordre d'écriture importe : le curseur n'avance qu'après l'application effective des
 * données. Une interruption laisse donc le curseur en arrière, et la reprise rejoue des
 * upserts idempotents (le conflit est résolu par updated_at), jamais une perte.
 */
class SyncPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun pullCursor(userId: String, table: String): Long =
        prefs.getLong(keyPull(userId, table), 0L)

    fun pushCursor(userId: String, table: String): Long =
        prefs.getLong(keyPush(userId, table), 0L)

    fun rememberPull(userId: String, table: String, maxUpdatedAt: Long) {
        if (maxUpdatedAt <= 0) return
        prefs.edit().putLong(keyPull(userId, table), maxUpdatedAt).apply()
    }

    fun rememberPush(userId: String, table: String, maxUpdatedAt: Long) {
        if (maxUpdatedAt <= 0) return
        prefs.edit().putLong(keyPush(userId, table), maxUpdatedAt).apply()
    }

    fun lastSyncAt(userId: String? = null): Long {
        val key = if (userId.isNullOrBlank()) KEY_LAST_SYNC_AT else "${KEY_LAST_SYNC_AT}_$userId"
        return prefs.getLong(key, 0L)
    }

    fun rememberSyncAt(at: Long, userId: String? = null) {
        val editor = prefs.edit().putLong(KEY_LAST_SYNC_AT, at)
        if (!userId.isNullOrBlank()) {
            editor.putLong("${KEY_LAST_SYNC_AT}_$userId", at)
        }
        editor.apply()
    }

    private fun keyPull(userId: String, table: String) = "fs_pull_${userId}_$table"
    private fun keyPush(userId: String, table: String) = "fs_push_${userId}_$table"

    companion object {
        private const val PREFS_NAME = "myapp_sync_prefs"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
    }
}
