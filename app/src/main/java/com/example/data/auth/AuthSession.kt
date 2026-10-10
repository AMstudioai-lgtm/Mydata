package com.example.data.auth

data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String? = null,
    val isOfflineMode: Boolean = false
)

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val session: AuthSession) : AuthState
    data class Error(val message: String) : AuthState
}
