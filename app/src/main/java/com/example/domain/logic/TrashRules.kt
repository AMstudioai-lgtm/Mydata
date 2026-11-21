package com.example.domain.logic

object TrashRules {
    fun isTrashed(deletedAt: Long?): Boolean = deletedAt != null
}
