package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Tri du tableau : colonne ciblée + sens. `ascending == null` = ordre de création. */
data class TableSort(
    val propertyId: String? = null,
    val ascending: Boolean? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val repository: JournalRepository,
    backupManager: Any? = null
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun setCurrentUser(userId: String?) {
        _currentUserId.value = userId
    }

    val journals: StateFlow<List<JournalEntity>> = _currentUserId
        .flatMapLatest { userId -> repository.watchJournals(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedJournalId = MutableStateFlow<String?>(null)
    val selectedJournalId: StateFlow<String?> = _selectedJournalId.asStateFlow()

    // Current selected journal entity
    val selectedJournal: StateFlow<JournalEntity?> = combine(journals, _selectedJournalId) { list, id ->
        when {
            list.isEmpty() -> null
            id == null -> list.firstOrNull()
            else -> list.find { it.id == id } ?: list.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Properties for selected journal
    // Combine avec l'utilisateur courant pour que le changement de compte
    // re-filtre immédiatement sans attendre un changement de journal.
    val properties: StateFlow<List<PropertyEntity>> = combine(selectedJournal, _currentUserId) { journal, userId ->
        journal to userId
    }
        .flatMapLatest { (journal, userId) ->
            if (journal != null) repository.watchProps(journal.id, userId) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Entry rows for selected journal
    val rows: StateFlow<List<EntryRowEntity>> = combine(selectedJournal, _currentUserId) { journal, userId ->
        journal to userId
    }
        .flatMapLatest { (journal, userId) ->
            if (journal != null) repository.watchRows(journal.id, userId) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recherche plein-texte sur les cellules. `recherche` est un alias de `query`.
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    val recherche: StateFlow<String> get() = query

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setRecherche(value: String) {
        _query.value = value
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    // Tri du tableau : appui cyclique croissant -> décroissant -> création.
    private val _sortPropertyId = MutableStateFlow<String?>(null)
    val sortPropertyId: StateFlow<String?> = _sortPropertyId.asStateFlow()

    private val _sortAscending = MutableStateFlow<Boolean?>(null)
    val sortAscending: StateFlow<Boolean?> = _sortAscending.asStateFlow()

    val tri: StateFlow<TableSort> = combine(_sortPropertyId, _sortAscending) { id, asc ->
        TableSort(propertyId = id, ascending = asc)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TableSort()
    )

    fun toggleSort(propertyId: String) {
        if (_sortPropertyId.value != propertyId) {
            _sortPropertyId.value = propertyId
            _sortAscending.value = true
            return
        }
        when (_sortAscending.value) {
            true -> _sortAscending.value = false
            else -> {
                _sortPropertyId.value = null
                _sortAscending.value = null
            }
        }
    }

    fun clearSort() {
        _sortPropertyId.value = null
        _sortAscending.value = null
    }

    fun selectJournal(id: String) {
        _selectedJournalId.value = id
    }

    fun createJournal(customName: String? = null) {
        viewModelScope.launch {
            val newId = repository.createJournal(customName)
            _selectedJournalId.value = newId
        }
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch {
            repository.deleteJournal(id)
            if (_selectedJournalId.value == id) {
                val remaining = journals.value.filter { it.id != id }
                _selectedJournalId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun renameJournal(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.renameJournal(id, name.trim())
        }
    }

    // Corbeille (méthodes T1 sur le repository, invoquées par réflexion pour que
    // cette branche compile avant la fusion : signatures visées à la lettre).
    @Suppress("UNCHECKED_CAST")
    private fun <T> flowFromRepository(methodName: String, vararg args: Any?): Flow<List<T>> {
        return try {
            val method = repository.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.size == args.size
            } ?: return flowOf(emptyList())
            (method.invoke(repository, *args) as? Flow<List<T>>) ?: flowOf(emptyList())
        } catch (_: Exception) {
            flowOf(emptyList())
        }
    }

    private suspend fun invokeRepositorySuspend(methodName: String, id: String) {
        val method = repository.javaClass.methods.firstOrNull { it.name == methodName } ?: return
        if (method.parameterTypes.size == 1) {
            // Méthode non-suspend : appel direct.
            try {
                method.invoke(repository, id)
            } catch (_: Exception) {
                Unit
            }
            return
        }
        // Méthode suspend (id, Continuation) : appel avec continuation manuelle.
        suspendCoroutine { cont ->
            try {
                val res = method.invoke(repository, id, cont)
                if (res != COROUTINE_SUSPENDED) cont.resume(Unit)
            } catch (e: InvocationTargetException) {
                cont.resumeWithException(e.targetException ?: e)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }

    val trashedJournals: StateFlow<List<JournalEntity>> =
        flowFromRepository<JournalEntity>("watchTrashedJournals")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val trashedRows: StateFlow<List<EntryRowEntity>> =
        flowFromRepository<EntryRowEntity>("watchAllTrashedRows")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun trashedRowsForJournal(journalId: String): Flow<List<EntryRowEntity>> =
        flowFromRepository("watchTrashedRows", journalId)

    fun restoreJournal(id: String) {
        viewModelScope.launch { invokeRepositorySuspend("restoreJournal", id) }
    }

    fun purgeJournal(id: String) {
        viewModelScope.launch { invokeRepositorySuspend("purgeJournal", id) }
    }

    fun restoreRow(id: String) {
        viewModelScope.launch { invokeRepositorySuspend("restoreRow", id) }
    }

    fun purgeRow(id: String) {
        viewModelScope.launch { invokeRepositorySuspend("purgeRow", id) }
    }

    // Sauvegarde via BackupManager (T1). Le manager est typé en `Any?` pour compiler
    // avant la fusion ; ses méthodes exportBackup/importBackup(uri): Result<*> sont
    // invoquées par réflexion, en version directe ou suspendue.
    private val _backupManager = MutableStateFlow<Any?>(backupManager)
    fun setBackupManager(manager: Any?) {
        _backupManager.value = manager
    }

    private val _backupFeedback = MutableStateFlow<String?>(null)
    val backupFeedback: StateFlow<String?> = _backupFeedback.asStateFlow()

    fun clearBackupFeedback() {
        _backupFeedback.value = null
    }

    private fun readBackupLong(info: Any, vararg names: String): Long? {
        val cls = info.javaClass
        for (name in names) {
            try {
                val field = cls.getDeclaredField(name)
                field.isAccessible = true
                (field.get(info) as? Number)?.let { return it.toLong() }
            } catch (_: Exception) {
                // Nom de champ absent : essaie les accesseurs.
            }
            try {
                val capitalized = name.replaceFirstChar { it.uppercase() }
                val accessor = cls.methods.firstOrNull {
                    it.name == name || it.name == "get$capitalized" || it.name == "is$capitalized"
                }
                (accessor?.invoke(info) as? Number)?.let { return it.toLong() }
            } catch (_: Exception) {
                // Accesseur absent : nom suivant.
            }
        }
        return null
    }

    private fun backupSummary(info: Any?): String? {
        if (info == null) return null
        val tables = readBackupLong(info, "tables", "tableCount", "journals", "journalCount")
        val rows = readBackupLong(info, "rows", "rowCount", "lines", "lineCount", "entries", "entryCount")
        val images = readBackupLong(info, "images", "imageCount")
        val parts = buildList {
            if (tables != null) add("$tables tables")
            if (rows != null) add("$rows lignes")
            if (images != null) add("$images images")
        }
        return parts.joinToString(", ").ifBlank { null }
    }

    private suspend fun invokeBackup(name: String, uri: Uri): Result<*>? {
        val manager = _backupManager.value ?: return null
        val method = manager.javaClass.methods.firstOrNull { it.name == name } ?: return null
        return try {
            if (method.parameterTypes.size == 1) {
                @Suppress("UNCHECKED_CAST")
                method.invoke(manager, uri) as? Result<*>
            } else {
                suspendCoroutine { cont ->
                    try {
                        val res = method.invoke(manager, uri, cont)
                        if (res != COROUTINE_SUSPENDED) {
                            @Suppress("UNCHECKED_CAST")
                            cont.resume(res as? Result<*>)
                        }
                    } catch (e: InvocationTargetException) {
                        cont.resumeWithException(e.targetException ?: e)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            }
        } catch (e: InvocationTargetException) {
            Result.failure(e.targetException ?: e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = invokeBackup("exportBackup", uri)
            _backupFeedback.value = when {
                result == null -> "Sauvegarde non disponible pour le moment."
                result.isSuccess -> backupSummary(result.getOrNull())
                    ?.let { "Sauvegarde exportée : $it." }
                    ?: "Sauvegarde exportée avec succès."
                else -> "Échec de la sauvegarde : ${result.exceptionOrNull()?.message ?: "erreur inconnue"}."
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = invokeBackup("importBackup", uri)
            _backupFeedback.value = when {
                result == null -> "Sauvegarde non disponible pour le moment."
                result.isSuccess -> backupSummary(result.getOrNull())
                    ?.let { "Sauvegarde restaurée : $it." }
                    ?: "Sauvegarde restaurée avec succès."
                else -> "Échec de la restauration : ${result.exceptionOrNull()?.message ?: "erreur inconnue"}."
            }
        }
    }

    fun addProperty(name: String, kind: String, options: List<String> = emptyList(), format: String? = null) {
        val journalId = selectedJournal.value?.id ?: return
        viewModelScope.launch {
            repository.addProperty(
                journalId = journalId,
                name = name,
                kind = kind,
                options = options,
                format = format
            )
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            repository.deleteProperty(propertyId)
        }
    }

    fun updateProperty(propertyId: String, name: String, kind: String, options: List<String> = emptyList(), format: String? = null) {
        viewModelScope.launch {
            repository.updateProperty(
                propertyId = propertyId,
                name = name,
                kind = kind,
                options = options,
                format = format
            )
        }
    }

    fun addRow() {
        val journalId = selectedJournal.value?.id ?: return
        viewModelScope.launch {
            repository.addRow(journalId)
        }
    }

    fun updateCell(rowId: String, propId: String, value: Any?) {
        viewModelScope.launch {
            repository.updateCell(rowId, propId, value)
        }
    }

    private val _selectedRowForDetailId = MutableStateFlow<String?>(null)
    val selectedRowForDetail: StateFlow<EntryRowEntity?> = combine(rows, _selectedRowForDetailId) { rowList, rowId ->
        if (rowId == null) null else rowList.find { it.id == rowId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun openRowDetail(rowId: String) {
        _selectedRowForDetailId.value = rowId
    }

    fun closeRowDetail() {
        _selectedRowForDetailId.value = null
    }

    fun moveProperty(propId: String, delta: Int) {
        val currentProps = properties.value
        val currentIndex = currentProps.indexOfFirst { it.id == propId }
        if (currentIndex == -1) return
        val targetIndex = currentIndex + delta
        if (targetIndex in currentProps.indices) {
            val mutableList = currentProps.toMutableList()
            val item = mutableList.removeAt(currentIndex)
            mutableList.add(targetIndex, item)
            viewModelScope.launch {
                repository.reorderProperties(mutableList.map { it.id })
            }
        }
    }

    fun movePropertyToFront(propId: String) {
        val currentProps = properties.value
        val currentIndex = currentProps.indexOfFirst { it.id == propId }
        if (currentIndex <= 0) return
        val mutableList = currentProps.toMutableList()
        val item = mutableList.removeAt(currentIndex)
        mutableList.add(0, item)
        viewModelScope.launch {
            repository.reorderProperties(mutableList.map { it.id })
        }
    }

    fun movePropertyToBack(propId: String) {
        val currentProps = properties.value
        val currentIndex = currentProps.indexOfFirst { it.id == propId }
        if (currentIndex == -1 || currentIndex == currentProps.lastIndex) return
        val mutableList = currentProps.toMutableList()
        val item = mutableList.removeAt(currentIndex)
        mutableList.add(item)
        viewModelScope.launch {
            repository.reorderProperties(mutableList.map { it.id })
        }
    }

    fun deleteRow(rowId: String) {
        viewModelScope.launch {
            repository.deleteRow(rowId)
        }
    }

    class Factory(
        private val repository: JournalRepository,
        private val backupManager: Any? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JournalViewModel(repository, backupManager) as T
        }
    }
}
