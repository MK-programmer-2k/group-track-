package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.repository.GroupTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userName: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: GroupTrackRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn = sessionManager.accessTokenFlow

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in both email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(email.trim(), pass)
            result.onSuccess {
                _uiState.value = AuthUiState.Success(it.user.name)
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.message ?: "Authentication failed")
            }
        }
    }

    fun register(name: String, email: String, phone: String?, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.length < 6) {
            _uiState.value = AuthUiState.Error("Please check inputs. Password must be >= 6 chars.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(name.trim(), email.trim(), phone?.trim(), pass)
            result.onSuccess {
                _uiState.value = AuthUiState.Success(it.user.name)
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.message ?: "Registration failed")
            }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    class Factory(
        private val repository: GroupTrackRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository, sessionManager) as T
        }
    }
}
