package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val repository: JournalRepository
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

    class Factory(private val repository: JournalRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JournalViewModel(repository) as T
        }
    }
}
