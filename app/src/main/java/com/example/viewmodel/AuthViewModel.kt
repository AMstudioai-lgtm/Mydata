package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthState
import com.example.data.auth.FirebaseAuthService
import com.example.data.auth.GoogleSignInHelper
import com.example.data.auth.GoogleSignInResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authService: FirebaseAuthService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUserId: StateFlow<String?> = _authState
        .map { state -> (state as? AuthState.LoggedIn)?.session?.userId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isAuthConfigured: Boolean
        get() = authService.isConfigured

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var autoSignInAttempted = false

    init {
        viewModelScope.launch {
            authService.observeAuthSession().collect { session ->
                _authState.value = if (session != null) {
                    AuthState.LoggedIn(session)
                } else {
                    AuthState.LoggedOut
                }
            }
        }
    }

    fun attemptSilentSignIn(activityContext: Context) {
        if (autoSignInAttempted || _authState.value is AuthState.LoggedIn) return
        autoSignInAttempted = true
        viewModelScope.launch {
            val idToken = GoogleSignInHelper.attemptAutoSignIn(activityContext) ?: return@launch
            signInWithGoogleIdToken(idToken)
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            try {
                when (val result = GoogleSignInHelper.requestInteractiveSignIn(activityContext)) {
                    is GoogleSignInResult.Success -> {
                        val firebaseResult = authService.signInWithGoogleIdToken(result.idToken)
                        firebaseResult.onSuccess { session ->
                            _authState.value = AuthState.LoggedIn(session)
                        }.onFailure { ex ->
                            _errorMessage.value = ex.message ?: "Erreur d'authentification Firebase."
                        }
                    }
                    is GoogleSignInResult.Cancelled -> {
                        _errorMessage.value = "Connexion annulée. Sélectionnez un compte Google pour continuer."
                    }
                    is GoogleSignInResult.Error -> {
                        _errorMessage.value = result.message
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur de connexion Google."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String) {
        _isSubmitting.value = true
        _errorMessage.value = null
        try {
            val result = authService.signInWithGoogleIdToken(idToken)
            result.onSuccess { session ->
                _authState.value = AuthState.LoggedIn(session)
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Erreur d'authentification Firebase."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Erreur d'authentification Firebase."
        } finally {
            _isSubmitting.value = false
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                GoogleSignInHelper.clearCredentialState(context)
                authService.signOut()
            }
            _authState.value = AuthState.LoggedOut
            _errorMessage.value = null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val authService: FirebaseAuthService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authService) as T
        }
    }
}
