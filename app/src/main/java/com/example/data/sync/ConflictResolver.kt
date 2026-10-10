package com.example.data.sync

/**
 * Règle de résolution des conflits, isolée du stockage pour être testable sans appareil.
 *
 * Le comparatif est strict : à horodatage égal, la ligne locale gagne. Cela évite qu'une
 * ligne rejouée par une reprise de synchronisation (même horodatage, upsert idempotent)
 * n'écrase une modification faite entre-temps sur l'appareil.
 */
object ConflictResolver {
    fun remoteWins(localUpdatedAt: Long, remoteUpdatedAt: Long): Boolean =
        remoteUpdatedAt > localUpdatedAt
}
