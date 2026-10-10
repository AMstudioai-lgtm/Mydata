package com.example.data.sync

import android.content.Context

/**
 * Curseurs de synchronisation, persistés dans les SharedPreferences.
 *
 * L'ordre d'écriture importe : le curseur n'avance qu'après l'application effective des
 * données. Une interruption laisse donc le curseur en arrière, et la reprise rejoue des
 * upserts idempotents (le conflit est résolu par updated_at), jamais une perte.
 */
class SyncPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun pullCursor(table: String): Long = prefs.getLong(keyPull(table), 0L)

    fun pushCursor(table: String): Long = prefs.getLong(keyPush(table), 0L)

    fun rememberPull(table: String, maxUpdatedAt: Long) {
        if (maxUpdatedAt <= 0) return
        prefs.edit().putLong(keyPull(table), maxUpdatedAt).apply()
    }

    fun rememberPush(table: String, maxUpdatedAt: Long) {
        if (maxUpdatedAt <= 0) return
        prefs.edit().putLong(keyPush(table), maxUpdatedAt).apply()
    }

    fun lastSyncAt(): Long = prefs.getLong(KEY_LAST_SYNC_AT, 0L)

    fun rememberSyncAt(at: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_AT, at).apply()
    }

    private fun keyPull(table: String) = "pull_$table"
    private fun keyPush(table: String) = "push_$table"

    companion object {
        private const val PREFS_NAME = "myapp_sync_prefs"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
    }
}
