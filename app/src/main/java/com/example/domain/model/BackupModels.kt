package com.example.domain.model

const val BACKUP_VERSION = 1

data class BackupJournal(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val userId: String?
)

data class BackupProperty(
    val id: String,
    val journalId: String,
    val name: String,
    val kind: String,
    val format: String?,
    val optionsJson: String,
    val sortOrder: Int,
    val updatedAt: Long,
    val deletedAt: Long?,
    val userId: String?
)

data class BackupRow(
    val id: String,
    val journalId: String,
    val valuesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val userId: String?
)

data class BackupPayload(
    val version: Int = BACKUP_VERSION,
    val journals: List<BackupJournal> = emptyList(),
    val properties: List<BackupProperty> = emptyList(),
    val rows: List<BackupRow> = emptyList()
)

data class BackupInfo(
    val journals: Int,
    val rows: Int,
    val images: Int,
    val firstActiveJournalId: String? = null
)

class BackupFormatException(message: String) : IllegalArgumentException(message)
