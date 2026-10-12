package com.example.data.backup

// Règle corbeille centralisée côté pur (testable sans Android) : un élément est
// en corbeille si et seulement si deleted_at est non nul. Les requêtes DAO
// traduisent exactement cette règle en `deleted_at IS NOT NULL`.
object TrashRules {
    fun isTrashed(deletedAt: Long?): Boolean = deletedAt != null
}
