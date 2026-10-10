package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.sync.SyncOutcome
import com.example.data.sync.SyncRepository
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Expose l'état de synchronisation à l'interface et déclenche les synchronisations.
 * La synchro est volontairement manuelle à la demande : une synchro automatique en arrière-plan
 * consommerait de la batterie et des données sans que l'utilisateur l'ait demandé.
 */
class SyncViewModel(
    private val syncRepository: SyncRepository?
) : ViewModel() {

    private val _isAvailable = kotlinx.coroutines.flow.MutableStateFlow(syncRepository != null)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    val status: StateFlow<SyncStatus> = syncRepository?.status
        ?: kotlinx.coroutines.flow.MutableStateFlow(SyncStatus())

    val lastOutcome = kotlinx.coroutines.flow.MutableStateFlow<SyncOutcome?>(null)

    fun syncNow() {
        val repository = syncRepository ?: run {
            lastOutcome.value = SyncOutcome.Skipped("Synchronisation indisponible")
            return
        }
        viewModelScope.launch {
            lastOutcome.value = repository.syncNow()
        }
    }

    fun clearFeedback() {
        lastOutcome.value = null
        syncRepository?.clearError()
    }

    class Factory(private val syncRepository: SyncRepository?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SyncViewModel(syncRepository) as T
        }
    }
}
