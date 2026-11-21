package com.example.domain.logic

object ConflictResolver {
    fun remoteWins(localUpdatedAt: Long, remoteUpdatedAt: Long): Boolean =
        remoteUpdatedAt > localUpdatedAt
}
