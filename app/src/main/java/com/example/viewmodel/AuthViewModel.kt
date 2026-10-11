package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthSession
import com.example.data.auth.AuthState
import com.example.data.auth.SupabaseAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: SupabaseAuthService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Dérivé de authState : null si déconnecté, sinon l'userId de la session.
    val currentUserId: StateFlow<String?> = _authState
        .map { state -> (state as? AuthState.LoggedIn)?.session?.userId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkInitialSession()
    }

    private fun checkInitialSession() {
        val cached = authService.getCachedSession()
        if (cached != null) {
            _authState.value = AuthState.LoggedIn(cached)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            val result = authService.signInWithPassword(email, password)
            _isSubmitting.value = false
            result.onSuccess { session ->
                _authState.value = AuthState.LoggedIn(session)
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Erreur de connexion"
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            val result = authService.signUp(email, password)
            _isSubmitting.value = false
            result.onSuccess { session ->
                _authState.value = AuthState.LoggedIn(session)
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Erreur d'inscription"
            }
        }
    }

    fun signOut() {
        authService.clearSession()
        _authState.value = AuthState.LoggedOut
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val authService: SupabaseAuthService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authService) as T
        }
    }
}
