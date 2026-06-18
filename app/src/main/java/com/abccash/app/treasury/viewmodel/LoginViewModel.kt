package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.repository.TreasuryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
)

class LoginViewModel(private val repository: TreasuryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    var onLoginSuccess: ((User) -> Unit)? = null

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val state = _uiState.value

            if (state.email.isBlank()) {
                _uiState.update { it.copy(emailError = "L'email est requis", isLoading = false) }
                return@launch
            }
            if (state.password.isBlank()) {
                _uiState.update { it.copy(passwordError = "Le mot de passe est requis", isLoading = false) }
                return@launch
            }

            val user = repository.login(state.email, state.password)
            if (user == null) {
                _uiState.update {
                    it.copy(
                        generalError = "Email ou mot de passe incorrect",
                        isLoading = false
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = false) }
            onLoginSuccess?.invoke(user)
        }
    }
}

class LoginViewModelFactory(private val repository: TreasuryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
