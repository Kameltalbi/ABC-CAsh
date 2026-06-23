package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.repository.TreasuryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InscriptionUiState(
    val nom: String = "",
    val nomEntreprise: String = "",
    val email: String = "",
    val telephone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val nomError: String? = null,
    val nomEntrepriseError: String? = null,
    val emailError: String? = null,
    val telephoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
)

class InscriptionViewModel(
    private val repository: TreasuryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InscriptionUiState())
    val uiState: StateFlow<InscriptionUiState> = _uiState.asStateFlow()

    var onInscriptionSuccess: ((User) -> Unit)? = null

    fun updateNom(nom: String) {
        _uiState.update { it.copy(nom = nom, nomError = null, generalError = null) }
    }

    fun updateNomEntreprise(nom: String) {
        _uiState.update { it.copy(nomEntreprise = nom, nomEntrepriseError = null, generalError = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun updateTelephone(telephone: String) {
        _uiState.update { it.copy(telephone = telephone, telephoneError = null, generalError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, generalError = null) }
    }

    fun inscrire() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            if (!validateFields()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val state = _uiState.value
            val entreprise = Entreprise(nom = state.nomEntreprise.trim())
            val user = User(
                nom = state.nom.trim(),
                email = state.email.trim(),
                telephone = state.telephone.trim(),
                passwordHash = "",
                role = UserRole.ADMIN,
                permissions = UserPermission.entries.toSet(),
                entrepriseId = entreprise.id
            )
            val registered = repository.registerAdmin(
                entreprise = entreprise,
                user = user,
                plainPassword = state.password
            )
            _uiState.update { it.copy(isLoading = false) }
            onInscriptionSuccess?.invoke(registered)
        }
    }

    private suspend fun validateFields(): Boolean {
        val state = _uiState.value
        var isValid = true

        if (state.nom.isBlank()) {
            _uiState.update { it.copy(nomError = "name_required") }
            isValid = false
        }
        if (state.nomEntreprise.isBlank()) {
            _uiState.update { it.copy(nomEntrepriseError = "company_required") }
            isValid = false
        }
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "email_required") }
            isValid = false
        } else if (repository.isEmailTaken(state.email)) {
            _uiState.update { it.copy(emailError = "email_taken") }
            isValid = false
        }
        if (state.telephone.isNotBlank() && repository.isTelephoneTaken(state.telephone)) {
            _uiState.update { it.copy(telephoneError = "phone_taken") }
            isValid = false
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "password_min_chars") }
            isValid = false
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "passwords_dont_match") }
            isValid = false
        }
        return isValid
    }
}

class InscriptionViewModelFactory(
    private val repository: TreasuryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InscriptionViewModel::class.java)) {
            return InscriptionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
